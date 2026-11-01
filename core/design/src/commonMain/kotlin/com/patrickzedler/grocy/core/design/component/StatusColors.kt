package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct

/**
 * A container color with the color of content on it, like a color role pair of the theme, and
 * [color] for small marks on surfaces, like the theme's primary role.
 */
@Immutable
data class StatusColor(val container: Color, val content: Color, val color: Color)

/**
 * Colors that mark a state like Grocy's highlighted rows, as colorful as the theme's primary
 * color and harmonized with it: [positive] (green, e.g. enough in stock), [info] (blue, e.g.
 * below the min. stock amount), [warning] (yellow, due soon) and [critical] (red, overdue or
 * expired). Always together with a text, never color alone.
 */
@Immutable
data class StatusColors(
    val positive: StatusColor,
    val info: StatusColor,
    val warning: StatusColor,
    val critical: StatusColor,
)

@Composable
fun rememberStatusColors(): StatusColors {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE
    return remember(primary, isDark) {
        StatusColors(
            positive = statusColorOf(StatusGreen, primary, isDark),
            info = statusColorOf(StatusBlue, primary, isDark),
            warning = statusColorOf(StatusYellow, primary, isDark),
            critical = statusColorOf(StatusRed, primary, isDark),
        )
    }
}

/**
 * The hue of [base] with the chroma of [primary], so every status color is as colorful as the
 * theme, turned towards [primary] like Material's harmonization, then as tones the way Material
 * derives a custom color's roles: container 90, content 10 and color 40 in light themes,
 * 30, 90 and 80 in dark ones.
 */
internal fun statusColorOf(base: Color, primary: Color, isDark: Boolean): StatusColor {
    val chroma = primary.toHct().chroma
    val hue = Hct.from(base.toHct().hue, chroma, LIGHT_COLOR_TONE).toColor()
        .harmonize(primary)
        .toHct()
        .hue
    fun tone(tone: Double) = Hct.from(hue, chroma, tone).toColor()
    return if (isDark) {
        StatusColor(
            container = tone(DARK_CONTAINER_TONE),
            content = tone(DARK_CONTENT_TONE),
            color = tone(DARK_COLOR_TONE),
        )
    } else {
        StatusColor(
            container = tone(LIGHT_CONTAINER_TONE),
            content = tone(LIGHT_CONTENT_TONE),
            color = tone(LIGHT_COLOR_TONE),
        )
    }
}

// Only their hues count, the chroma comes from the theme
internal val StatusGreen = Color(0xFF2E9E4F)
internal val StatusBlue = Color(0xFF2A78D6)
internal val StatusYellow = Color(0xFFEDA100)
internal val StatusRed = Color(0xFFE34948)

private const val LIGHT_CONTAINER_TONE = 90.0
private const val LIGHT_CONTENT_TONE = 10.0
private const val DARK_CONTAINER_TONE = 30.0
private const val DARK_CONTENT_TONE = 90.0
private const val LIGHT_COLOR_TONE = 40.0
private const val DARK_COLOR_TONE = 80.0
