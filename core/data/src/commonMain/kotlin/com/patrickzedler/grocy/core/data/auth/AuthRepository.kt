package com.patrickzedler.grocy.core.data.auth

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val credentialStore: CredentialStore,
    private val grocyApi: GrocyApi,
) {

    val authState: Flow<AuthState> = credentialStore.connection.map { connection ->
        if (connection == null) AuthState.LoggedOut else AuthState.LoggedIn(connection)
    }

    /**
     * Verifies that [connection] reaches a Grocy server that accepts it, then stores it.
     *
     * @throws GrocyApiException if the server is unreachable, rejects the key or is no Grocy server.
     */
    suspend fun login(connection: ServerConnection) {
        // Fails early with a clear error if the address is no Grocy server at all
        grocyApi.systemInfo(connection)
        // Requires authentication, so this is where a wrong API key shows up
        grocyApi.currentUser(connection)
        credentialStore.save(connection)
    }

    suspend fun logout() {
        credentialStore.clear()
    }
}
