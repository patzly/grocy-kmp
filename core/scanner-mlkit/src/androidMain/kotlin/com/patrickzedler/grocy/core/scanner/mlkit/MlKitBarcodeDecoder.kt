package com.patrickzedler.grocy.core.scanner.mlkit

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.patrickzedler.grocy.core.scanner.Barcode
import com.patrickzedler.grocy.core.scanner.BarcodeDecoder
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory
import com.patrickzedler.grocy.core.scanner.BarcodeFormat
import com.patrickzedler.grocy.core.scanner.CameraFrame
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.mlkit.vision.barcode.common.Barcode as MlKitBarcode

/** ML Kit with its bundled model, so it needs no download from Play services. */
val MlKitBarcodeDecoderFactory = BarcodeDecoderFactory { MlKitBarcodeDecoder() }

internal class MlKitBarcodeDecoder : BarcodeDecoder {

    private var scanner: BarcodeScanner? = null
    private var scannerFormats: Set<BarcodeFormat> = emptySet()

    @OptIn(ExperimentalGetImage::class)
    override suspend fun decode(frame: CameraFrame, formats: Set<BarcodeFormat>): List<Barcode> {
        val image = frame.image
        val mediaImage = image.image ?: return emptyList()
        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        return scannerFor(formats).process(input).await().mapNotNull { it.toBarcode() }
    }

    private fun scannerFor(formats: Set<BarcodeFormat>): BarcodeScanner {
        scanner?.takeIf { formats == scannerFormats }?.let { return it }
        scanner?.close()
        val mlKitFormats = formats.map { it.toMlKit() }
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(mlKitFormats.first(), *mlKitFormats.drop(1).toIntArray())
            .build()
        return BarcodeScanning.getClient(options).also {
            scanner = it
            scannerFormats = formats
        }
    }

    override fun close() {
        scanner?.close()
        scanner = null
    }
}

private fun MlKitBarcode.toBarcode(): Barcode? {
    val value = rawValue ?: return null
    val format = barcodeFormatOf(format) ?: return null
    return Barcode(value, format)
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

internal fun BarcodeFormat.toMlKit(): Int = when (this) {
    BarcodeFormat.AZTEC -> MlKitBarcode.FORMAT_AZTEC
    BarcodeFormat.CODABAR -> MlKitBarcode.FORMAT_CODABAR
    BarcodeFormat.CODE_39 -> MlKitBarcode.FORMAT_CODE_39
    BarcodeFormat.CODE_93 -> MlKitBarcode.FORMAT_CODE_93
    BarcodeFormat.CODE_128 -> MlKitBarcode.FORMAT_CODE_128
    BarcodeFormat.DATA_MATRIX -> MlKitBarcode.FORMAT_DATA_MATRIX
    BarcodeFormat.EAN_8 -> MlKitBarcode.FORMAT_EAN_8
    BarcodeFormat.EAN_13 -> MlKitBarcode.FORMAT_EAN_13
    BarcodeFormat.ITF -> MlKitBarcode.FORMAT_ITF
    BarcodeFormat.PDF_417 -> MlKitBarcode.FORMAT_PDF417
    BarcodeFormat.QR_CODE -> MlKitBarcode.FORMAT_QR_CODE
    BarcodeFormat.UPC_A -> MlKitBarcode.FORMAT_UPC_A
    BarcodeFormat.UPC_E -> MlKitBarcode.FORMAT_UPC_E
}

internal fun barcodeFormatOf(mlKitFormat: Int): BarcodeFormat? =
    BarcodeFormat.entries.firstOrNull { it.toMlKit() == mlKitFormat }
