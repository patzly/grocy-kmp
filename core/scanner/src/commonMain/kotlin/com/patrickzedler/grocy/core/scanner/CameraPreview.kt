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

/**
 * Shows the camera image and passes the decoded barcodes of every frame to [onBarcodes].
 *
 * Starts the camera when it enters the composition and stops it when it leaves. Decodes only
 * while [isDecoding]. [shape] rounds the image where the platform cannot clip it otherwise.
 */
@Composable
internal expect fun CameraPreview(
    decoderFactory: BarcodeDecoderFactory,
    formats: Set<BarcodeFormat>,
    isDecoding: Boolean,
    torchEnabled: Boolean,
    shape: Shape,
    onBarcodes: (List<Barcode>) -> Unit,
    onStateChange: (CameraState) -> Unit,
    modifier: Modifier = Modifier,
)
