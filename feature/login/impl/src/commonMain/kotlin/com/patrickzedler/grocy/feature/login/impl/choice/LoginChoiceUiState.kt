package com.patrickzedler.grocy.feature.login.impl.choice

import com.patrickzedler.grocy.feature.login.impl.LoginError

internal data class LoginChoiceUiState(
    val isLoggingIn: Boolean = false,
    val error: LoginError? = null,
)
