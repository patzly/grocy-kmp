package com.patrickzedler.grocy.core.scanner.mlkit

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.patrickzedler.grocy.core.scanner.CameraFrame
import com.patrickzedler.grocy.core.scanner.TextRecognizer
import com.patrickzedler.grocy.core.scanner.TextRecognizerFactory
import com.patrickzedler.grocy.core.scanner.dotsJoined

/** ML Kit's Latin text recognition with its bundled model, so it needs no download. */
val MlKitTextRecognizerFactory = TextRecognizerFactory { MlKitTextRecognizer() }

/**
 * Reads every other frame with the dots of dot matrix prints joined, which ML Kit reads badly
 * as they are; the frames between stay as they are for sharp print. Voting over frames combines
 * both.
 */
internal class MlKitTextRecognizer : TextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var frames = 0L

    @OptIn(ExperimentalGetImage::class)
    override suspend fun recognize(frame: CameraFrame): List<String> {
        val image = frame.image
        val rotation = image.imageInfo.rotationDegrees
        val input = if (frames++ % 2 == 0L) {
            val mediaImage = image.image ?: return emptyList()
            InputImage.fromMediaImage(mediaImage, rotation)
        } else {
            val joined = frame.dotsJoined()
            InputImage.fromByteArray(
                joined.bytes,
                joined.width,
                joined.height,
                rotation,
                InputImage.IMAGE_FORMAT_NV21,
            )
        }
        val text = recognizer.process(input).await()
        return readingOrder(
            text.textBlocks.flatMap { block ->
                block.lines.map { line ->
                    RecognizedLine(
                        text = line.text,
                        top = line.boundingBox?.top ?: 0,
                        left = line.boundingBox?.left ?: 0,
                    )
                }
            },
        )
    }

    override fun close() {
        recognizer.close()
    }
}

/** A line ML Kit found, with the top left corner of its box in the upright image. */
internal data class RecognizedLine(val text: String, val top: Int, val left: Int)

/** The lines' texts from top to bottom and left to right, how a package is read. */
internal fun readingOrder(lines: List<RecognizedLine>): List<String> =
    lines.sortedWith(compareBy({ it.top }, { it.left })).map { it.text }
