package com.patrickzedler.grocy.core.scanner.writer

import java.io.ByteArrayInputStream
import java.util.zip.Adler32
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PngWriterTest {

    @Test
    fun checksumsMatchThoseOfJava() {
        val data = "grcy:p:12".encodeToByteArray()

        assertEquals(CRC32().apply { update(data) }.value.toInt(), PngWriter.crc32(data))
        assertEquals(Adler32().apply { update(data) }.value.toInt(), PngWriter.adler32(data))
    }

    @Test
    fun storedZlibDataInflatesToTheOriginalAlsoOverSeveralBlocks() {
        val data = ByteArray(150_000) { (it % 251).toByte() }

        val inflated = InflaterInputStream(ByteArrayInputStream(PngWriter.zlibStored(data)))
            .readBytes()

        assertContentEquals(data, inflated)
    }

    @Test
    fun imageHasTheSizeOfModulesAndQuietZone() {
        val png = PngWriter.write(DataMatrixWriter.write("ab"), moduleSize = 4, quietZone = 1)

        // Signature, then IHDR with width and height
        assertEquals(0x89, png[0].toInt() and 0xFF)
        assertEquals("IHDR", png.copyOfRange(12, 16).decodeToString())
        assertEquals(48, int(png, 16))
        assertEquals(48, int(png, 20))
    }

    @Test
    fun linearCodesGetTheirHeight() {
        val matrix = Code128Writer.write("grcy:p:1")

        val png = PngWriter.write(matrix, moduleSize = 2, quietZone = 10, linearHeight = 30)

        assertEquals((matrix.width + 20) * 2, int(png, 16))
        assertEquals((30 + 20) * 2, int(png, 20))
    }

    @Test
    fun imageDecodesWithItsModules() {
        val matrix = DataMatrixWriter.write("ab")

        val image = ImageIO.read(
            ByteArrayInputStream(PngWriter.write(matrix, moduleSize = 2, quietZone = 1)),
        )

        // The quiet zone is white, the lower left finder corner black
        assertEquals(0xFFFFFF, image.getRGB(0, 0) and 0xFFFFFF)
        assertEquals(0x000000, image.getRGB(2, image.height - 3) and 0xFFFFFF)
    }

    private fun int(bytes: ByteArray, offset: Int) = (0 until 4).fold(0) { value, i ->
        (value shl 8) or (bytes[offset + i].toInt() and 0xFF)
    }
}
