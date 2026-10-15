package com.patrickzedler.grocy.core.scanner

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Outline
import kotlin.test.Test
import kotlin.test.assertEquals

class CssBorderRadiusTest {

    @Test
    fun cornersAreGivenInCssPixelsClockwiseFromTopLeft() {
        val rect = RoundRect(
            rect = Rect(0f, 0f, 640f, 400f),
            topLeft = CornerRadius(56f),
            topRight = CornerRadius(54f),
            bottomRight = CornerRadius(8f),
            bottomLeft = CornerRadius(6f),
        )

        assertEquals("28px 27px 4px 3px", rect.toCssBorderRadius(density = 2f))
    }

    @Test
    fun fractionalRadiiAreRounded() {
        val rect = RoundRect(Rect(0f, 0f, 100f, 100f), CornerRadius(20.6667f))

        assertEquals("10.33px 10.33px 10.33px 10.33px", rect.toCssBorderRadius(density = 2f))
    }

    @Test
    fun outlineWithoutRoundedCornersHasNoRadius() {
        assertEquals("0", Outline.Rectangle(Rect(0f, 0f, 1f, 1f)).toCssBorderRadius(2f))
    }
}
