package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Colors of chart series in a fixed order, never cycled: more series must be drawn together.
 * A validated categorical palette (neighbours stay apart for color blind users too), harmonized
 * with the theme's primary color as far as it stays valid: the first series is the palette's
 * color closest to primary, and every hue turns towards primary.
 */
@Composable
fun rememberChartColors(): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE
    return remember(primary, isDark) {
        harmonizedChartColors(if (isDark) DarkChartColors else LightChartColors, primary, isDark)
    }
}

/** Number of distinct series colors. */
const val CHART_COLOR_COUNT = 8

// Blue, orange, aqua, yellow, magenta, green, violet, red
internal val LightChartColors = listOf(
    Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100),
    Color(0xFFE87BA4), Color(0xFF008300), Color(0xFF6250D6), Color(0xFFE34948),
)

internal val DarkChartColors = listOf(
    Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500),
    Color(0xFFD55181), Color(0xFF008300), Color(0xFF9085E9), Color(0xFFE66767),
)

/**
 * [palette] harmonized with [primary]: it starts at its color closest to primary (the order
 * stays, the last color neighbours the first one), then the strongest hue rotation and tone
 * shift of [HarmonizationStrengths] that keep [isValidPalette]; the palette itself is valid.
 */
internal fun harmonizedChartColors(
    palette: List<Color>,
    primary: Color,
    isDark: Boolean,
): List<Color> {
    val source = Hct.fromInt(primary.toArgb())
    val closest = palette.indices.minBy { abs(hueDifference(hctOf(palette[it]).hue, source.hue)) }
    val orders = listOf(palette.drop(closest) + palette.take(closest), palette)
    val meanTone = palette.map { hctOf(it).tone }.average()
    for (order in orders) {
        for (rotation in HarmonizationStrengths) {
            for (toning in HarmonizationStrengths) {
                val colors = order.map { it.harmonize(source, rotation, toning, meanTone) }
                if (isValidPalette(colors, isDark)) return colors
            }
        }
    }
    return palette
}

/**
 * Turns the hue towards [source] like Material's harmonization, half their difference but up to
 * [MAX_ROTATION] degrees, times [rotation]; [toning] moves the tone towards the one of [source],
 * keeping the palette's spread around its [meanTone]. Chroma stays, so the colors keep their
 * weight.
 */
internal fun Color.harmonize(
    source: Hct,
    rotation: Double,
    toning: Double,
    meanTone: Double,
): Color {
    val from = hctOf(this)
    val difference = hueDifference(from.hue, source.hue)
    val turn = min(abs(difference) / 2, MAX_ROTATION) * sign(difference) * rotation
    val hue = (from.hue + turn + FULL_TURN) % FULL_TURN
    val themedTone = source.tone + (from.tone - meanTone) * TONE_SPREAD
    val tone = (from.tone + (themedTone - from.tone) * toning).coerceIn(MIN_TONE, MAX_TONE)
    return Color(Hct.from(hue, from.chroma, tone).toInt())
}

private fun hctOf(color: Color): Hct = Hct.fromInt(color.toArgb())

// From [from] to [to] the short way round, -180 to 180 degrees
private fun hueDifference(from: Double, to: Double): Double =
    (to - from + HALF_TURN * 3) % FULL_TURN - HALF_TURN

/**
 * The dataviz validator's checks of a categorical palette: every color inside the lightness
 * band of its mode with enough chroma not to read gray, neighbours [isDistinguishable].
 */
internal fun isValidPalette(colors: List<Color>, isDark: Boolean): Boolean {
    val band = if (isDark) DarkLightnessBand else LightLightnessBand
    return colors.all { color ->
        val (lightness, a, b) = oklab(linearRgb(color))
        lightness in band && sqrt(a * a + b * b) >= CHROMA_FLOOR
    } && isDistinguishable(colors)
}

/**
 * Whether neighbours of [colors] can be told apart with normal vision and when simulated for
 * protanopia and deuteranopia (Machado et al. 2009), measured as OKLab distance x100.
 */
internal fun isDistinguishable(colors: List<Color>): Boolean =
    colors.zipWithNext().all { (a, b) ->
        distance(a, b, transform = null) >= NORMAL_VISION_MIN_DISTANCE &&
            distance(a, b, Protanopia) >= COLOR_BLIND_MIN_DISTANCE &&
            distance(a, b, Deuteranopia) >= COLOR_BLIND_MIN_DISTANCE
    }

private fun distance(a: Color, b: Color, transform: List<DoubleArray>?): Double {
    val first = oklab(simulate(linearRgb(a), transform))
    val second = oklab(simulate(linearRgb(b), transform))
    return PERCENT * sqrt(first.indices.sumOf { (first[it] - second[it]).pow(2) })
}

private fun linearRgb(color: Color) = listOf(color.red, color.green, color.blue).map {
    val channel = it.toDouble()
    if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}

private fun simulate(rgb: List<Double>, transform: List<DoubleArray>?): List<Double> =
    transform?.map { row -> rgb.indices.sumOf { row[it] * rgb[it] }.coerceIn(0.0, 1.0) } ?: rgb

private fun oklab(rgb: List<Double>): List<Double> {
    val (r, g, b) = rgb
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return listOf(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
    )
}

// Machado, Oliveira & Fernandes (2009) at severity 1.0, on linear RGB
private val Protanopia = listOf(
    doubleArrayOf(0.152286, 1.052583, -0.204868),
    doubleArrayOf(0.114503, 0.786281, 0.099216),
    doubleArrayOf(-0.003882, -0.048116, 1.051998),
)
private val Deuteranopia = listOf(
    doubleArrayOf(0.367322, 0.860646, -0.227968),
    doubleArrayOf(0.280085, 0.672501, 0.047413),
    doubleArrayOf(-0.011820, 0.042940, 0.968881),
)

// From full strength down to none, rotation before tone: hues matter most for the look
private val HarmonizationStrengths = listOf(1.0, 0.8, 0.6, 0.4, 0.2, 0.0)

// Twice Material's 15 degrees: its harmonization alone left the palette looking foreign
private const val MAX_ROTATION = 30.0
private const val TONE_SPREAD = 0.8
private const val MIN_TONE = 5.0
private const val MAX_TONE = 95.0

// OKLCH lightness and chroma limits of the dataviz validator
private val LightLightnessBand = 0.43..0.77
private val DarkLightnessBand = 0.48..0.67
private const val CHROMA_FLOOR = 0.10
private const val NORMAL_VISION_MIN_DISTANCE = 15.0
private const val COLOR_BLIND_MIN_DISTANCE = 8.0
private const val PERCENT = 100.0
private const val FULL_TURN = 360.0
private const val HALF_TURN = 180.0

// Surfaces darker than this belong to a dark theme
internal const val DARK_LUMINANCE = 0.5f
