package com.patrickzedler.grocy.core.scanner

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults

/**
 * A text recognition library, e.g. ML Kit (core:scanner-mlkit, Play app only), which reads
 * printed text like due dates from camera frames.
 */
interface TextRecognizer : AutoCloseable {

    /**
     * The lines of text in [frame], from top to bottom, empty when there is none. Runs off the
     * main thread and must not close [frame].
     */
    suspend fun recognize(frame: CameraFrame): List<String>

    override fun close() {}
}

/** Creates recognizers of one engine; each running scanner owns one recognizer. */
fun interface TextRecognizerFactory {

    fun create(): TextRecognizer
}

/**
 * The engine that reads printed text, provided once at the root of the app; null where the
 * platform has none or the user turned scanning dates off, which hides the date scanners.
 */
val LocalTextRecognizerFactory = staticCompositionLocalOf<TextRecognizerFactory?> { null }

/**
 * Camera that reads text, like [BarcodeScanner] for codes: the same frame, permission handling
 * and torch, but [onLines] gets the lines of every frame, also an empty list, so the caller can
 * vote over frames. Needs [LocalTextRecognizerFactory]; without one it shows nothing.
 *
 * @param paused stops reading while the camera keeps running, e.g. while a choice is open.
 */
@Composable
fun TextScanner(
    onLines: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    paused: Boolean = false,
    containerColor: Color = SegmentedListDefaults.containerColor,
) {
    val recognizerFactory = LocalTextRecognizerFactory.current ?: return
    val currentOnLines by rememberUpdatedState(onLines)

    CameraScannerBox(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
    ) { torchEnabled, onStateChange, previewModifier ->
        val recognizer = remember(recognizerFactory) { recognizerFactory.create() }
        DisposableEffect(recognizer) {
            onDispose { recognizer.close() }
        }
        val analyzer = remember(recognizer) { FrameAnalyzer(recognizer::recognize) }
        CameraPreview(
            analyzer = analyzer,
            isAnalyzing = !paused,
            torchEnabled = torchEnabled,
            shape = shape,
            onResult = { currentOnLines(it) },
            onStateChange = onStateChange,
            modifier = previewModifier,
        )
    }
}
