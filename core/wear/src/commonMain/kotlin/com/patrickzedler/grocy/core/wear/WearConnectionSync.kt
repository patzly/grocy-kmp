package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.credentials.ServerConnectionCodec
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.ServerUrl
import kotlinx.coroutines.flow.first

/** Where the phone keeps the login for the watch in the Wearable Data Layer. */
const val WEAR_CONNECTION_PATH = "/grocy/connection"

/** The key of the login in that data item. */
const val WEAR_CONNECTION_KEY = "connection"

/** What the watch sends to wake a phone whose app has not published the login yet. */
const val WEAR_REQUEST_CONNECTION_PATH = "/grocy/request-connection"

/** The link of the watch to its phone. */
interface PhoneLink {
    /** The login the phone published, encoded; null if none or the Data Layer is unavailable. */
    suspend fun publishedConnection(): String?

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
 */
class WearConnectionSync(
    private val credentialStore: CredentialStore,
    private val auth: AuthRepository,
    private val marker: PhoneLoginMarker,
) {

    /** The phone's login in its [encoded] form, null when the phone logged out. */
    suspend fun onPhoneConnection(encoded: String?) {
        if (encoded == null) {
            if (marker.isFromPhone()) {
                auth.logout()
                marker.setFromPhone(false)
            }
            return
        }
        val connection = ServerConnectionCodec.decode(encoded)?.takeIf(::isUsable) ?: return
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
