package com.patrickzedler.grocy.core.scanner

import androidx.camera.core.ImageProxy

/** Grayscale pixels, one byte each, row by row without padding. */
internal class LuminanceImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    /** The image turned clockwise by [degrees] (0, 90, 180 or 270). */
    fun rotated(degrees: Int): LuminanceImage = when (degrees.mod(360)) {
        0 -> this
        90 -> {
            // Source (x, y) moves to (height - 1 - y, x), the new width is the old height
            val out = ByteArray(bytes.size)
            for (y in 0 until height) {
                val sourceRow = y * width
                val targetColumn = height - 1 - y
                for (x in 0 until width) {
                    out[x * height + targetColumn] = bytes[sourceRow + x]
                }
            }
            LuminanceImage(out, width = height, height = width)
        }
        180 -> LuminanceImage(bytes.reversedArray(), width, height)
        270 -> {
            // Source (x, y) moves to (y, width - 1 - x)
            val out = ByteArray(bytes.size)
            for (y in 0 until height) {
                val sourceRow = y * width
                for (x in 0 until width) {
                    out[(width - 1 - x) * height + y] = bytes[sourceRow + x]
                }
            }
            LuminanceImage(out, width = height, height = width)
        }
        else -> throw IllegalArgumentException("Unsupported rotation $degrees")
    }
}

/** The Y plane of a YUV image, which is all ZXing needs. */
internal fun ImageProxy.toLuminanceImage(): LuminanceImage {
    val plane = planes[0]
    val buffer = plane.buffer.duplicate().apply { rewind() }
    val rowStride = plane.rowStride
    val bytes = ByteArray(width * height)
    if (rowStride == width) {
        buffer.get(bytes, 0, bytes.size)
    } else {
        // Rows are padded to the stride, copy them one by one
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(bytes, row * width, width)
        }
    }
    return LuminanceImage(bytes, width, height)
}
