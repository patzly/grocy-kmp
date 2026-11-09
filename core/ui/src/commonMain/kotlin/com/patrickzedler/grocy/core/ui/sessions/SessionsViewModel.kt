package com.patrickzedler.grocy.core.ui.sessions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Why another session could not be opened; the current one stays then. */
enum class SessionError { UNREACHABLE, REJECTED, UNTRUSTED, FAILED }

/** The sessions kept on the device for the start screen's profile and the login. */
class SessionsViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val sessions: StateFlow<List<SavedSession>> =
        authRepository.sessions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The session being opened, which its server has to confirm first. */
    var switching by mutableStateOf<SavedSession?>(null)
        private set

    var error by mutableStateOf<SessionError?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                authRepository.refreshCurrentUser()
            } catch (_: GrocyApiException) {
                // Offline: the profile keeps what it knows
            }
        }
    }

    /** Logs in to [session] and then calls [onSwitched]; the current login stays if it fails. */
    fun switchTo(session: SavedSession, onSwitched: () -> Unit = {}) {
        if (switching != null) return
        switching = session
        error = null
        viewModelScope.launch {
            try {
                authRepository.switchTo(session)
                // Until the list has followed, the profile would show the former session again
                sessions.first { it.firstOrNull()?.connection == session.connection }
                onSwitched()
            } catch (e: GrocyApiException) {
                error = when (e) {
                    is GrocyApiException.Unreachable -> SessionError.UNREACHABLE
                    is GrocyApiException.Unauthorized,
                    is GrocyApiException.HomeAssistantTokenRejected,
                    -> SessionError.REJECTED
                    is GrocyApiException.UntrustedCertificate -> SessionError.UNTRUSTED
                    else -> SessionError.FAILED
                }
            } finally {
                switching = null
            }
        }
    }

    fun onErrorShown() {
        error = null
    }

    fun remove(session: SavedSession) {
        viewModelScope.launch { authRepository.removeSession(session) }
    }

    /** Back to the login through the auth state; the session stays for later. */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
