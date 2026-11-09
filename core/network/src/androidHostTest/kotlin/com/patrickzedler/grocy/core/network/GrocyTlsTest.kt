package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.network.CertificateSettings
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import java.net.InetAddress
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate

/** Real TLS over the loopback device, against a server with a self-signed certificate. */
class GrocyTlsTest {

    private val loopback = InetAddress.getByName("127.0.0.1")
    private val serverCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName("other.example")
        .build()
    private val clientCertificate = HeldCertificate.Builder().commonName("phone").build()
    private var settings = CertificateSettings()
    private var peer: X509Certificate? = null
    private val socket: SSLServerSocket = serverSocket()

    @AfterTest
    fun tearDown() = socket.close()

    private fun serverSocket(): SSLServerSocket {
        val handshake = HandshakeCertificates.Builder()
            .heldCertificate(serverCertificate)
            .addTrustedCertificate(clientCertificate.certificate)
            .build()
        return (handshake.sslContext().serverSocketFactory.createServerSocket(0, 5, loopback)
            as SSLServerSocket).apply { wantClientAuth = true }
    }

    private fun serve() {
        thread(isDaemon = true) {
            val client = socket.accept() as SSLSocket
            client.use {
                it.startHandshake()
                peer = runCatching { it.session.peerCertificates.first() as X509Certificate }
                    .getOrNull()
                it.inputStream.read(ByteArray(1024))
                it.outputStream.write(RESPONSE.toByteArray())
                it.outputStream.flush()
            }
        }
    }

    private val keys = object : ClientKeySource {
        override fun privateKey(alias: String): PrivateKey = clientCertificate.keyPair.private

        override fun certificateChain(alias: String): Array<X509Certificate> =
            arrayOf(clientCertificate.certificate)
    }

    private fun request(): Int {
        serve()
        val options = TlsOptions(keys, { settings }, {})
        val client = OkHttpClient.Builder().also { GrocyTls(options).applyTo(it) }.build()
        val url = "https://${loopback.hostAddress}:${socket.localPort}/"
        return client.newCall(Request.Builder().url(url).build()).execute().use { it.code }
    }

    private val leaf get() = serverCertificate.certificate.toServerCertificate()

    @Test
    fun anUnknownCertificateIsRefusedAndNamed() {
        val error = assertFailsWith<SSLException> { request() }

        assertEquals(leaf, error.untrustedCertificate())
    }

    @Test
    fun aCertificateTrustedForTheServerConnectsDespiteItsOtherName() {
        settings = CertificateSettings(
            trusted = listOf(TrustedCertificate(loopback.hostAddress!!, leaf)),
        )

        assertEquals(200, request())
    }

    @Test
    fun aCertificateTrustedForAnotherServerDoesNotCount() {
        settings = CertificateSettings(trusted = listOf(TrustedCertificate("elsewhere", leaf)))

        assertFailsWith<SSLPeerUnverifiedException> { request() }
    }

    @Test
    fun withoutAChosenCertificateTheServerGetsNone() {
        settings = CertificateSettings(
            trusted = listOf(TrustedCertificate(loopback.hostAddress!!, leaf)),
        )

        request()

        assertNull(peer)
    }

    @Test
    fun theChosenCertificateIdentifiesTheClient() {
        settings = CertificateSettings(
            clientAlias = "phone",
            trusted = listOf(TrustedCertificate(loopback.hostAddress!!, leaf)),
        )

        request()

        assertEquals(clientCertificate.certificate, assertNotNull(peer))
    }

    @Test
    fun theFingerprintIsTheCertificatesSha256() {
        assertEquals(leaf.sha256.length, 64)
        assertEquals(leaf.fingerprint.length, 64 + 31)
    }

    private companion object {
        const val RESPONSE = "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
    }
}
