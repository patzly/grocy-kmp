package com.patrickzedler.grocy.core.scanner

import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/** Enough detail for small barcodes and printed dates, small enough for several frames a second. */
private val ANALYSIS_SIZE = Size(1280, 720)

@Composable
internal actual fun <T> CameraPreview(
    analyzer: FrameAnalyzer<T>,
    isAnalyzing: Boolean,
    torchEnabled: Boolean,
    shape: Shape,
    onResult: (T) -> Unit,
    onStateChange: (CameraState) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentIsAnalyzing by rememberUpdatedState(isAnalyzing)
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(lifecycleOwner, analyzer) {
        val provider = try {
            ProcessCameraProvider.awaitInstance(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            currentOnStateChange(CameraState.Failed(CameraFailure.Error))
            return@LaunchedEffect
        }
        // Chromebooks and tablets may only have a front or an external camera
        val selector = listOf(
            CameraSelector.DEFAULT_BACK_CAMERA,
            CameraSelector.DEFAULT_FRONT_CAMERA,
        ).firstOrNull { provider.hasCamera(it) }
            ?: provider.availableCameraInfos.firstOrNull()?.cameraSelector
        if (selector == null) {
            currentOnStateChange(CameraState.Failed(CameraFailure.NoCamera))
            return@LaunchedEffect
        }

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { surfaceRequest = it }
        }
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ANALYSIS_SIZE,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            // The camera waits for the current frame to close, so frames never pile up
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val frames = Channel<ImageProxy>(capacity = 1, onUndeliveredElement = { it.close() })
        val executor = Executors.newSingleThreadExecutor()
        analysis.setAnalyzer(executor) { image ->
            if (frames.trySend(image).isFailure) image.close()
        }

        try {
            val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            camera = boundCamera
            currentOnStateChange(
                CameraState.Running(hasTorch = boundCamera.cameraInfo.hasFlashUnit()),
            )
            withContext(Dispatchers.Default) {
                for (image in frames) {
                    try {
                        if (currentIsAnalyzing) {
                            val result = analyzer.analyze(CameraFrame(image))
                            withContext(Dispatchers.Main) { currentOnResult(result) }
                        }
                    } finally {
                        image.close()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            currentOnStateChange(CameraState.Failed(CameraFailure.Error))
        } finally {
            analysis.clearAnalyzer()
            provider.unbind(preview, analysis)
            frames.close()
            executor.shutdown()
            camera = null
            surfaceRequest = null
        }
    }

    LaunchedEffect(camera, torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    surfaceRequest?.let {
        CameraXViewfinder(
            surfaceRequest = it,
            // A TextureView, which follows the rounded clip of the scanner unlike a SurfaceView
            implementationMode = ImplementationMode.EMBEDDED,
            modifier = modifier,
        )
    }
}
