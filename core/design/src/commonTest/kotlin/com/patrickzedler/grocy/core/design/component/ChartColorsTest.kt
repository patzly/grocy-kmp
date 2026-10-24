package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChartColorsTest {

    private fun primary(hue: Int) = Color(Hct.from(hue.toDouble(), 48.0, 40.0).toInt())

    private fun hueOf(color: Color) = Hct.fromInt(color.toArgb()).hue

    @Test
    fun bothModesHaveEveryColorOnce() {
        for (colors in listOf(LightChartColors, DarkChartColors)) {
            assertEquals(CHART_COLOR_COUNT, colors.size)
            assertEquals(CHART_COLOR_COUNT, colors.distinct().size)
        }
    }

    @Test
    fun palettesAreDistinguishableThemselves() {
        assertTrue(isDistinguishable(LightChartColors))
        assertTrue(isDistinguishable(DarkChartColors))
    }

    @Test
    fun harmonizedColorsStayDistinguishableForEveryPrimaryHue() {
        for (hue in 0 until 360) {
            for (palette in listOf(LightChartColors, DarkChartColors)) {
                val colors = harmonizedChartColors(palette, primary(hue))

                assertTrue(isDistinguishable(colors), "hue $hue")
            }
        }
    }

    @Test
    fun harmonizingTurnsTheHuesTowardsThePrimaryColor() {
        val blue = LightChartColors.first()

        val harmonized = blue.harmonize(primary(154), maxRotation = 15.0)

        assertNotEquals(blue, harmonized)
        assertTrue(abs(hueOf(harmonized) - 154) < abs(hueOf(blue) - 154))
    }

    @Test
    fun harmonizingTurnsAtMostTheGivenDegrees() {
        val blue = LightChartColors.first()

        val harmonized = blue.harmonize(primary(154), maxRotation = 5.0)

        assertTrue(abs(hueOf(harmonized) - hueOf(blue)) <= 5.5)
    }

    @Test
    fun redThemesHarmonizeLessThanMaterialsFifteenDegrees() {
        val fully = LightChartColors.map { it.harmonize(primary(0), maxRotation = 15.0) }

        val harmonized = harmonizedChartColors(LightChartColors, primary(0))

        assertFalse(isDistinguishable(fully))
        assertNotEquals(fully, harmonized)
    }

    @Test
    fun theDefaultThemeHarmonizesTheColors() {
        val harmonized = harmonizedChartColors(LightChartColors, primary(154))

        assertNotEquals(LightChartColors, harmonized)
    }
}
