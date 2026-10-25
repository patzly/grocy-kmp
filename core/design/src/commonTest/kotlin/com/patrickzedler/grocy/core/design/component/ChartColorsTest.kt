package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChartColorsTest {

    // Material's primary tones: 40 in light schemes, 80 in dark ones
    private fun primary(hue: Int, isDark: Boolean = false) =
        Color(Hct.from(hue.toDouble(), 48.0, if (isDark) 80.0 else 40.0).toInt())

    private fun hctOf(color: Color) = Hct.fromInt(color.toArgb())

    private fun hueDistance(a: Double, b: Double) = abs((b - a + 540) % 360 - 180)

    private fun palette(isDark: Boolean) = if (isDark) DarkChartColors else LightChartColors

    @Test
    fun bothModesHaveEveryColorOnce() {
        for (colors in listOf(LightChartColors, DarkChartColors)) {
            assertEquals(CHART_COLOR_COUNT, colors.size)
            assertEquals(CHART_COLOR_COUNT, colors.distinct().size)
        }
    }

    @Test
    fun palettesAreValidThemselves() {
        assertTrue(isValidPalette(LightChartColors, isDark = false))
        assertTrue(isValidPalette(DarkChartColors, isDark = true))
    }

    @Test
    fun harmonizedColorsStayValidForEveryPrimaryHue() {
        for (hue in 0 until 360) {
            for (isDark in listOf(false, true)) {
                val colors = harmonizedChartColors(palette(isDark), primary(hue, isDark), isDark)

                assertTrue(isValidPalette(colors, isDark), "hue $hue, dark $isDark")
                assertEquals(CHART_COLOR_COUNT, colors.distinct().size)
            }
        }
    }

    @Test
    fun theFirstSeriesTakesTheColorClosestToPrimary() {
        for (hue in listOf(0, 60, 154, 250, 300)) {
            for (isDark in listOf(false, true)) {
                val source = primary(hue, isDark)
                val closest = palette(isDark).minOf {
                    hueDistance(hctOf(it).hue, hctOf(source).hue)
                }

                val first = harmonizedChartColors(palette(isDark), source, isDark).first()

                assertTrue(
                    hueDistance(hctOf(first).hue, hctOf(source).hue) <= closest,
                    "hue $hue, dark $isDark",
                )
            }
        }
    }

    @Test
    fun theDefaultThemeTurnsTheHuesTowardsPrimary() {
        val source = primary(154)

        val harmonized = harmonizedChartColors(LightChartColors, source, isDark = false)

        // The default green theme starts with the aqua, turned towards its green
        val aqua = LightChartColors[2]
        assertTrue(
            hueDistance(hctOf(harmonized.first()).hue, 154.0) <
                hueDistance(hctOf(aqua).hue, 154.0),
        )
        assertNotEquals(LightChartColors, harmonized)
    }

    @Test
    fun harmonizingTurnsAtMostThirtyDegrees() {
        val blue = LightChartColors.first()

        val harmonized = blue.harmonize(hctOf(primary(30)), 1.0, 0.0, 50.0)

        val turn = hueDistance(hctOf(blue).hue, hctOf(harmonized).hue)
        assertTrue(turn in 25.0..30.5, "turned $turn")
    }

    @Test
    fun harmonizingWithoutStrengthKeepsTheColor() {
        val blue = LightChartColors.first()

        val harmonized = blue.harmonize(hctOf(primary(154)), 0.0, 0.0, 50.0)

        assertTrue(hueDistance(hctOf(blue).hue, hctOf(harmonized).hue) < 1)
        assertTrue(abs(hctOf(blue).tone - hctOf(harmonized).tone) < 1)
    }

    @Test
    fun toningMovesTheToneTowardsPrimary() {
        val yellow = LightChartColors[3]

        val harmonized = yellow.harmonize(hctOf(primary(154)), 0.0, 1.0, hctOf(yellow).tone)

        assertTrue(abs(hctOf(harmonized).tone - 40) < 2)
    }
}
