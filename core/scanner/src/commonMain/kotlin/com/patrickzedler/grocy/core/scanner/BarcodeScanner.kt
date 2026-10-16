package com.patrickzedler.grocy.core.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_error
import com.patrickzedler.grocy.core.resources.ic_rounded_flashlight_off
import com.patrickzedler.grocy.core.resources.ic_rounded_flashlight_on
import com.patrickzedler.grocy.core.resources.ic_rounded_no_photography
import com.patrickzedler.grocy.core.resources.ic_rounded_photo_camera
import com.patrickzedler.grocy.core.resources.scanner_error
import com.patrickzedler.grocy.core.resources.scanner_no_camera
import com.patrickzedler.grocy.core.resources.scanner_open_settings
import com.patrickzedler.grocy.core.resources.scanner_permission_allow
import com.patrickzedler.grocy.core.resources.scanner_permission_denied
import com.patrickzedler.grocy.core.resources.scanner_permission_denied_browser
import com.patrickzedler.grocy.core.resources.scanner_permission_message
import com.patrickzedler.grocy.core.resources.scanner_retry
import com.patrickzedler.grocy.core.resources.scanner_torch_off
import com.patrickzedler.grocy.core.resources.scanner_torch_on
import com.patrickzedler.grocy.core.resources.scanner_unsupported
import com.patrickzedler.grocy.core.resources.scanner_viewfinder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Landscape, so the scanner leaves room for the rest of the screen. */
private const val SCANNER_ASPECT_RATIO = 16f / 10f

/** Wide windows with little height (phones in landscape) keep most of it for the screen. */
private const val MAX_WINDOW_HEIGHT_FRACTION = 0.45f

/** Below this a code no longer fits comfortably into the preview. */
private val MIN_SCANNER_HEIGHT = 140.dp

/**
 * Height of a scanner [width] wide in a window [windowHeight] high: 16:10 where the window has
 * room for it, otherwise a wider strip, so the scanner never fills the screen. Linear codes are
 * wide anyway and the preview is cropped, never distorted.
 */
internal fun scannerHeight(width: Dp, windowHeight: Dp): Dp {
    val maxHeight = (windowHeight * MAX_WINDOW_HEIGHT_FRACTION).coerceAtLeast(MIN_SCANNER_HEIGHT)
    return minOf(width / SCANNER_ASPECT_RATIO, maxHeight)
}

/**
 * Camera barcode scanner, the one scanner of the whole app.
 *
 * Decodes with the engine of [LocalBarcodeDecoderFactory], so ZXing and ML Kit behave the
 * same. The camera starts only when the permission is granted; otherwise the scanner asks
 * for it when the user taps its button, never by itself. Place it as a segmented item: pass the
 * item's [shape], the scanner draws the segmented item background itself. It fills the item's
 * width; its height follows [scannerHeight], so it looks right in every window size.
 *
 * @param formats formats to look for; fewer formats scan faster and avoid wrong matches.
 * @param paused stops decoding while the camera keeps running, e.g. while a scan is processed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BarcodeScanner(
    onBarcodeScanned: (Barcode) -> Unit,
    modifier: Modifier = Modifier,
    formats: Set<BarcodeFormat> = BarcodeFormat.Products,
    shape: Shape = MaterialTheme.shapes.large,
    paused: Boolean = false,
) {
    val decoderFactory = LocalBarcodeDecoderFactory.current
    val access = rememberCameraAccess()
    val haptic = LocalHapticFeedback.current
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)
    val filter = remember { ScanResultFilter() }
    var cameraState by remember { mutableStateOf<CameraState>(CameraState.Starting) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }
    // Changing it restarts the camera after a failure
    var attempt by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val height = scannerHeight(width.toDp(), windowHeight).roundToPx()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .clip(shape)
            .background(SegmentedListDefaults.containerColor),
        contentAlignment = Alignment.Center,
    ) {
        when (access.permission) {
            CameraPermission.Unknown -> LoadingIndicator()

            CameraPermission.Requestable -> ScannerMessage(
                icon = Res.drawable.ic_rounded_photo_camera,
                text = stringResource(Res.string.scanner_permission_message),
                actionLabel = stringResource(Res.string.scanner_permission_allow),
                onAction = access::request,
            )

            CameraPermission.Denied -> PermissionDeniedMessage(access)

            CameraPermission.Granted -> {
                val failure = (cameraState as? CameraState.Failed)?.reason
                if (failure == CameraFailure.PermissionDenied) {
                    PermissionDeniedMessage(access)
                } else if (failure != null) {
                    ScannerMessage(
                        icon = Res.drawable.ic_rounded_error,
                        text = stringResource(failure.message()),
                        actionLabel = stringResource(Res.string.scanner_retry)
                            .takeIf { failure == CameraFailure.Error },
                        onAction = {
                            cameraState = CameraState.Starting
                            attempt++
                        },
                    )
                } else {
                    val description = stringResource(Res.string.scanner_viewfinder)
                    key(attempt) {
                        CameraPreview(
                            decoderFactory = decoderFactory,
                            formats = formats,
                            isDecoding = !paused,
                            torchEnabled = torchEnabled,
                            shape = shape,
                            onBarcodes = { barcodes ->
                                filter.accept(barcodes)?.let {
                                    haptic.confirm()
                                    currentOnBarcodeScanned(it)
                                }
                            },
                            onStateChange = { cameraState = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics { contentDescription = description },
                        )
                    }
                    when (val state = cameraState) {
                        CameraState.Starting -> LoadingIndicator()
                        is CameraState.Running -> if (state.hasTorch) {
                            TorchButton(
                                enabled = torchEnabled,
                                onEnabledChange = { torchEnabled = it },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                            )
                        }
                        is CameraState.Failed -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedMessage(access: CameraAccess) {
    ScannerMessage(
        icon = Res.drawable.ic_rounded_no_photography,
        text = stringResource(
            if (access.canOpenSettings) {
                Res.string.scanner_permission_denied
            } else {
                Res.string.scanner_permission_denied_browser
            },
        ),
        actionLabel = stringResource(Res.string.scanner_open_settings)
            .takeIf { access.canOpenSettings },
        onAction = access::openSettings,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScannerMessage(
    icon: DrawableResource,
    text: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (actionLabel != null) {
            Button(
                onClick = {
                    haptic.confirm()
                    onAction()
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TorchButton(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val description = stringResource(
        if (enabled) Res.string.scanner_torch_off else Res.string.scanner_torch_on,
    )

    HapticTooltipBox(text = description, modifier = modifier) {
        FilledIconToggleButton(
            checked = enabled,
            onCheckedChange = {
                haptic.confirm()
                onEnabledChange(it)
            },
            shapes = IconButtonDefaults.toggleableShapes(),
        ) {
            Icon(
                painter = painterResource(
                    if (enabled) {
                        Res.drawable.ic_rounded_flashlight_off
                    } else {
                        Res.drawable.ic_rounded_flashlight_on
                    },
                ),
                contentDescription = description,
            )
        }
    }
}

private fun CameraFailure.message() = when (this) {
    CameraFailure.NoCamera -> Res.string.scanner_no_camera
    CameraFailure.Unsupported -> Res.string.scanner_unsupported
    CameraFailure.PermissionDenied, CameraFailure.Error -> Res.string.scanner_error
}
