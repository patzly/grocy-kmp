package com.patrickzedler.grocy.core.data.auth

import com.patrickzedler.grocy.core.model.ServerConnection

sealed interface AuthState {

    data object LoggedOut : AuthState

    data class LoggedIn(
        val connection: ServerConnection,
    ) : AuthState
}
