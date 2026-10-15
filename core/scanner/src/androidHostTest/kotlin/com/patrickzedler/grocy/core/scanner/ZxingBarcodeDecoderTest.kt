package com.patrickzedler.grocy.core.scanner

import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZxingBarcodeDecoderTest {

    private val decoder = ZxingBarcodeDecoder()

    /**
     * Renders a code like a camera would see it: black on white, every module [scale] pixels
     * wide and a white border of [border] pixels around it.
     */
    private fun render(
        value: String,
        format: BarcodeFormat,
        height: Int = 0,
        scale: Int = 4,
        border: Int = 40,
    ): LuminanceImage {
        // Size 0 gives one pixel per module, the border is added here
        val matrix = MultiFormatWriter().encode(
            value,
            format.toZxing(),
            0,
            height,
            mapOf(EncodeHintType.MARGIN to 0),
        )
        val width = matrix.width * scale + 2 * border
        val imageHeight = (if (height > 0) matrix.height else matrix.height * scale) + 2 * border
        val rowScale = if (height > 0) 1 else scale
        val bytes = ByteArray(width * imageHeight) { index ->
            val x = (index % width - border).floorDiv(scale)
            val y = (index / width - border).floorDiv(rowScale)
            val isBlack = x in 0 until matrix.width && y in 0 until matrix.height && matrix[x, y]
            if (isBlack) 0 else -1
        }
        return LuminanceImage(bytes, width, imageHeight)
    }

    @Test
    fun decodesQrCode() {
        val image = render("https://grocy.example.com/api|key", BarcodeFormat.QR_CODE)

        assertEquals(
            listOf(Barcode("https://grocy.example.com/api|key", BarcodeFormat.QR_CODE)),
            decoder.decode(image, BarcodeFormat.Products),
        )
    }

    @Test
    fun decodesEan13() {
        val image = render("4006381333931", BarcodeFormat.EAN_13, height = 120)

        assertEquals(
            listOf(Barcode("4006381333931", BarcodeFormat.EAN_13)),
            decoder.decode(image, BarcodeFormat.Products),
        )
    }

    @Test
    fun decodesLinearCodeHeldUpright() {
        val image = render("4006381333931", BarcodeFormat.EAN_13, height = 120).rotated(90)

        assertEquals(
            listOf(Barcode("4006381333931", BarcodeFormat.EAN_13)),
            decoder.decode(image, BarcodeFormat.Products),
        )
    }

    @Test
    fun ignoresFormatsNotAskedFor() {
        val image = render("4006381333931", BarcodeFormat.EAN_13, height = 120)

        assertTrue(decoder.decode(image, BarcodeFormat.QrCode).isEmpty())
    }

    @Test
    fun emptyImageHasNoBarcode() {
        val image = LuminanceImage(ByteArray(320 * 240) { -1 }, 320, 240)

        assertTrue(decoder.decode(image, BarcodeFormat.Products).isEmpty())
    }

    @Test
    fun everyFormatMapsToZxingAndBack() {
        BarcodeFormat.entries.forEach { assertEquals(it, it.toZxing().toBarcodeFormat()) }
    }
}
