package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardInsetsTest {

    @Test
    fun hiddenKeyboardCoversNothing() {
        assertEquals(0f, keyboardOverlap(containerBottom = 800.0, keyboardTop = null))
    }

    @Test
    fun keyboardCoversTheContainerBelowItsTop() {
        assertEquals(300f, keyboardOverlap(containerBottom = 800.0, keyboardTop = 500.0))
    }

    @Test
    fun keyboardBelowTheContainerCoversNothing() {
        // e.g. the container ends above the navigation bar, the keyboard area starts below it
        assertEquals(0f, keyboardOverlap(containerBottom = 760.0, keyboardTop = 780.0))
    }
}
