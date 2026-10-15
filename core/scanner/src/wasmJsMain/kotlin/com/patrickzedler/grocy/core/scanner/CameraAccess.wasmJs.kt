@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

@Composable
internal actual fun rememberCameraAccess(): CameraAccess {
    var permission by remember { mutableStateOf(CameraPermission.Unknown) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        permission = cameraPermissionOf(queryCameraPermission().await<JsString>().toString())
    }

    return remember(scope) {
        object : CameraAccess {
            override val permission get() = permission

            // Pages cannot open the site settings of the browser
            override val canOpenSettings = false

            override fun request() {
                scope.launch {
                    val state = requestCameraPermission().await<JsString>().toString()
                    permission = cameraPermissionOf(state)
                }
            }

            override fun openSettings() = Unit
        }
    }
}

private fun cameraPermissionOf(state: String): CameraPermission = when (state) {
    "granted" -> CameraPermission.Granted
    "denied" -> CameraPermission.Denied
    // The camera then fails to start and the scanner explains why
    "unsupported" -> CameraPermission.Granted
    else -> CameraPermission.Requestable
}

/** "granted", "denied", "prompt" or "unsupported" (no camera API, e.g. without HTTPS). */
private fun queryCameraPermission(): Promise<JsString> = js(
    """(() => {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            return Promise.resolve('unsupported');
        }
        if (!navigator.permissions || !navigator.permissions.query) {
            return Promise.resolve('prompt');
        }
        return navigator.permissions.query({ name: 'camera' })
            .then((status) => status.state)
            .catch(() => 'prompt');
    })()""",
)

/**
 * Shows the browser's permission dialog. A dismissed dialog also rejects, so the stored state
 * decides whether the browser would ask again.
 */
private fun requestCameraPermission(): Promise<JsString> = js(
    """navigator.mediaDevices.getUserMedia({ video: true, audio: false })
        .then((stream) => {
            stream.getTracks().forEach((track) => track.stop());
            return 'granted';
        })
        .catch((error) => {
            if (!error || (error.name !== 'NotAllowedError' && error.name !== 'SecurityError')) {
                return 'granted';
            }
            if (!navigator.permissions || !navigator.permissions.query) return 'prompt';
            return navigator.permissions.query({ name: 'camera' })
                .then((status) => status.state === 'denied' ? 'denied' : 'prompt')
                .catch(() => 'prompt');
        })""",
)
