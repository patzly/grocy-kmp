package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import com.materialkolor.contrast.Contrast
import com.materialkolor.ktx.toHct
import com.patrickzedler.grocy.core.design.ColorRoleSpec
import com.patrickzedler.grocy.core.design.colorRoleSpecOf
import com.patrickzedler.grocy.core.design.harmonizedHue
import com.patrickzedler.grocy.core.design.seededPrimaryColor
import com.patrickzedler.grocy.core.design.surfaceTone
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ChartColorsTest {

    private fun primary(hue: Float, isDark: Boolean = false) =
        seededPrimaryColor(AppearanceSettings(hue = hue), isDark)

    private fun hueOf(color: Color) = color.toHct().hue

    @Test
    fun everySeriesGetsItsOwnColor() {
        for (isDark in listOf(false, true)) {
            val colors = chartColorsOf(primary(154f, isDark), ColorRoleSpec(isDark))

            assertEquals(CHART_COLOR_COUNT, colors.size)
            assertEquals(CHART_COLOR_COUNT, colors.distinct().size)
        }
    }

    @Test
    fun neighboursStayApartForColorBlindUsersInEveryTheme() {
        for (style in AppPaletteStyle.entries) {
            for (contrast in AppContrast.entries) {
                for (hue in listOf(0f, 30f, 60f, 90f, 120f, 154f, 180f, 210f, 250f, 280f, 320f)) {
                    for (isDark in listOf(false, true)) {
                        val appearance =
                            AppearanceSettings(hue = hue, contrast = contrast, style = style)
                        val colors = chartColorsOf(
                            seededPrimaryColor(appearance, isDark),
                            colorRoleSpecOf(appearance, isDark),
                        )

                        // The last series neighbours the first once the order starts elsewhere
                        assertTrue(
                            isDistinguishable(colors + colors.first()),
                            "$style $contrast hue $hue dark $isDark",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun everySeriesStandsOutFromTheSurface() {
        for (contrast in AppContrast.entries) {
            for (isDark in listOf(false, true)) {
                val appearance = AppearanceSettings(contrast = contrast)
                val spec = colorRoleSpecOf(appearance, isDark)
                val surface = surfaceTone(spec)
                val minimum = if (contrast == AppContrast.HIGH) 4.4 else 2.9

                chartColorsOf(seededPrimaryColor(appearance, isDark), spec).forEach {
                    val ratio = Contrast.ratioOfTones(surface, it.toHct().tone)
                    assertTrue(ratio >= minimum, "$contrast dark $isDark: $ratio")
                }
            }
        }
    }

    @Test
    fun theFirstSeriesTakesTheHueClosestToPrimary() {
        for (hue in listOf(0f, 60f, 154f, 250f, 300f)) {
            val source = primary(hue)
            val closest = ChartHues.minBy { hueDistance(it, hueOf(source)) }

            val first = chartColorsOf(source, ColorRoleSpec(isDark = false)).first()

            assertTrue(hueDistance(hueOf(first), harmonizedHue(closest, source)) < 2.0, "$hue")
        }
    }

    @Test
    fun moreContrastMovesTheColorsFurtherFromTheSurface() {
        val source = primary(154f)

        val standard = chartColorsOf(source, ColorRoleSpec(isDark = false))
        val high = chartColorsOf(source, ColorRoleSpec(isDark = false, contrastLevel = 1.0))

        standard.zip(high).forEach { (normal, contrast) ->
            assertTrue(contrast.toHct().tone < normal.toHct().tone)
        }
    }

    @Test
    fun calmThemesStillGetColorfulSeries() {
        val appearance = AppearanceSettings(style = AppPaletteStyle.MONOCHROME)
        val colors = chartColorsOf(
            seededPrimaryColor(appearance, isDark = false),
            colorRoleSpecOf(appearance, isDark = false),
        )

        // Unless sRGB has no color that colorful at the hue and tone
        colors.forEach { assertTrue(it.toHct().chroma > CHART_MIN_CHROMA - 12, "${it.toHct()}") }
    }

    @Test
    fun neighbouringSeriesNeverShareAMarker() {
        for (index in 0 until CHART_COLOR_COUNT - 1) {
            assertNotEquals(chartMarkerOf(index), chartMarkerOf(index + 1))
        }
    }

    @Test
    fun hueDistanceGoesTheShortWayRound() {
        assertTrue(abs(hueDistance(350.0, 10.0) - 20.0) < 0.001)
    }

    @Test
    fun equalToneColorsAreOneGrayInEveryTheme() {
        listOf(false, true).forEach { isDark ->
            (0 until 360 step 30).forEach { hue ->
                val primary = primary(hue.toFloat(), isDark)
                val colors = equalToneChartColorsOf(primary)

                (colors.series + colors.other).forEach { color ->
                    assertTrue(abs(color.toHct().tone - primary.toHct().tone) < 0.5, "$hue")
                }
            }
        }
    }

    @Test
    fun equalToneColorsUseTheWholeHueCircleAndKeepNeighboursApart() {
        val series = equalToneChartColorsOf(primary(154f)).series

        assertEquals(CHART_COLOR_COUNT, series.toSet().size)
        (series + series.first()).zipWithNext().forEach { (a, b) ->
            assertTrue(hueDistance(hueOf(a), hueOf(b)) > 90.0)
        }
    }

    @Test
    fun equalToneColorsTakeTheFirstHueAndChromaFromPrimary() {
        val primary = primary(154f)

        val first = equalToneChartColorsOf(primary).series.first()

        assertTrue(hueDistance(hueOf(first), hueOf(primary)) < 2.0)
        assertTrue(abs(first.toHct().chroma - primary.toHct().chroma) < 2.0)
    }

    @Test
    fun whatIsLeftOverHasNoColor() {
        val other = equalToneChartColorsOf(primary(154f)).other

        assertTrue(abs(other.red - other.green) < 0.01f && abs(other.green - other.blue) < 0.01f)
    }
}
