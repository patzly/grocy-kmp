package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

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

// OKLCH lightness and chroma limits of the dataviz validator
private val LightLightnessBand = 0.43..0.77
private val DarkLightnessBand = 0.48..0.67
private const val CHROMA_FLOOR = 0.10
private const val NORMAL_VISION_MIN_DISTANCE = 15.0
private const val COLOR_BLIND_MIN_DISTANCE = 8.0
private const val PERCENT = 100.0
