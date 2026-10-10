package com.patrickzedler.grocy.feature.login.impl.manual

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.ServerUrl
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.feature.login.impl.toLoginError
import com.patrickzedler.grocy.feature.login.impl.withMinimumDuration
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

internal class LoginManualViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val grocyUrl = TextFieldState()
    val homeAssistantUrl = TextFieldState()
    val homeAssistantToken = TextFieldState()
    val ingressProxyId = TextFieldState()

    // Needed in both modes, so switching keeps it
    val apiKey = TextFieldState()

    var serverType by mutableStateOf(ServerType.GROCY)
        private set

    var scheme by mutableStateOf(ServerUrl.Scheme.Https)
        private set

    var isLoggingIn by mutableStateOf(false)
        private set

    var error by mutableStateOf<LoginError?>(null)
        private set

    /** Base URL of the Grocy server, null while the input is no usable address. */
    val normalizedGrocyUrl: String? by derivedStateOf {
        ServerUrl.normalize(grocyUrl.text.toString(), scheme)
    }

    /** Base URL of Home Assistant, null while the input is no usable address. */
    val normalizedHomeAssistantUrl: String? by derivedStateOf {
        ServerUrl.normalize(homeAssistantUrl.text.toString(), scheme)
    }

    val canConnect: Boolean by derivedStateOf {
        // Home Assistant needs the ingress session, which is not implemented yet
        !isLoggingIn &&
            serverType == ServerType.GROCY &&
            normalizedGrocyUrl != null &&
            apiKey.text.isNotBlank()
    }

    fun selectServerType(type: ServerType) {
        serverType = type
        error = null
    }

    fun selectScheme(newScheme: ServerUrl.Scheme) {
        scheme = newScheme
        error = null
    }

    fun connect() {
        if (!canConnect) return
        val serverUrl = normalizedGrocyUrl ?: return
        val connection = ServerConnection.ApiKey(
            serverUrl = serverUrl,
            apiKey = apiKey.text.toString().trim(),
        )

        isLoggingIn = true
        error = null
        viewModelScope.launch {
            try {
                withMinimumDuration(MINIMUM_LOADING_DURATION) {
                    authRepository.verify(connection)
                }
                // Saving switches the app to the start screen, see GrocyApp
                authRepository.saveLogin(connection)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isLoggingIn = false
                error = e.toLoginError()
            }
        }
    }
}
