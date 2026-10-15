package com.patrickzedler.grocy.core.scanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.BarcodeFormat as ZxingFormat

internal actual fun createZxingBarcodeDecoder(): BarcodeDecoder = ZxingBarcodeDecoder()

/** zxing-core, the open source engine of the Android apps. */
internal class ZxingBarcodeDecoder : BarcodeDecoder {

    private val reader = MultiFormatReader()

    override suspend fun decode(frame: CameraFrame, formats: Set<BarcodeFormat>): List<Barcode> {
        val image = frame.image
        return decode(image.toLuminanceImage().rotated(image.imageInfo.rotationDegrees), formats)
    }

    /** Decodes an upright image, like the user sees it in the preview. */
    fun decode(image: LuminanceImage, formats: Set<BarcodeFormat>): List<Barcode> {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to formats.map { it.toZxing() },
            DecodeHintType.ALSO_INVERTED to true,
        )
        decodeOnce(image, hints)?.let { return listOf(it) }
        // ZXing reads linear codes only along rows, ML Kit in every direction
        if (formats.any { !it.isTwoDimensional }) {
            decodeOnce(image.rotated(90), hints)?.let { return listOf(it) }
        }
        return emptyList()
    }

    private fun decodeOnce(image: LuminanceImage, hints: Map<DecodeHintType, Any>): Barcode? {
        val source = PlanarYUVLuminanceSource(
            image.bytes, image.width, image.height, 0, 0, image.width, image.height, false,
        )
        return try {
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
            result.barcodeFormat.toBarcodeFormat()?.let { Barcode(result.text, it) }
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }
}

internal fun BarcodeFormat.toZxing(): ZxingFormat = when (this) {
    BarcodeFormat.AZTEC -> ZxingFormat.AZTEC
    BarcodeFormat.CODABAR -> ZxingFormat.CODABAR
    BarcodeFormat.CODE_39 -> ZxingFormat.CODE_39
    BarcodeFormat.CODE_93 -> ZxingFormat.CODE_93
    BarcodeFormat.CODE_128 -> ZxingFormat.CODE_128
    BarcodeFormat.DATA_MATRIX -> ZxingFormat.DATA_MATRIX
    BarcodeFormat.EAN_8 -> ZxingFormat.EAN_8
    BarcodeFormat.EAN_13 -> ZxingFormat.EAN_13
    BarcodeFormat.ITF -> ZxingFormat.ITF
    BarcodeFormat.PDF_417 -> ZxingFormat.PDF_417
    BarcodeFormat.QR_CODE -> ZxingFormat.QR_CODE
    BarcodeFormat.UPC_A -> ZxingFormat.UPC_A
    BarcodeFormat.UPC_E -> ZxingFormat.UPC_E
}

internal fun ZxingFormat.toBarcodeFormat(): BarcodeFormat? =
    BarcodeFormat.entries.firstOrNull { it.toZxing() == this }
