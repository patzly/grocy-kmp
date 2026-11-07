package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import com.patrickzedler.grocy.core.model.network.NetworkSettings
import com.patrickzedler.grocy.core.model.network.networkProxyOf
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Tor and the HTTP proxy, read once at app start in [scope], so the network page shows the stored
 * values in its first frame and no request leaves before they are known. Requests use the
 * settings the app started with: an HTTP client keeps connections open, and one opened before a
 * change would else carry requests past the new proxy.
 */
class NetworkSettingsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    // The first stored value, kept before anyone sees it, so a change can never become it
    @Volatile
    private var startSettings: NetworkSettings? = null

    /** Null only for the moment at app start until the stored settings are read. */
    val settings: StateFlow<NetworkSettings?> = settingsStore.networkSettings
        .onEach { if (startSettings == null) startSettings = it }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Where this process sends requests, null until the settings are read. */
    val proxyInProcess: NetworkProxy?
        get() = startSettings?.let(::networkProxyOf)

    /** Where every request of this process goes; waits for the stored settings at app start. */
    suspend fun proxy(): NetworkProxy {
        settings.value ?: settings.filterNotNull().first()
        return networkProxyOf(checkNotNull(startSettings))
    }

    /** Whether [settings] send requests elsewhere than this process does until a restart. */
    fun restartRequired(settings: NetworkSettings): Boolean =
        proxyInProcess.let { it != null && it != networkProxyOf(settings) }

    /** Changes the current settings; nothing happens before they are read. */
    suspend fun update(transform: (NetworkSettings) -> NetworkSettings) {
        val current = settings.value ?: return
        settingsStore.setNetworkSettings(transform(current))
    }
}
