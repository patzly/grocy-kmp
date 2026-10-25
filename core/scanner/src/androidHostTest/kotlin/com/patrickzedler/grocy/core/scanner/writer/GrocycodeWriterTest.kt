package com.patrickzedler.grocy.core.scanner.writer

import com.patrickzedler.grocy.core.scanner.Barcode
import com.patrickzedler.grocy.core.scanner.BarcodeFormat
import com.patrickzedler.grocy.core.scanner.LuminanceImage
import com.patrickzedler.grocy.core.scanner.ZxingBarcodeDecoder
import kotlin.test.Test
import kotlin.test.assertEquals

/** The written grocycodes as ZXing reads them, like a scanner in Grocy's web frontend would. */
class GrocycodeWriterTest {

    private val decoder = ZxingBarcodeDecoder()

    /** Black modules on white, [scale] pixels each, rows [rowHeight] pixels for 1D codes. */
    private fun render(
        matrix: BarcodeMatrix,
        scale: Int = 4,
        rowHeight: Int = scale,
    ): LuminanceImage {
        val border = 40
        val width = matrix.width * scale + 2 * border
        val height = matrix.height * rowHeight + 2 * border
        val bytes = ByteArray(width * height) { index ->
            val x = (index % width - border).floorDiv(scale)
            val y = (index / width - border).floorDiv(rowHeight)
            val dark = x in 0 until matrix.width && y in 0 until matrix.height && matrix[x, y]
            if (dark) 0 else -1
        }
        return LuminanceImage(bytes, width, height)
    }

    private fun decodeDataMatrix(text: String) =
        decoder.decode(render(DataMatrixWriter.write(text)), setOf(BarcodeFormat.DATA_MATRIX))

    @Test
    fun productGrocycodeIsReadable() {
        val code = Grocycode.product(12)

        assertEquals(listOf(Barcode(code, BarcodeFormat.DATA_MATRIX)), decodeDataMatrix(code))
    }

    @Test
    fun stockEntryGrocycodeIsReadable() {
        val code = Grocycode.stockEntry(1234, "63f1a2b3c4d5e")

        assertEquals(listOf(Barcode(code, BarcodeFormat.DATA_MATRIX)), decodeDataMatrix(code))
    }

    @Test
    fun everySymbolSizeIsReadable() {
        // From the smallest symbol up to the largest with one block of 174 codewords
        listOf(1, 3, 6, 10, 17, 25, 33, 40, 50, 70, 100, 130, 160, 174).forEach { length ->
            val text = (0 until length).joinToString("") { ('a' + it % 26).toString() }

            assertEquals(
                listOf(Barcode(text, BarcodeFormat.DATA_MATRIX)),
                decodeDataMatrix(text),
                "length $length",
            )
        }
    }

    @Test
    fun digitPairsAndSymbolsAreReadable() {
        val text = "grcy:p:9876543210:x_!~"

        assertEquals(listOf(Barcode(text, BarcodeFormat.DATA_MATRIX)), decodeDataMatrix(text))
    }

    @Test
    fun smallestSymbolIsTenModulesWide() {
        assertEquals(10, DataMatrixWriter.write("ab").width)
    }

    @Test
    fun linearGrocycodeIsReadable() {
        val code = Grocycode.stockEntry(5, "63f1a2b3c4d5e")
        val image = render(Code128Writer.write(code), scale = 3, rowHeight = 120)

        assertEquals(
            listOf(Barcode(code, BarcodeFormat.CODE_128)),
            decoder.decode(image, setOf(BarcodeFormat.CODE_128)),
        )
    }

    @Test
    fun everyCode128PatternIsElevenModulesWide() {
        Code128Writer.PATTERNS.forEach { pattern ->
            assertEquals(11, pattern.sumOf { it - '0' }, pattern)
        }
    }

    @Test
    fun choosesTheCodeOfTheServersType() {
        val code = Grocycode.product(3)

        assertEquals(1, Grocycode.matrix(code, twoDimensional = false).height)
        // Eight codewords take the 14x14 symbol
        assertEquals(14, Grocycode.matrix(code, twoDimensional = true).height)
    }
}
