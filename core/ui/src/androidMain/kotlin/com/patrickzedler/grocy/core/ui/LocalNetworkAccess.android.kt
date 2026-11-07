package com.patrickzedler.grocy.core.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Whether the app may reach the local network: always before Android 17, which blocks it for
 * apps targeting it without `ACCESS_LOCAL_NETWORK`.
 */
fun hasLocalNetworkPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN ||
        context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) ==
        PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberLocalNetworkAccess(onResult: (granted: Boolean) -> Unit): LocalNetworkAccess? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return null
    return rememberAndroidLocalNetworkAccess(onResult)
}

@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
@Composable
private fun rememberAndroidLocalNetworkAccess(
    onResult: (granted: Boolean) -> Unit,
): LocalNetworkAccess {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnResult by rememberUpdatedState(onResult)
    var permission by remember { mutableStateOf(context.localNetworkPermission(activity)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permission = context.localNetworkPermission(activity)
        currentOnResult(granted)
    }

    // The user may change it in the settings and come back
    LifecycleResumeEffect(context) {
        permission = context.localNetworkPermission(activity)
        onPauseOrDispose {}
    }

    return remember(launcher, context) {
        object : LocalNetworkAccess {
            override val permission get() = permission

            override fun request() {
                context.markLocalNetworkRequested()
                launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }

            override fun openSettings() {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    // Some devices leave out the app's settings, nothing else leads there
                }
            }
        }
    }
}

/**
 * Android tells "denied for good" only through a missing rationale after it was asked, and
 * this question comes from several places and process starts, so whether it was asked is kept.
 */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
private fun Context.localNetworkPermission(activity: Activity?): LocalNetworkPermission = when {
    hasLocalNetworkPermission(this) -> LocalNetworkPermission.Granted
    wasLocalNetworkRequested() && activity?.shouldShowRequestPermissionRationale(
        Manifest.permission.ACCESS_LOCAL_NETWORK,
    ) == false -> LocalNetworkPermission.Denied
    else -> LocalNetworkPermission.Requestable
}

private fun Context.wasLocalNetworkRequested(): Boolean =
    getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(KEY_REQUESTED, false)

private fun Context.markLocalNetworkRequested() {
    getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit { putBoolean(KEY_REQUESTED, true) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Excluded from backups: a restored device has not asked yet
private const val PREFERENCES = "local_network"
private const val KEY_REQUESTED = "requested"
