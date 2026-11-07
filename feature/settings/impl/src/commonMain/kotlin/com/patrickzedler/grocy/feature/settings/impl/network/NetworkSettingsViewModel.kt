package com.patrickzedler.grocy.feature.settings.impl.network

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.settings.NetworkSettingsRepository
import com.patrickzedler.grocy.core.model.network.MAX_PORT
import com.patrickzedler.grocy.core.model.network.NetworkSettings
import com.patrickzedler.grocy.core.model.network.isValidProxyHost
import com.patrickzedler.grocy.core.model.network.isValidProxyPort
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Tor and the HTTP proxy, kept on the device. The switches are saved at once, host and port once
 * typing pauses and only when valid. The app keeps the way it started with until a restart,
 * which a changed Tor setting offers right away.
 */
@OptIn(FlowPreview::class)
internal class NetworkSettingsViewModel(
    private val repository: NetworkSettingsRepository,
) : ViewModel() {

    /** Read at app start, so the page shows the stored values in its first frame. */
    val settings: StateFlow<NetworkSettings?> = repository.settings

    val host = TextFieldState(settings.value?.httpProxyHost.orEmpty())

    val port = TextFieldState(
        (settings.value?.httpProxyPort ?: NetworkSettings.DEFAULT_HTTP_PROXY_PORT).toString(),
    )

    /** Whether the typed host is no host name or address. */
    var hostInvalid by mutableStateOf(false)
        private set

    var portError by mutableStateOf<SettingError?>(null)
        private set

    /** True once Tor was switched, until the user chose to restart or not. */
    var isRestartOffered by mutableStateOf(false)
        private set

    /** The host takes the focus once the proxy is switched on without one. */
    var focusesHost by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            snapshotFlow { host.text.toString() }
                .drop(1)
                .debounce(TYPING_DELAY_MILLIS)
                .collect(::saveHost)
        }
        viewModelScope.launch {
            snapshotFlow { port.text.toString() }
                .drop(1)
                .debounce(TYPING_DELAY_MILLIS)
                .collect(::savePort)
        }
    }

    /** Whether [settings] send requests elsewhere than the app does until it restarts. */
    fun restartRequired(settings: NetworkSettings): Boolean = repository.restartRequired(settings)

    fun setUseTor(useTor: Boolean) {
        val current = settings.value ?: return
        val changed = current.copy(useTor = useTor)
        viewModelScope.launch { repository.update { it.copy(useTor = useTor) } }
        // Switched back to how the app runs, nothing needs a restart
        isRestartOffered = repository.restartRequired(changed)
    }

    fun setHttpProxyEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.update { it.copy(httpProxyEnabled = enabled) } }
        focusesHost = enabled && host.text.isBlank()
    }

    fun onRestartAnswered() {
        isRestartOffered = false
    }

    private suspend fun saveHost(text: String) {
        val trimmed = text.trim()
        hostInvalid = !isValidProxyHost(trimmed)
        if (!hostInvalid) repository.update { it.copy(httpProxyHost = trimmed) }
    }

    private suspend fun savePort(text: String) {
        val port = text.trim().toIntOrNull()?.takeIf(::isValidProxyPort)
        // Grocy's number message, the range without separators like ports are written
        portError = if (port == null) PortError else null
        if (port != null) repository.update { it.copy(httpProxyPort = port) }
    }

    internal companion object {
        const val TYPING_DELAY_MILLIS = 800L

        val PortError = SettingError.InvalidNumber(min = "1", max = MAX_PORT.toString(), decimals = 0)
    }
}
