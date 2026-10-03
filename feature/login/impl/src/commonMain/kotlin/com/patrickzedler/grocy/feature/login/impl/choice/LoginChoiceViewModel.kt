package com.patrickzedler.grocy.feature.login.impl.choice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.feature.login.impl.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.feature.login.impl.toLoginError
import com.patrickzedler.grocy.feature.login.impl.withMinimumDuration
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
            val connection = ServerConnection.Demo()
            try {
                withMinimumDuration(MINIMUM_LOADING_DURATION) {
                    authRepository.verify(connection)
                }
                // Saving switches the app to the start screen, see GrocyApp
                authRepository.saveLogin(connection)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoggingIn = false, error = e.toLoginError()) }
            }
        }
    }
}
