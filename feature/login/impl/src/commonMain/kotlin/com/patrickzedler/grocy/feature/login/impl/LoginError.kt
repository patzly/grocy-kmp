package com.patrickzedler.grocy.feature.login.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.login_error_addon_not_found
import com.patrickzedler.grocy.core.resources.login_error_calendar_code
import com.patrickzedler.grocy.core.resources.login_error_ha_token_rejected
import com.patrickzedler.grocy.core.resources.login_error_not_grocy
import com.patrickzedler.grocy.core.resources.login_error_not_home_assistant
import com.patrickzedler.grocy.core.resources.login_error_not_login_code
import com.patrickzedler.grocy.core.resources.login_error_unauthorized
import com.patrickzedler.grocy.core.resources.login_error_unknown
import com.patrickzedler.grocy.core.resources.login_error_unreachable
import org.jetbrains.compose.resources.stringResource

internal enum class LoginError {
    Unreachable,
    Unauthorized,
    NotGrocyServer,
    HomeAssistantTokenRejected,
    AddonNotFound,
    NotHomeAssistant,

    /** A scanned code that is no Grocy login code. */
    NotLoginCode,

    /** The QR code of a calendar link from the API key page, not of an API key. */
    CalendarCode,
    Unknown,
}

internal fun Throwable.toLoginError(): LoginError =
    when (this) {
        is GrocyApiException.Unreachable -> LoginError.Unreachable
        is GrocyApiException.Unauthorized -> LoginError.Unauthorized
        is GrocyApiException.NotGrocyServer -> LoginError.NotGrocyServer
        // Login only calls endpoints every Grocy server has, so they are missing at this address
        is GrocyApiException.HttpError if statusCode == 404 -> LoginError.NotGrocyServer
        is GrocyApiException.HomeAssistantTokenRejected -> LoginError.HomeAssistantTokenRejected
        is GrocyApiException.AddonNotFound -> LoginError.AddonNotFound
        is GrocyApiException.NotHomeAssistant -> LoginError.NotHomeAssistant
        else -> LoginError.Unknown
    }

@Composable
internal fun LoginError.message(): String =
    stringResource(
        when (this) {
            LoginError.Unreachable -> Res.string.login_error_unreachable
            LoginError.Unauthorized -> Res.string.login_error_unauthorized
            LoginError.NotGrocyServer -> Res.string.login_error_not_grocy
            LoginError.HomeAssistantTokenRejected -> Res.string.login_error_ha_token_rejected
            LoginError.AddonNotFound -> Res.string.login_error_addon_not_found
            LoginError.NotHomeAssistant -> Res.string.login_error_not_home_assistant
            LoginError.NotLoginCode -> Res.string.login_error_not_login_code
            LoginError.CalendarCode -> Res.string.login_error_calendar_code
            LoginError.Unknown -> Res.string.login_error_unknown
        },
    )

/** The error as segmented item, the last one of the group it belongs to. */
@Composable
internal fun LoginErrorItem(
    error: LoginError,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    SegmentedContainer(index = index, count = count, modifier = modifier) {
        Text(
            text = error.message(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            // Screen readers announce the error as soon as it appears
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
