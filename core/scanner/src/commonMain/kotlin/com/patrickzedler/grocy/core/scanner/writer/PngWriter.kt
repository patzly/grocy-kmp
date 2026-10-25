package com.patrickzedler.grocy.core.scanner.writer

/**
 * Writes a code as black on white PNG image like Grocy's grocycode downloads: every module
 * [moduleSize] pixels, a quiet zone of [quietZone] modules around it, the single row of a linear
 * code [linearHeight] modules high. Uncompressed deflate blocks keep it simple and small enough
 * for a few hundred modules.
 */
object PngWriter {

    fun write(
        matrix: BarcodeMatrix,
        moduleSize: Int,
        quietZone: Int,
        linearHeight: Int = 0,
    ): ByteArray {
        val rows = if (matrix.height == 1) linearHeight.coerceAtLeast(1) else matrix.height
        val width = (matrix.width + 2 * quietZone) * moduleSize
        val height = (rows + 2 * quietZone) * moduleSize
        // Each scanline starts with filter type 0, then one gray byte per pixel
        val raw = ByteArray(height * (width + 1))
        for (y in 0 until height) {
            val line = y * (width + 1)
            val moduleY = y / moduleSize - quietZone
            for (x in 0 until width) {
                val moduleX = x / moduleSize - quietZone
                val dark = moduleX in 0 until matrix.width && moduleY in 0 until rows &&
                    matrix[moduleX, if (matrix.height == 1) 0 else moduleY]
                raw[line + 1 + x] = if (dark) 0 else WHITE
            }
        }
        return buildPng(width, height, raw)
    }

    private fun buildPng(width: Int, height: Int, raw: ByteArray): ByteArray {
        val header = ByteBuilder().apply {
            int(width)
            int(height)
            // 8 bit grayscale, deflate, adaptive filtering, no interlace
            bytes(byteArrayOf(8, 0, 0, 0, 0))
        }.toByteArray()
        return ByteBuilder().apply {
            bytes(SIGNATURE)
            chunk("IHDR", header)
            chunk("IDAT", zlibStored(raw))
            chunk("IEND", ByteArray(0))
        }.toByteArray()
    }

    /** Zlib data of [data] in stored (uncompressed) deflate blocks. */
    internal fun zlibStored(data: ByteArray): ByteArray = ByteBuilder().apply {
        bytes(byteArrayOf(0x78, 0x01))
        var offset = 0
        do {
            val length = minOf(MAX_STORED_BLOCK, data.size - offset)
            val isLast = offset + length >= data.size
            byte(if (isLast) 1 else 0)
            byte(length and 0xFF)
            byte(length shr 8)
            byte(length.inv() and 0xFF)
            byte((length.inv() shr 8) and 0xFF)
            bytes(data.copyOfRange(offset, offset + length))
            offset += length
        } while (offset < data.size)
        int(adler32(data))
    }.toByteArray()

    internal fun adler32(data: ByteArray): Int {
        var a = 1
        var b = 0
        data.forEach {
            a = (a + (it.toInt() and 0xFF)) % ADLER_MODULUS
            b = (b + a) % ADLER_MODULUS
        }
        return (b shl 16) or a
    }

    internal fun crc32(data: ByteArray): Int {
        var crc = -1
        data.forEach { crc = CRC_TABLE[(crc xor it.toInt()) and 0xFF] xor (crc ushr 8) }
        return crc.inv()
    }

    private fun ByteBuilder.chunk(type: String, data: ByteArray) {
        val typeBytes = type.encodeToByteArray()
        int(data.size)
        bytes(typeBytes)
        bytes(data)
        int(crc32(typeBytes + data))
    }

    private class ByteBuilder {
        private val buffer = ArrayList<Byte>()

        fun byte(value: Int) {
            buffer += value.toByte()
        }

        fun bytes(values: ByteArray) {
            values.forEach { buffer += it }
        }

        fun int(value: Int) {
            byte(value ushr 24)
            byte(value ushr 16)
            byte(value ushr 8)
            byte(value)
        }

        fun toByteArray(): ByteArray = buffer.toByteArray()
    }

    private const val WHITE: Byte = -1
    private const val MAX_STORED_BLOCK = 65_535
    private const val ADLER_MODULUS = 65_521

    private val SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)

    private val CRC_TABLE = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) 0xEDB88320.toInt() xor (c ushr 1) else c ushr 1 }
        c
    }
}
