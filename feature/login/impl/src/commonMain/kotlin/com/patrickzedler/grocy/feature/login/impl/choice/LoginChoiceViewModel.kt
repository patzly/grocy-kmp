package com.patrickzedler.grocy.feature.login.impl.choice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.feature.login.impl.toLoginError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginChoiceViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginChoiceUiState())
    val uiState: StateFlow<LoginChoiceUiState> = _uiState.asStateFlow()

    fun loginWithDemoServer() {
        if (_uiState.value.isLoggingIn) return
        _uiState.update { it.copy(isLoggingIn = true, error = null) }
        viewModelScope.launch {
            try {
                authRepository.login(ServerConnection.Demo())
                // On success the app switches to the start screen by itself, see GrocyApp
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoggingIn = false, error = e.toLoginError()) }
            }
        }
    }
}
