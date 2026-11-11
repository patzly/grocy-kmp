package com.patrickzedler.grocy.core.data.auth

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.settings.CertificateTrustGate
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.sessionsAfterLogin
import com.patrickzedler.grocy.core.model.sessionsOf
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.network.ServerUrl
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * @param keepsSessions a logout keeps the login for later; the watch, which shows no list of
 *   sessions, must not keep credentials it no longer uses.
 * @param certificateGate asks whether to trust a server's certificate nothing vouches for.
 * @param clearLocalData forgets the data of the previous server (local database), called before
 *   a login and on logout, so no screen ever shows another server's data.
 */
class AuthRepository(
    private val credentialStore: CredentialStore,
    private val grocyApi: GrocyApi,
    private val keepsSessions: Boolean = true,
    private val certificateGate: CertificateTrustGate? = null,
    private val clearLocalData: suspend () -> Unit = {},
) {

    val authState: Flow<AuthState> = credentialStore.connection.map { connection ->
        if (connection == null) AuthState.LoggedOut else AuthState.LoggedIn(connection)
    }

    /** The logins kept on the device, most recently used first, the current one leading. */
    val sessions: Flow<List<SavedSession>> =
        combine(credentialStore.connection, credentialStore.sessions, ::sessionsOf)

    /**
     * Checks that [connection] reaches a Grocy server that accepts it, without storing anything.
     *
     * @return who the connection logs in as.
     * @throws GrocyApiException if the server is unreachable, rejects the key or is no Grocy server.
     */
    suspend fun verify(connection: ServerConnection): User = try {
        systemAndUser(connection)
    } catch (e: GrocyApiException.UntrustedCertificate) {
        val host = ServerUrl.hostOf(connection.serverUrl)
        if (certificateGate?.ask(host, e.certificate) != true) throw e
        systemAndUser(connection)
    }

    private suspend fun systemAndUser(connection: ServerConnection): User {
        // Fails early with a clear error if the address is no Grocy server at all
        grocyApi.systemInfo(connection)
        // Requires authentication, so this is where a wrong API key shows up
        return grocyApi.currentUser(connection)
    }

    /**
     * Stores [connection] and thereby logs in. Call [verify] first, unless the connection was
     * already verified elsewhere.
     *
     * @param user who it logs in as, from [verify].
     */
    suspend fun saveLogin(connection: ServerConnection, user: User? = null) {
        clearLocalData()
        if (keepsSessions) {
            val saved = credentialStore.sessions.first()
            credentialStore.saveSessions(sessionsAfterLogin(saved, connection, user))
        }
        credentialStore.save(connection)
    }

    /**
     * Logs in to [session]. The current login stays if it fails.
     *
     * @throws GrocyApiException if its server does not answer or no longer accepts it.
     */
    suspend fun switchTo(session: SavedSession) {
        val user = verify(session.connection)
        // Cancelled after the check, the login still completes: never cleared data without it
        withContext(NonCancellable) { saveLogin(session.connection, user) }
    }

    /** Back to the login; the session stays kept for later, see [removeSession]. */
    suspend fun logout() {
        // A login older than the sessions is only kept from now on
        if (keepsSessions) credentialStore.saveSessions(sessions.first())
        credentialStore.clear()
        clearLocalData()
    }

    /** Forgets [session] with its credentials; for the current one that also logs out. */
    suspend fun removeSession(session: SavedSession) {
        val isCurrent = credentialStore.connection.first() == session.connection
        credentialStore.saveSessions(
            sessions.first().filterNot { it.isSameAs(session) },
        )
        if (isCurrent) {
            credentialStore.clear()
            clearLocalData()
        }
    }

    /**
     * Asks the server who is logged in, for a login stored before the app knew or a user who was
     * renamed.
     *
     * @throws GrocyApiException e.g. offline; the session then stays as it is.
     */
    suspend fun refreshCurrentUser() {
        val connection = credentialStore.connection.first() ?: return
        val user = grocyApi.currentUser(connection)
        credentialStore.saveSessions(
            sessionsAfterLogin(sessions.first(), connection, user),
        )
    }
}
