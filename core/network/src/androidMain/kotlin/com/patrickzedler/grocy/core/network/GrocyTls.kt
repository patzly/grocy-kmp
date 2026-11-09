package com.patrickzedler.grocy.core.network

import android.content.Context
import android.security.KeyChain
import android.security.KeyChainException
import com.patrickzedler.grocy.core.model.network.CertificateSettings
import com.patrickzedler.grocy.core.model.network.ServerCertificate
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient

actual interface ClientKeySource {
    fun privateKey(alias: String): PrivateKey?

    fun certificateChain(alias: String): Array<X509Certificate>?
}

/** Android's key store, where the user installs client certificates; keys never leave it. */
class KeyChainClientKeys(context: Context) : ClientKeySource {
    private val context = context.applicationContext

    override fun privateKey(alias: String): PrivateKey? =
        keyChain { KeyChain.getPrivateKey(context, alias) }

    override fun certificateChain(alias: String): Array<X509Certificate>? =
        keyChain { KeyChain.getCertificateChain(context, alias) }

    // Removed from the device or the permission taken back: the server then asks in vain
    private inline fun <T> keyChain(read: () -> T?): T? = try {
        read()
    } catch (_: KeyChainException) {
        null
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    }
}

/** The certificate of the server is neither from a known authority nor trusted by the user. */
internal class UntrustedCertificateException(
    val certificate: ServerCertificate,
    cause: Throwable,
) : CertificateException("The server's certificate is not trusted", cause)

/** Applies [TlsOptions] to OkHttp. */
internal class GrocyTls(private val options: TlsOptions) {

    private val trustManager = PinningTrustManager(systemTrustManager(), ::settings)
    private val context = SSLContext.getInstance("TLS").apply {
        init(arrayOf(ClientKeyManager(options.clientKeys, ::settings)), arrayOf(trustManager), null)
    }

    private fun settings(): CertificateSettings = options.settings() ?: CertificateSettings()

    fun applyTo(builder: OkHttpClient.Builder) {
        val pool = builder.build().connectionPool
        builder.sslSocketFactory(context.socketFactory, trustManager)
        builder.hostnameVerifier(PinningHostnameVerifier(builder.build().hostnameVerifier, ::settings))
        builder.addInterceptor(
            Interceptor { chain ->
                // OkHttp's own threads may wait for the settings at app start
                if (options.settings() == null) runBlocking { options.awaitSettings() }
                chain.proceed(chain.request())
            },
        )
        options.onChanged = {
            pool.evictAll()
            // A resumed session would skip the new choice of certificates
            val sessions = context.clientSessionContext
            sessions.ids.asSequence().forEach { sessions.getSession(it)?.invalidate() }
        }
    }
}

private fun systemTrustManager(): X509TrustManager =
    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers
        .filterIsInstance<X509TrustManager>()
        .first()

/** What the system trusts, and what the user trusted by its exact certificate. */
internal class PinningTrustManager(
    private val system: X509TrustManager,
    private val settings: () -> CertificateSettings,
) : X509TrustManager {

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        try {
            system.checkServerTrusted(chain, authType)
        } catch (e: CertificateException) {
            val leaf = chain.firstOrNull() ?: throw e
            val certificate = leaf.toServerCertificate()
            // Which host it is for decides the hostname verifier, which always runs after this
            if (!settings().isTrusted(certificate.sha256)) {
                throw UntrustedCertificateException(certificate, e)
            }
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        system.checkClientTrusted(chain, authType)

    override fun getAcceptedIssuers(): Array<X509Certificate> = system.acceptedIssuers
}

/** A certificate the user trusted for a host needs no matching name, e.g. for an IP address. */
internal class PinningHostnameVerifier(
    private val default: HostnameVerifier,
    private val settings: () -> CertificateSettings,
) : HostnameVerifier {

    override fun verify(hostname: String, session: SSLSession): Boolean {
        if (default.verify(hostname, session)) return true
        val leaf = try {
            session.peerCertificates.firstOrNull() as? X509Certificate
        } catch (_: SSLPeerUnverifiedException) {
            null
        } ?: return false
        return settings().isTrusted(hostname, leaf.toServerCertificate().sha256)
    }
}

/** Identifies with the chosen certificate, only if there is one and only if a server asks. */
internal class ClientKeyManager(
    private val keys: ClientKeySource?,
    private val settings: () -> CertificateSettings,
) : X509ExtendedKeyManager() {

    private fun alias(): String? = settings().clientAlias.takeIf { keys != null }

    override fun chooseClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? = alias()

    override fun chooseEngineClientAlias(
        keyType: Array<out String>?,
        issuers: Array<out Principal>?,
        engine: SSLEngine?,
    ): String? = alias()

    override fun getClientAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String>? = alias()?.let { arrayOf(it) }

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? =
        alias?.let { keys?.certificateChain(it) }

    override fun getPrivateKey(alias: String?): PrivateKey? = alias?.let { keys?.privateKey(it) }

    override fun getServerAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String>? = null

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? = null
}

internal fun X509Certificate.toServerCertificate() = ServerCertificate(
    sha256 = MessageDigest.getInstance("SHA-256")
        .digest(encoded)
        .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') },
    subject = subjectX500Principal.name,
    issuer = issuerX500Principal.name,
    notAfterEpochDay = notAfter.time.floorDiv(MILLIS_PER_DAY),
)

private const val MILLIS_PER_DAY = 86_400_000L
