package com.patrickzedler.grocy.core.design.extensions

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorExtensionsTest {

    @Test
    fun cssHexHasSixUppercaseDigits() {
        assertEquals("#11140F", Color(0xFF11140F).toCssHex())
    }

    @Test
    fun cssHexKeepsLeadingZeros() {
        assertEquals("#000A0B", Color(0xFF000A0B).toCssHex())
    }

    @Test
    fun cssHexDropsAlpha() {
        assertEquals("#F8FAF0", Color(0x80F8FAF0).toCssHex())
    }
}
