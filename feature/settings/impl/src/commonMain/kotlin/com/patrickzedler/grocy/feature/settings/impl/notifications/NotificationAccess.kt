package com.patrickzedler.grocy.feature.settings.impl.notifications

import androidx.compose.runtime.Composable

/** Whether the platform lets the app notify. */
internal enum class NotificationPermission {
    Granted,

    /** Asking shows the platform's question. */
    Requestable,

    /** Only the settings of the system or browser can change it. */
    Denied,
}

/** What the platform needs for notifications to come, and on time. */
internal interface NotificationAccess {
    val permission: NotificationPermission

    /** Pages cannot open the settings of the browser, Android can. */
    val canOpenSettings: Boolean

    /** Android only delivers on the minute with this special access, from Android 14 on asked. */
    val exactAlarmsBlocked: Boolean

    /** Android's battery optimization may keep the app from the network when an alarm fires. */
    val batteryOptimized: Boolean

    /** A browser only runs the app while it is open. */
    val onlyWhileOpen: Boolean

    fun request()

    fun openSettings()

    fun openExactAlarmSettings()

    fun openBatterySettings()
}

@Composable
internal expect fun rememberNotificationAccess(): NotificationAccess
