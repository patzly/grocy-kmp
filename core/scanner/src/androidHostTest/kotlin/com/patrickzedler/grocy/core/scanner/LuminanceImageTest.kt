package com.patrickzedler.grocy.core.scanner

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LuminanceImageTest {

    // 3 x 2:
    // 1 2 3
    // 4 5 6
    private val image = LuminanceImage(byteArrayOf(1, 2, 3, 4, 5, 6), width = 3, height = 2)

    @Test
    fun rotatesClockwise() {
        val rotated = image.rotated(90)

        // 4 1
        // 5 2
        // 6 3
        assertEquals(2, rotated.width)
        assertEquals(3, rotated.height)
        assertContentEquals(byteArrayOf(4, 1, 5, 2, 6, 3), rotated.bytes)
    }

    @Test
    fun rotatesHalfTurn() {
        assertContentEquals(byteArrayOf(6, 5, 4, 3, 2, 1), image.rotated(180).bytes)
    }

    @Test
    fun rotatesCounterclockwise() {
        val rotated = image.rotated(270)

        // 3 6
        // 2 5
        // 1 4
        assertContentEquals(byteArrayOf(3, 6, 2, 5, 1, 4), rotated.bytes)
    }

    @Test
    fun fullTurnIsUnchanged() {
        assertContentEquals(image.bytes, image.rotated(90).rotated(270).bytes)
        assertContentEquals(image.bytes, image.rotated(360).bytes)
    }
}
