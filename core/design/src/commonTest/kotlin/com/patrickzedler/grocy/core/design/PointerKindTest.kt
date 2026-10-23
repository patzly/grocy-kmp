package com.patrickzedler.grocy.core.design

import androidx.compose.ui.input.pointer.PointerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PointerKindTest {

    @Test
    fun mouseCountsAsMouse() {
        assertEquals(true, usesMouse(PointerType.Mouse))
    }

    @Test
    fun touchAndStylusCountAsTouch() {
        assertEquals(false, usesMouse(PointerType.Touch))
        assertEquals(false, usesMouse(PointerType.Stylus))
        assertEquals(false, usesMouse(PointerType.Eraser))
    }

    @Test
    fun unknownPointerKeepsTheCurrentKind() {
        assertNull(usesMouse(PointerType.Unknown))
    }
}
