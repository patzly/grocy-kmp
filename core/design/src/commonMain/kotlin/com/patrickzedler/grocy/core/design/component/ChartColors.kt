package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Colors of chart series in a fixed order, never cycled: more series must be drawn together.
 * Not derived from the theme: this order keeps neighbours apart for color blind users too (a
 * validated categorical palette), each step chosen for the light or dark surface.
 */
val chartColors: List<Color>
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE) {
        DarkChartColors
    } else {
        LightChartColors
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

private const val DARK_LUMINANCE = 0.5f
