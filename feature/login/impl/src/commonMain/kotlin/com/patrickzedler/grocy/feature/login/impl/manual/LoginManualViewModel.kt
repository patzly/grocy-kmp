package com.patrickzedler.grocy.feature.login.impl.manual

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.util.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.core.data.util.withMinimumDuration
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.ServerUrl
import com.patrickzedler.grocy.core.network.isHomeAssistantIngressSupported
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.toLoginError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * [isHomeAssistantSupported] is false in browsers, which cannot use ingress; the screen then
 * explains the alternative instead of offering a login that cannot work.
 */
internal class LoginManualViewModel(
    private val authRepository: AuthRepository,
    val isHomeAssistantSupported: Boolean = isHomeAssistantIngressSupported,
) : ViewModel() {

    val grocyUrl = TextFieldState()
    val homeAssistantUrl = TextFieldState()
    val homeAssistantToken = TextFieldState()
    val addonSlug = TextFieldState(ServerConnection.HomeAssistant.DEFAULT_ADDON_SLUG)

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
        !isLoggingIn && connection() != null
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
        if (isLoggingIn) return
        val connection = connection() ?: return

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

    /** The connection the input describes, null while it is incomplete. */
    private fun connection(): ServerConnection? {
        val key = apiKey.text.toString().trim().takeIf { it.isNotEmpty() } ?: return null
        return when (serverType) {
            ServerType.GROCY -> ServerConnection.ApiKey(
                serverUrl = normalizedGrocyUrl ?: return null,
                apiKey = key,
            )

            ServerType.HOME_ASSISTANT -> {
                if (!isHomeAssistantSupported) return null
                ServerConnection.HomeAssistant(
                    serverUrl = normalizedHomeAssistantUrl ?: return null,
                    accessToken = homeAssistantToken.text.toString().trim()
                        .takeIf { it.isNotEmpty() } ?: return null,
                    apiKey = key,
                    addonSlug = addonSlug.text.toString().trim()
                        .takeIf { it.isNotEmpty() } ?: return null,
                )
            }
        }
    }
}
