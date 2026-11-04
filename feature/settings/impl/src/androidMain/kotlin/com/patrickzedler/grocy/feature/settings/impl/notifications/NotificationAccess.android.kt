package com.patrickzedler.grocy.feature.settings.impl.notifications

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
internal actual fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    // Android tells "denied for good" only through a missing rationale after a request
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var permission by remember { mutableStateOf(context.permission(activity, hasRequested)) }
    var exactAlarmsBlocked by remember { mutableStateOf(context.exactAlarmsBlocked()) }
    var batteryOptimized by remember { mutableStateOf(context.batteryOptimized()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        hasRequested = true
        permission = context.permission(activity, hasRequested = true)
    }

    // The user may change all of them in the settings and come back
    LifecycleResumeEffect(hasRequested) {
        permission = context.permission(activity, hasRequested)
        exactAlarmsBlocked = context.exactAlarmsBlocked()
        batteryOptimized = context.batteryOptimized()
        onPauseOrDispose {}
    }

    return remember(launcher, context) {
        object : NotificationAccess {
            override val permission get() = permission
            override val canOpenSettings = true
            override val exactAlarmsBlocked get() = exactAlarmsBlocked
            override val batteryOptimized get() = batteryOptimized
            override val onlyWhileOpen = false

            override fun request() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openSettings()
                }
            }

            override fun openSettings() {
                context.open(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    },
                )
            }

            override fun openExactAlarmSettings() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.open(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }

            // The list of apps: asking directly would need a permission Google Play restricts
            override fun openBatterySettings() {
                context.open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

private fun Context.permission(activity: Activity?, hasRequested: Boolean): NotificationPermission {
    val canRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return when {
        NotificationManagerCompat.from(this).areNotificationsEnabled() ->
            NotificationPermission.Granted
        // Before Android 13 there is no question to ask, only the settings
        !canRequest -> NotificationPermission.Denied
        hasRequested && activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.POST_NOTIFICATIONS,
        ) == false -> NotificationPermission.Denied
        else -> NotificationPermission.Requestable
    }
}

private fun Context.exactAlarmsBlocked(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun Context.batteryOptimized(): Boolean =
    !getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

private fun Context.open(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Some devices leave out these settings, nothing else leads there
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
