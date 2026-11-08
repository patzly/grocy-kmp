package com.patrickzedler.grocy.core.wear

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class WearPaletteTest {

    private fun channel(argb: Long, shift: Int): Double {
        val value = ((argb shr shift) and 0xFF) / 255.0
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Long) =
        0.2126 * channel(argb, 16) + 0.7152 * channel(argb, 8) + 0.0722 * channel(argb, 0)

    private fun contrast(first: Long, second: Long): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    @Test
    fun theAccentIsGreen() = with(WearPalette) {
        for (color in listOf(primary, primaryDim, primaryContainer)) {
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            assertTrue(green > red && green > blue, color.toString(16))
        }
    }

    @Test
    fun textIsReadableOnItsBackground() = with(WearPalette) {
        val pairs = listOf(
            onPrimary to primary,
            onPrimaryContainer to primaryContainer,
            onSecondary to secondary,
            onSecondaryContainer to secondaryContainer,
            onTertiary to tertiary,
            onTertiaryContainer to tertiaryContainer,
            onSurface to surfaceContainerLow,
            onSurface to surfaceContainer,
            onSurface to surfaceContainerHigh,
            onSurfaceVariant to surfaceContainer,
            onBackground to background,
            onError to error,
            onErrorContainer to errorContainer,
        )

        for ((text, surface) in pairs) {
            assertTrue(
                contrast(text, surface) >= 4.5,
                "${text.toString(16)} on ${surface.toString(16)}",
            )
        }
    }

    @Test
    fun theBackgroundIsBlackForTheOledScreen() {
        assertTrue(WearPalette.background == 0xFF000000)
    }
}
