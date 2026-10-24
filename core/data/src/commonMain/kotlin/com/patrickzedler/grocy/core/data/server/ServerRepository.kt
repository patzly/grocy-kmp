package com.patrickzedler.grocy.core.data.server

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The logged in server as the server settings show it. */
data class ServerState(
    val connection: ServerConnection,
    /** Null until the server's version was downloaded once. */
    val grocyVersion: String?,
) {
    /** Grocy's public demo, where everything entered is visible to everyone. */
    val isDemo: Boolean
        get() = connection is ServerConnection.Demo
}

/**
 * The logged in server, read once at app start ([scope] is the app's), so the server settings
 * show it in their first frame.
 *
 * @param connection the stored connection, null when logged out.
 */
class ServerRepository(
    connection: Flow<ServerConnection?>,
    private val masterDataRepository: MasterDataRepository,
    scope: CoroutineScope,
) {

    /** Null while logged out and for the moment until the stored login is read. */
    val server: StateFlow<ServerState?> =
        combine(connection, masterDataRepository.systemInfo) { connection, systemInfo ->
            connection?.let { ServerState(it, systemInfo?.grocyVersion) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The stored configuration of the server, null for the moment until it is read. Screens get
     * it without waiting for the database, which a running download can keep busy for a while.
     */
    val serverConfig: StateFlow<ServerConfig?> =
        masterDataRepository.serverConfig.stateIn(scope, SharingStarted.Eagerly, null)

    /** @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline. */
    suspend fun reloadConfiguration() {
        masterDataRepository.reloadConfiguration()
    }

    /** @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline. */
    suspend fun refreshVersion() {
        masterDataRepository.refreshSystemInfo()
    }
}
