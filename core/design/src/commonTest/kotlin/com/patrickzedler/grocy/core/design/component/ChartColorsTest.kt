package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChartColorsTest {

    // Material's primary tones: 40 in light schemes, 80 in dark ones
    private fun primary(hue: Int, isDark: Boolean = false) =
        Color(Hct.from(hue.toDouble(), 48.0, if (isDark) 80.0 else 40.0).toInt())

    private fun hueOf(color: Color) = Hct.fromInt(color.toArgb()).hue

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
    fun theFirstSeriesTakesTheColorClosestToPrimary() {
        for (hue in listOf(0, 60, 154, 250, 300)) {
            for (isDark in listOf(false, true)) {
                val source = primary(hue, isDark)
                val closest = palette(isDark).minBy { hueDistance(hueOf(it), hue.toDouble()) }

                val first = harmonizedChartColors(palette(isDark), source).first()

                assertEquals(closest.harmonize(source), first, "hue $hue, dark $isDark")
            }
        }
    }

    @Test
    fun everyColorIsHarmonizedLikeTheStatusColors() {
        val source = primary(154)

        val harmonized = harmonizedChartColors(LightChartColors, source)

        // The default green theme starts with the aqua, the order stays
        val order = LightChartColors.drop(2) + LightChartColors.take(2)
        assertEquals(order.map { it.harmonize(source) }, harmonized)
        assertEquals(CHART_COLOR_COUNT, harmonized.distinct().size)
    }

    @Test
    fun harmonizingTurnsTheBlueTowardsAGreenPrimary() {
        val blue = LightChartColors.first()

        // The default green theme starts with the aqua, so the blue comes 6th
        val harmonized = harmonizedChartColors(LightChartColors, primary(154))[6]

        assertNotEquals(blue, harmonized)
        assertTrue(hueDistance(hueOf(harmonized), 154.0) < hueDistance(hueOf(blue), 154.0))
    }

    @Test
    fun neighbouringSeriesNeverShareAMarker() {
        for (index in 0 until CHART_COLOR_COUNT - 1) {
            assertNotEquals(chartMarkerOf(index), chartMarkerOf(index + 1))
        }
    }
}
