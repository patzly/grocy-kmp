package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize
import kotlin.math.abs

/**
 * Colors of chart series in a fixed order, never cycled: more series must be drawn together.
 * A validated categorical palette harmonized with the theme's primary color like the status
 * colors and Material's custom colors; the first series takes the palette's color closest to
 * primary. Harmonized hues can come close for color blind users, so charts mark their series
 * with [ChartMarker]s too.
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
 * [palette] from its color closest to [primary] on (the order stays, the last color neighbours
 * the first one), every color harmonized with [primary] by Material's rule.
 */
internal fun harmonizedChartColors(palette: List<Color>, primary: Color): List<Color> {
    val primaryHue = hueOf(primary)
    val closest = palette.indices.minBy { hueDistance(hueOf(palette[it]), primaryHue) }
    return (palette.drop(closest) + palette.take(closest)).map { it.harmonize(primary) }
}

/** The shape of a series' points, so series differ without telling their colors apart. */
enum class ChartMarker { Circle, Square, Diamond, Triangle }

/** The marker of the series with [colorIndex]: neighbouring colors never share one. */
fun chartMarkerOf(colorIndex: Int): ChartMarker =
    ChartMarker.entries[colorIndex % ChartMarker.entries.size]

private fun hueOf(color: Color): Double = Hct.fromInt(color.toArgb()).hue

private fun hueDistance(a: Double, b: Double): Double = abs((b - a + 540) % 360 - 180)

// Surfaces darker than this belong to a dark theme
internal const val DARK_LUMINANCE = 0.5f
