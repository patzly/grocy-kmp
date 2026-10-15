package com.patrickzedler.grocy.core.scanner

import androidx.compose.runtime.staticCompositionLocalOf

/** One camera image: an ImageProxy on Android, ImageData in browsers. */
expect class CameraFrame

/**
 * A barcode decoding library. Every engine implements this, so the scanner behaves the same
 * whichever engine decodes: ZXing ([BarcodeDecoderFactory.Zxing], all platforms) or ML Kit
 * (core:scanner-mlkit, Play app only).
 */
interface BarcodeDecoder : AutoCloseable {

    /**
     * Barcodes of [formats] found in [frame], empty when there is none. Runs off the main
     * thread and must not close [frame].
     */
    suspend fun decode(frame: CameraFrame, formats: Set<BarcodeFormat>): List<Barcode>

    override fun close() {}
}

/** Creates decoders of one engine; each running scanner owns one decoder. */
fun interface BarcodeDecoderFactory {

    fun create(): BarcodeDecoder

    companion object {
        /** ZXing: zxing-core on Android, zxing-wasm in browsers. */
        val Zxing = BarcodeDecoderFactory { createZxingBarcodeDecoder() }
    }
}

internal expect fun createZxingBarcodeDecoder(): BarcodeDecoder

/**
 * Decoder of the engine selected in the settings, provided once at the root of the app so every
 * [BarcodeScanner] uses it without further wiring.
 */
val LocalBarcodeDecoderFactory = staticCompositionLocalOf { BarcodeDecoderFactory.Zxing }
