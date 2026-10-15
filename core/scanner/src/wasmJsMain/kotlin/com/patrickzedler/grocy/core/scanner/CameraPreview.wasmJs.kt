@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement

/** Pause between two decoded frames; decoding in Wasm is slower than on Android. */
private const val FRAME_INTERVAL_MILLIS = 120L

private external interface CameraStartResult : JsAny {
    val stream: JsAny?
    val error: String?
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun CameraPreview(
    decoderFactory: BarcodeDecoderFactory,
    formats: Set<BarcodeFormat>,
    isDecoding: Boolean,
    torchEnabled: Boolean,
    shape: Shape,
    onBarcodes: (List<Barcode>) -> Unit,
    onStateChange: (CameraState) -> Unit,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val currentFormats by rememberUpdatedState(formats)
    val currentIsDecoding by rememberUpdatedState(isDecoding)
    val currentOnBarcodes by rememberUpdatedState(onBarcodes)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val video = remember { createVideoElement() }
    val canvas = remember { createCanvasElement() }

    val decoder = remember(decoderFactory) { decoderFactory.create() }
    DisposableEffect(decoder) {
        onDispose { decoder.close() }
    }

    // The video lies above the Compose canvas, so it is rounded with CSS instead of a clip
    HtmlElementView(
        factory = { video },
        modifier = modifier.onSizeChanged { size ->
            val outline = shape.createOutline(size.toSize(), layoutDirection, density)
            setBorderRadius(video, outline.toCssBorderRadius(density.density))
        },
    )

    LaunchedEffect(decoder) {
        val result = startCamera(video).await<CameraStartResult>()
        val stream = result.stream
        if (stream == null) {
            currentOnStateChange(CameraState.Failed(cameraFailureOf(result.error)))
            return@LaunchedEffect
        }
        try {
            // Browsers offer no torch API except on some Android devices
            currentOnStateChange(CameraState.Running(hasTorch = false))
            while (true) {
                delay(FRAME_INTERVAL_MILLIS)
                if (!currentIsDecoding) continue
                val imageData = grabFrame(video, canvas) ?: continue
                currentOnBarcodes(decoder.decode(CameraFrame(imageData), currentFormats))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            currentOnStateChange(CameraState.Failed(CameraFailure.Error))
        } finally {
            stopCamera(video, stream)
        }
    }
}

private fun cameraFailureOf(errorName: String?): CameraFailure = when (errorName) {
    "NotAllowedError" -> CameraFailure.PermissionDenied
    "NotFoundError", "OverconstrainedError" -> CameraFailure.NoCamera
    "NotSupportedError", "SecurityError" -> CameraFailure.Unsupported
    else -> CameraFailure.Error
}

private fun createVideoElement(): HTMLVideoElement = js(
    """(() => {
        const video = document.createElement('video');
        video.muted = true;
        video.autoplay = true;
        video.playsInline = true;
        video.setAttribute('playsinline', '');
        video.setAttribute('aria-hidden', 'true');
        video.style.width = '100%';
        video.style.height = '100%';
        video.style.objectFit = 'cover';
        video.style.display = 'block';
        return video;
    })()""",
)

private fun createCanvasElement(): HTMLCanvasElement = js("document.createElement('canvas')")

private fun setBorderRadius(video: HTMLVideoElement, radius: String) {
    js("video.style.borderRadius = radius")
}

/** Resolves to { stream } or { error } with the name of the DOMException. */
private fun startCamera(video: HTMLVideoElement): Promise<CameraStartResult> = js(
    """(() => {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            return Promise.resolve({ stream: null, error: 'NotSupportedError' });
        }
        return navigator.mediaDevices.getUserMedia({
            audio: false,
            video: {
                facingMode: { ideal: 'environment' },
                width: { ideal: 1280 },
                height: { ideal: 720 }
            }
        }).then((stream) => {
            video.srcObject = stream;
            return video.play().then(() => ({ stream: stream, error: null }));
        }).catch((error) => ({ stream: null, error: (error && error.name) || 'Error' }));
    })()""",
)

/** The current video frame as ImageData, or null while the video has no frame yet. */
private fun grabFrame(video: HTMLVideoElement, canvas: HTMLCanvasElement): JsAny? = js(
    """(() => {
        const width = video.videoWidth;
        const height = video.videoHeight;
        if (video.readyState < 2 || width === 0 || height === 0) return null;
        if (canvas.width !== width) canvas.width = width;
        if (canvas.height !== height) canvas.height = height;
        const context = canvas.getContext('2d', { willReadFrequently: true });
        context.drawImage(video, 0, 0, width, height);
        return context.getImageData(0, 0, width, height);
    })()""",
)

private fun stopCamera(video: HTMLVideoElement, stream: JsAny) {
    js("stream.getTracks().forEach((track) => track.stop()); video.srcObject = null;")
}
