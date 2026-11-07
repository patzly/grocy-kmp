package com.patrickzedler.grocy.core.scanner

/**
 * Grayscale [pixels] ([width] × [height], one byte each, row by row) blurred with a box of
 * 2 × [radius] + 1 pixels. Dot matrix prints, like the due dates of cans, become strokes that text
 * recognition reads like other print; the edges repeat their outermost pixels.
 */
internal fun boxBlurred(pixels: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
    require(pixels.size == width * height) { "Expected ${width * height} pixels" }
    if (radius <= 0 || pixels.isEmpty()) return pixels.copyOf()
    // Two passes along rows and columns cost 2 × width × height instead of the box's area each
    val rows = IntArray(pixels.size)
    for (y in 0 until height) {
        val row = y * width
        blurLine(width, radius, read = { x -> pixels[row + x].toInt() and 0xFF }) { x, sum ->
            rows[row + x] = sum
        }
    }
    val out = ByteArray(pixels.size)
    val area = (2 * radius + 1) * (2 * radius + 1)
    for (x in 0 until width) {
        blurLine(height, radius, read = { y -> rows[y * width + x] }) { y, sum ->
            out[y * width + x] = ((sum + area / 2) / area).toByte()
        }
    }
    return out
}

/** Sliding sums of 2 × [radius] + 1 values of a line, its ends repeated beyond it. */
private inline fun blurLine(
    count: Int,
    radius: Int,
    read: (Int) -> Int,
    write: (Int, Int) -> Unit,
) {
    var sum = 0
    for (offset in -radius..radius) sum += read(offset.coerceIn(0, count - 1))
    for (index in 0 until count) {
        write(index, sum)
        sum += read((index + radius + 1).coerceAtMost(count - 1)) -
            read((index - radius).coerceAtLeast(0))
    }
}

/**
 * Grayscale [luminance] ([width] × [height]) as an NV21 image, the format text recognition
 * libraries take from cameras: the pixels followed by neutral color.
 */
internal fun grayscaleNv21(luminance: ByteArray, width: Int, height: Int): ByteArray {
    val chroma = 2 * ((width + 1) / 2) * ((height + 1) / 2)
    return luminance.copyOf(luminance.size + chroma).also {
        it.fill(NEUTRAL_CHROMA, fromIndex = luminance.size)
    }
}

private const val NEUTRAL_CHROMA: Byte = -128
