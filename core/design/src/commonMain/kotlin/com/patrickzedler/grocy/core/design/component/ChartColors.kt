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
 * A validated categorical palette (neighbours stay apart for color blind users too), its hues
 * turned towards the theme's primary color as far as they stay that distinguishable.
 */
@Composable
fun rememberChartColors(): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE
    return remember(primary, isDark) {
        harmonizedChartColors(if (isDark) DarkChartColors else LightChartColors, primary)
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
 * [palette] harmonized with [primary] like Material's color harmonization, with the strongest
 * hue rotation of [HarmonizationSteps] that keeps [isDistinguishable]; the palette itself is.
 */
internal fun harmonizedChartColors(palette: List<Color>, primary: Color): List<Color> =
    HarmonizationSteps.asSequence()
        .map { maxRotation -> palette.map { it.harmonize(primary, maxRotation) } }
        .firstOrNull(::isDistinguishable)
        ?: palette

/**
 * Turns the hue towards [source] by half their difference, at most [maxRotation] degrees.
 * Chroma and tone stay, so the colors keep their weight.
 */
internal fun Color.harmonize(source: Color, maxRotation: Double): Color {
    val from = Hct.fromInt(toArgb())
    val towards = Hct.fromInt(source.toArgb()).hue
    val difference = (towards - from.hue + HALF_TURN * 3) % FULL_TURN - HALF_TURN
    val rotation = min(abs(difference) / 2, maxRotation) * sign(difference)
    val hue = (from.hue + rotation + FULL_TURN) % FULL_TURN
    return Color(Hct.from(hue, from.chroma, from.tone).toInt())
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

// Material harmonizes by up to 15 degrees; red themes pull green and red too close for that
private val HarmonizationSteps = listOf(15.0, 12.0, 9.0, 6.0, 3.0)
private const val NORMAL_VISION_MIN_DISTANCE = 15.0
private const val COLOR_BLIND_MIN_DISTANCE = 8.0
private const val PERCENT = 100.0
private const val FULL_TURN = 360.0
private const val HALF_TURN = 180.0
private const val DARK_LUMINANCE = 0.5f
