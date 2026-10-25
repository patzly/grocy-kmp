package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.model.DemoServer
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApiException

/** Logs in to another of Grocy's public demos. */
internal fun interface DemoServerSwitcher {

    /** @throws GrocyApiException if [server] cannot be reached; nothing changed then. */
    suspend fun switchTo(server: DemoServer)
}

/**
 * Checks the demo first, so a demo that is down never replaces a working one. The new login
 * clears the data of the former demo, the configuration is downloaded right away, so screens
 * show the new demo's features.
 */
internal fun DemoServerSwitcher(
    authRepository: AuthRepository,
    serverRepository: ServerRepository,
) = DemoServerSwitcher { server ->
    val connection = ServerConnection.Demo(server.url)
    authRepository.verify(connection)
    authRepository.saveLogin(connection)
    try {
        serverRepository.reloadConfiguration()
    } catch (_: GrocyApiException) {
        // The screens download it themselves once the demo answers again
    }
}
