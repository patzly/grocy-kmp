package com.patrickzedler.grocy.feature.settings.impl.server

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.server.GrocyVersionCheck
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.DemoServer
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.feature.settings.impl.DemoServerSwitcher
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Errors the screen shows as snackbar. */
internal enum class ServerSettingsError {
    DEMO_SERVER_UNREACHABLE,
    VERSION_CHECK_FAILED,
}

internal class ServerSettingsViewModel(
    private val serverRepository: ServerRepository,
    private val authRepository: AuthRepository,
    private val demoServerSwitcher: DemoServerSwitcher,
    timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    /**
     * Read at app start, so the items show the server in the first frame. Keeps the server
     * after a logout, while the screen slides out.
     */
    val server: StateFlow<ServerState?> = serverRepository.server
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, serverRepository.server.value)

    /** True while the server's version is asked for, until then it may be unknown. */
    var isLoadingVersion by mutableStateOf(false)
        private set

    /** True once the configuration was reloaded, until the user chose to restart or not. */
    var isRestartOffered by mutableStateOf(false)
        private set

    /** True while another demo is being switched to. */
    var isSwitchingDemoServer by mutableStateOf(false)
        private set

    /** True while Grocy's newest version is looked up. */
    var isCheckingVersion by mutableStateOf(false)
        private set

    /** The answer of a version check for its dialog, until closed; failures are an [error]. */
    var versionCheck by mutableStateOf<GrocyVersionCheck?>(null)
        private set

    var error by mutableStateOf<ServerSettingsError?>(null)
        private set

    private val reloadController = RefreshController(viewModelScope, timeSource) {
        serverRepository.reloadConfiguration()
        isRestartOffered = true
    }

    val reloadState: StateFlow<RefreshState> = reloadController.state

    init {
        // An update of the server changes its version, the stored one may be outdated
        isLoadingVersion = true
        viewModelScope.launch {
            try {
                serverRepository.refreshVersion()
            } catch (_: GrocyApiException) {
                // Offline: the stored version stays
            } finally {
                isLoadingVersion = false
            }
        }
    }

    fun reloadConfiguration() {
        reloadController.refresh()
    }

    fun onReloadErrorShown() {
        reloadController.onErrorShown()
    }

    fun onRestartAnswered() {
        isRestartOffered = false
    }

    /** Logs in to [demo] instead of the current demo, which stays if [demo] does not answer. */
    fun switchDemoServer(demo: DemoServer) {
        if (isSwitchingDemoServer || demo.url == server.value?.connection?.serverUrl) return
        isSwitchingDemoServer = true
        viewModelScope.launch {
            try {
                demoServerSwitcher.switchTo(demo)
            } catch (_: GrocyApiException) {
                error = ServerSettingsError.DEMO_SERVER_UNREACHABLE
            } finally {
                isSwitchingDemoServer = false
            }
        }
    }

    fun checkGrocyVersion() {
        if (isCheckingVersion) return
        isCheckingVersion = true
        viewModelScope.launch {
            try {
                val check = serverRepository.checkGrocyVersion()
                if (check == GrocyVersionCheck.Failed) {
                    error = ServerSettingsError.VERSION_CHECK_FAILED
                } else {
                    versionCheck = check
                }
            } finally {
                isCheckingVersion = false
            }
        }
    }

    fun onVersionCheckShown() {
        versionCheck = null
    }

    fun onErrorShown() {
        error = null
    }

    /** Returns to the login through the auth state, which every screen follows. */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
