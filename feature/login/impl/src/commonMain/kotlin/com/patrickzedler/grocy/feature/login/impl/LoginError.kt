package com.patrickzedler.grocy.feature.login.impl

import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.login_error_not_grocy
import com.patrickzedler.grocy.core.resources.login_error_unauthorized
import com.patrickzedler.grocy.core.resources.login_error_unknown
import com.patrickzedler.grocy.core.resources.login_error_unreachable
import org.jetbrains.compose.resources.stringResource

internal enum class LoginError {
    Unreachable,
    Unauthorized,
    NotGrocyServer,
    Unknown,
}

internal fun Throwable.toLoginError(): LoginError =
    when (this) {
        is GrocyApiException.Unreachable -> LoginError.Unreachable
        is GrocyApiException.Unauthorized -> LoginError.Unauthorized
        is GrocyApiException.NotGrocyServer -> LoginError.NotGrocyServer
        else -> LoginError.Unknown
    }

@Composable
internal fun LoginError.message(): String =
    stringResource(
        when (this) {
            LoginError.Unreachable -> Res.string.login_error_unreachable
            LoginError.Unauthorized -> Res.string.login_error_unauthorized
            LoginError.NotGrocyServer -> Res.string.login_error_not_grocy
            LoginError.Unknown -> Res.string.login_error_unknown
        },
    )
