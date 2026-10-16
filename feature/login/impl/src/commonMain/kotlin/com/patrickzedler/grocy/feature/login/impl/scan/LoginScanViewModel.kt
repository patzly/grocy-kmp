package com.patrickzedler.grocy.feature.login.impl.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.util.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.core.data.util.withMinimumDuration
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.scanner.Barcode
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.toLoginError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LoginScanUiState(
    val isLoggingIn: Boolean = false,
    val error: LoginError? = null,
)

internal class LoginScanViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginScanUiState())
    val uiState: StateFlow<LoginScanUiState> = _uiState.asStateFlow()

    fun onBarcodeScanned(barcode: Barcode) {
        if (_uiState.value.isLoggingIn) return
        when (val code = LoginQrCode.parse(barcode.value)) {
            is LoginQrCode.ApiKey -> login(code.connection)
            LoginQrCode.CalendarLink -> _uiState.update { it.copy(error = LoginError.CalendarCode) }
            LoginQrCode.Invalid -> _uiState.update { it.copy(error = LoginError.NotLoginCode) }
        }
    }

    private fun login(scanned: ServerConnection.ApiKey) {
        _uiState.update { it.copy(isLoggingIn = true, error = null) }
        viewModelScope.launch {
            try {
                val connection = withMinimumDuration(MINIMUM_LOADING_DURATION) {
                    verifyFirstWorking(candidatesOf(scanned))
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

    /**
     * Grocy writes http:// into the code when a reverse proxy does not forward the scheme, even
     * if the server is reached through HTTPS. HTTPS is tried first, so the key is not sent
     * unencrypted when it does not have to be.
     */
    private fun candidatesOf(scanned: ServerConnection.ApiKey): List<ServerConnection.ApiKey> {
        val url = scanned.serverUrl
        if (!url.startsWith("http://")) return listOf(scanned)
        return listOf(scanned.copy(serverUrl = "https://" + url.removePrefix("http://")), scanned)
    }

    private suspend fun verifyFirstWorking(
        candidates: List<ServerConnection.ApiKey>,
    ): ServerConnection.ApiKey {
        candidates.dropLast(1).forEach { candidate ->
            try {
                authRepository.verify(candidate)
                return candidate
            } catch (e: GrocyApiException.Unauthorized) {
                // The server was found, a wrong key stays wrong with another scheme
                throw e
            } catch (_: GrocyApiException) {
                // Not reachable this way, try the next candidate
            }
        }
        return candidates.last().also { authRepository.verify(it) }
    }
}
