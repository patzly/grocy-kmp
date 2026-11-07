package com.patrickzedler.grocy.core.scanner

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotJoiningTest {

    private fun pixels(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun ByteArray.values() = map { it.toInt() and 0xFF }

    @Test
    fun evenAreaStaysTheSame() {
        val image = ByteArray(5 * 4) { 200.toByte() }

        val blurred = boxBlurred(image, width = 5, height = 4, radius = 2)

        assertTrue(blurred.values().all { it == 200 })
    }

    @Test
    fun gapBetweenTwoDotsIsFilled() {
        // Two dark dots with a light pixel between them, one row high
        val row = pixels(255, 0, 255, 0, 255)

        val blurred = boxBlurred(row, width = 5, height = 1, radius = 1).values()

        // The light gap turns darker than mid gray, one stroke with the dots
        assertEquals(85, blurred[2])
    }

    @Test
    fun edgesRepeatTheirOutermostPixels() {
        val row = pixels(0, 255, 255)

        val blurred = boxBlurred(row, width = 3, height = 1, radius = 1).values()

        // (0 + 0 + 255) / 3, rounded, at the left edge
        assertEquals(85, blurred[0])
        assertEquals(255, blurred[2])
    }

    @Test
    fun columnsAreBlurredToo() {
        val column = pixels(0, 255, 255)

        val blurred = boxBlurred(column, width = 1, height = 3, radius = 1).values()

        assertEquals(listOf(85, 170, 255), blurred)
    }

    @Test
    fun noRadiusCopiesThePixels() {
        val image = pixels(1, 2, 3, 4)

        assertContentEquals(image, boxBlurred(image, width = 2, height = 2, radius = 0))
    }

    @Test
    fun grayscaleNv21AddsNeutralColorForEachTwoByTwoBlock() {
        val luminance = pixels(10, 20, 30, 40, 50, 60)

        val nv21 = grayscaleNv21(luminance, width = 3, height = 2)

        // Six pixels, then one pair of color values for each of the two 2 × 2 blocks
        assertEquals(6 + 4, nv21.size)
        assertEquals(listOf(10, 20, 30, 40, 50, 60), nv21.values().take(6))
        assertTrue(nv21.values().drop(6).all { it == 128 })
    }
}
