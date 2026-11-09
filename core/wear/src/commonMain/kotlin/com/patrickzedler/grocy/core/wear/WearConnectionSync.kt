package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.credentials.ServerConnectionCodec
import com.patrickzedler.grocy.core.data.credentials.TrustedCertificatesCodec
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import com.patrickzedler.grocy.core.network.ServerUrl
import kotlinx.coroutines.flow.first

/** Where the phone keeps the login for the watch in the Wearable Data Layer. */
const val WEAR_CONNECTION_PATH = "/grocy/connection"

/** The key of the login in that data item. */
const val WEAR_CONNECTION_KEY = "connection"

/** The key of the certificates the phone trusts for that login's server. */
const val WEAR_CERTIFICATES_KEY = "certificates"

/** What the watch sends to wake a phone whose app has not published the login yet. */
const val WEAR_REQUEST_CONNECTION_PATH = "/grocy/request-connection"

/** Of [trusted] those for the server of [connection], nothing else leaves the phone. */
fun trustedFor(
    connection: ServerConnection,
    trusted: List<TrustedCertificate>,
): List<TrustedCertificate> {
    val host = ServerUrl.hostOf(connection.serverUrl)
    return trusted.filter { it.host.equals(host, ignoreCase = true) }
}

/** The link of the watch to its phone. */
interface PhoneLink {
    /** The login the phone published, encoded; null if none or the Data Layer is unavailable. */
    suspend fun publishedConnection(): String?

    /** The certificates the phone trusts for that server, encoded; null if there are none. */
    suspend fun publishedCertificates(): String? = null

    /** Wakes the phone's app, which then publishes its login. */
    suspend fun requestConnection()
}

/** Whether the login of the watch came from the phone, kept on the watch. */
interface PhoneLoginMarker {
    suspend fun isFromPhone(): Boolean

    suspend fun setFromPhone(fromPhone: Boolean)
}

/**
 * Applies the login the phone published to the watch. A login entered on the watch itself (for
 * phones without Play Services) stays until the phone publishes one; logging out on the phone
 * only logs out a watch that got its login from there.
 *
 * The certificates the user trusted on the phone for its server come along, so a self-signed
 * server works without a question on the watch and a renewed certificate follows by itself.
 *
 * @param setTrusted replaces the certificates the watch trusts.
 */
class WearConnectionSync(
    private val credentialStore: CredentialStore,
    private val auth: AuthRepository,
    private val marker: PhoneLoginMarker,
    private val setTrusted: suspend (List<TrustedCertificate>) -> Unit = {},
) {

    /**
     * The phone's login in its [encoded] form, null when the phone logged out, with the
     * [certificates] it trusts for that server.
     */
    suspend fun onPhoneConnection(encoded: String?, certificates: String? = null) {
        if (encoded == null) {
            if (marker.isFromPhone()) {
                auth.logout()
                marker.setFromPhone(false)
                setTrusted(emptyList())
            }
            return
        }
        val connection = ServerConnectionCodec.decode(encoded)?.takeIf(::isUsable) ?: return
        // First, the login's first requests already need them
        setTrusted(trustedFor(connection, TrustedCertificatesCodec.decode(certificates)))
        if (credentialStore.connection.first() != connection) auth.saveLogin(connection)
        marker.setFromPhone(true)
    }

    /** A login entered on the watch, already verified. */
    suspend fun onManualLogin(connection: ServerConnection) {
        auth.saveLogin(connection)
        marker.setFromPhone(false)
    }

    private fun isUsable(connection: ServerConnection): Boolean =
        ServerUrl.normalize(connection.serverUrl) == connection.serverUrl
}
