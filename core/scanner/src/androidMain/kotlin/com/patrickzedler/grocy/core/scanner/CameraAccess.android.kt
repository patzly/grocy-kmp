package com.patrickzedler.grocy.core.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
internal actual fun rememberCameraAccess(): CameraAccess {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    // Android tells "denied for good" only through a missing rationale after a request
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var permission by remember { mutableStateOf(context.cameraPermission(activity, hasRequested)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        hasRequested = true
        permission = context.cameraPermission(activity, hasRequested = true)
    }

    // The user may change the permission in the settings and come back
    LifecycleResumeEffect(hasRequested) {
        permission = context.cameraPermission(activity, hasRequested)
        onPauseOrDispose {}
    }

    return remember(launcher, context) {
        object : CameraAccess {
            override val permission get() = permission
            override val canOpenSettings = true

            override fun request() = launcher.launch(Manifest.permission.CAMERA)

            override fun openSettings() {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}

private fun Context.cameraPermission(activity: Activity?, hasRequested: Boolean): CameraPermission {
    val isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    return when {
        isGranted -> CameraPermission.Granted
        hasRequested && activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.CAMERA,
        ) == false -> CameraPermission.Denied
        else -> CameraPermission.Requestable
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
