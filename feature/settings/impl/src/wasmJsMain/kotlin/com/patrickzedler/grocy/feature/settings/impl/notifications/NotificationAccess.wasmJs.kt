@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.feature.settings.impl.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

@Composable
internal actual fun rememberNotificationAccess(): NotificationAccess {
    var permission by remember { mutableStateOf(notificationPermissionOf(browserPermission())) }
    val scope = rememberCoroutineScope()

    return remember(scope) {
        object : NotificationAccess {
            override val permission get() = permission

            // Pages cannot open the site settings of the browser
            override val canOpenSettings = false
            override val exactAlarmsBlocked = false
            override val batteryOptimized = false
            override val onlyWhileOpen = true

            override fun request() {
                scope.launch {
                    val state = requestBrowserPermission().await<JsString>().toString()
                    permission = notificationPermissionOf(state)
                }
            }

            override fun openSettings() = Unit

            override fun openExactAlarmSettings() = Unit

            override fun openBatterySettings() = Unit
        }
    }
}

/** The browser's word for the permission; "default" means it was not asked yet. */
internal fun notificationPermissionOf(state: String): NotificationPermission = when (state) {
    "granted" -> NotificationPermission.Granted
    "default" -> NotificationPermission.Requestable
    else -> NotificationPermission.Denied
}

@JsFun("() => typeof Notification === 'undefined' ? 'denied' : Notification.permission")
private external fun browserPermission(): String

@JsFun(
    """() => typeof Notification === 'undefined'
        ? Promise.resolve('denied')
        : Notification.requestPermission()""",
)
private external fun requestBrowserPermission(): Promise<JsString>
