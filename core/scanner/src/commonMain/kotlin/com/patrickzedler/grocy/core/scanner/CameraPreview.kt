package com.patrickzedler.grocy.core.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

internal sealed interface CameraState {
    data object Starting : CameraState

    data class Running(val hasTorch: Boolean) : CameraState

    data class Failed(val reason: CameraFailure) : CameraState
}

internal enum class CameraFailure {
    NoCamera,
    PermissionDenied,

    /** The platform offers no camera access here, e.g. a browser page without HTTPS. */
    Unsupported,
    Error,
}

/** Work on one camera frame off the main thread, e.g. decoding its barcodes. */
internal fun interface FrameAnalyzer<T> {

    /** The result of [frame], which stays owned by the camera. */
    suspend fun analyze(frame: CameraFrame): T
}

/**
 * Shows the camera image and passes the result of [analyzer] for every frame to [onResult] on
 * the main thread.
 *
 * Starts the camera when it enters the composition and stops it when it leaves. Analyzes only
 * while [isAnalyzing]. [shape] rounds the image where the platform cannot clip it otherwise.
 */
@Composable
internal expect fun <T> CameraPreview(
    analyzer: FrameAnalyzer<T>,
    isAnalyzing: Boolean,
    torchEnabled: Boolean,
    shape: Shape,
    onResult: (T) -> Unit,
    onStateChange: (CameraState) -> Unit,
    modifier: Modifier = Modifier,
)
