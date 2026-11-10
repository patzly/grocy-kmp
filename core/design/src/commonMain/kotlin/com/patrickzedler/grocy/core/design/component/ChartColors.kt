package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.contrast.Contrast
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import com.patrickzedler.grocy.core.design.ColorRoleSpec
import com.patrickzedler.grocy.core.design.LocalColorRoleSpec
import com.patrickzedler.grocy.core.design.harmonizedHue
import com.patrickzedler.grocy.core.design.surfaceTone
import kotlin.math.abs
import kotlin.math.max

/**
 * Colors of chart series in a fixed order, never cycled: more series must be drawn together.
 * Only their hues are fixed, in the order of a validated categorical palette that puts warm and
 * cool hues next to each other; the first series takes the hue closest to primary. Everything
 * else follows the theme ([chartColorsOf]).
 */
@Composable
fun rememberChartColors(): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    val spec = LocalColorRoleSpec.current
    return remember(primary, spec) { chartColorsOf(primary, spec) }
}

/** Number of distinct series colors. */
const val CHART_COLOR_COUNT = 8

// Blue, orange, aqua, yellow, magenta, green, violet, red (HCT hues)
internal val ChartHues = listOf(261.4, 38.3, 164.5, 75.9, 358.2, 142.2, 291.3, 22.1)

/**
 * The series colors for [primary] in a scheme of [spec], a graphic role of each hue: harmonized
 * with primary and as colorful as primary, but at least [minChroma], since series are told apart
 * by their color. Their tones come from the contrast to the surface, like Material's roles: 3:1
 * like graphics need, more with the theme's contrast level, so every color stands out from the
 * surface in light and dark schemes; [toneOffsets] move each position further from the surface,
 * so neighbours also differ in lightness, which color blind users see.
 */
internal fun chartColorsOf(
    primary: Color,
    spec: ColorRoleSpec,
    toneOffsets: List<Double> = ChartToneOffsets,
    minChroma: Double = CHART_MIN_CHROMA,
): List<Color> {
    val primaryHct = primary.toHct()
    val chroma = max(primaryHct.chroma, minChroma)
    val surface = surfaceTone(spec)
    val ratio = GRAPHIC_CONTRAST +
        (HIGH_GRAPHIC_CONTRAST - GRAPHIC_CONTRAST) * spec.contrastLevel.coerceAtLeast(0.0)
    // The tone closest to the surface that still keeps the ratio
    val edge = if (spec.isDark) {
        Contrast.lighterUnsafe(surface, ratio)
    } else {
        Contrast.darkerUnsafe(surface, ratio)
    }
    val closest = ChartHues.indices.minBy { hueDistance(ChartHues[it], primaryHct.hue) }
    val hues = ChartHues.drop(closest) + ChartHues.take(closest)
    return hues.mapIndexed { position, hue ->
        val offset = toneOffsets[position % toneOffsets.size]
        val tone = if (spec.isDark) edge + offset else edge - offset
        Hct.from(harmonizedHue(hue, primary), chroma, tone.coerceIn(0.0, 100.0)).toColor()
    }
}

// Every other series lies further from the surface; found by ChartColorsTest's check of every
// theme hue, style, contrast level, light and dark scheme, neighbours and first with last
internal val ChartToneOffsets = listOf(0.0, 24.0, 0.0, 24.0, 0.0, 24.0, 6.0, 24.0)

// The chroma of the default theme's primary: calmer styles keep colorful series
internal const val CHART_MIN_CHROMA = 36.0

// WCAG's ratio for graphics, and what Material's high contrast asks of its colors
private const val GRAPHIC_CONTRAST = 3.0
private const val HIGH_GRAPHIC_CONTRAST = 4.5

/** Series told apart by hue alone and the color of what is left over ([other]). */
data class EqualToneChartColors(val series: List<Color>, val other: Color)

/**
 * Colors of a chart whose parts are meant to weigh the same: every color has primary's tone, in
 * grayscale they are one gray. A list beside the chart has to tell the parts apart for whoever
 * cannot tell these hues apart.
 */
@Composable
fun rememberEqualToneChartColors(): EqualToneChartColors {
    val primary = MaterialTheme.colorScheme.primary
    return remember(primary) { equalToneChartColorsOf(primary) }
}

/** Primary's chroma and tone around the whole hue circle; [other] has its tone without color. */
internal fun equalToneChartColorsOf(primary: Color): EqualToneChartColors {
    val hct = primary.toHct()
    return EqualToneChartColors(
        series = List(CHART_COLOR_COUNT) { position ->
            val hue = (hct.hue + position * EQUAL_TONE_HUE_STEP) % FULL_HUE_CIRCLE
            Hct.from(hue, hct.chroma, hct.tone).toColor()
        },
        other = Hct.from(hct.hue, 0.0, hct.tone).toColor(),
    )
}

// 3 of 8 parts of the circle: all hues get used and neighbours are far apart, also last and first
private const val EQUAL_TONE_HUE_STEP = 135.0
private const val FULL_HUE_CIRCLE = 360.0

/** The shape of a series' points, so series differ without telling their colors apart. */
enum class ChartMarker { Circle, Square, Diamond, Triangle }

/** The marker of the series with [colorIndex]: neighbouring colors never share one. */
fun chartMarkerOf(colorIndex: Int): ChartMarker =
    ChartMarker.entries[colorIndex % ChartMarker.entries.size]

internal fun hueDistance(a: Double, b: Double): Double = abs((b - a + 540) % 360 - 180)
