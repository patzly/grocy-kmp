package com.patrickzedler.grocy.core.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

internal enum class CameraPermission {
    /** Not known yet, e.g. while the browser is asked. */
    Unknown,
    Granted,

    /** Not granted, asking shows the system or browser dialog. */
    Requestable,

    /** Denied for good: only the app or site settings can change it. */
    Denied,
}

/** Camera permission of the platform. Asks only through [request], never by itself. */
@Stable
internal interface CameraAccess {
    val permission: CameraPermission

    /** Whether [openSettings] can show a page where the user allows the camera. */
    val canOpenSettings: Boolean

    fun request()

    fun openSettings()
}

@Composable
internal expect fun rememberCameraAccess(): CameraAccess
