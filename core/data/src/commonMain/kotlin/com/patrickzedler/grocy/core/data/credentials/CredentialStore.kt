package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.coroutines.flow.Flow

/**
 * Persists the connection of the logged-in server, including its secrets.
 *
 * Implementations must exist only once per process, the app graph takes care of that.
 */
interface CredentialStore {

    /** The stored connection, or null when logged out. Emits again on every change. */
    val connection: Flow<ServerConnection?>

    suspend fun save(connection: ServerConnection)

    suspend fun clear()
}
