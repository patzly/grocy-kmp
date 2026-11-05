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
            positive = statusColorOf(GREEN_HUE, primary, isDark),
            info = statusColorOf(BLUE_HUE, primary, isDark),
            warning = statusColorOf(YELLOW_HUE, primary, isDark),
            critical = statusColorOf(RED_HUE, primary, isDark),
        )
    }
}

/** Whether the theme is a dark one, for colors the theme does not derive itself. */
@Composable
fun isDarkColorScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE

/**
 * The roles of a [color] the user chose, like Material derives a custom color's: turned towards
 * [primary] (harmonized), so it fits the theme and stays recognizable, and its chroma at most that
 * of the theme's colorful roles. [StatusColor.container] and [StatusColor.color] are its filled
 * role, tone 40 in light themes and 80 in dark ones, with [StatusColor.content] 100 and 20 on it.
 */
fun customStatusColorOf(color: Color, primary: Color, isDark: Boolean): StatusColor {
    val hct = color.harmonize(primary).toHct()
    val chroma = hct.chroma.coerceAtMost(MAX_CUSTOM_CHROMA)
    fun tone(tone: Double) = Hct.from(hct.hue, chroma, tone).toColor()
    val filled = tone(if (isDark) DARK_COLOR_TONE else LIGHT_COLOR_TONE)
    return StatusColor(
        container = filled,
        content = tone(if (isDark) DARK_ON_COLOR_TONE else LIGHT_ON_COLOR_TONE),
        color = filled,
    )
}

/**
 * [baseHue] with the chroma of [primary], so every status color is as colorful as the theme,
 * turned towards [primary] like Material's harmonization, then as tones the way Material
 * derives a custom color's roles: container 90, content 10 and color 40 in light themes,
 * 30, 90 and 80 in dark ones.
 */
internal fun statusColorOf(baseHue: Double, primary: Color, isDark: Boolean): StatusColor {
    val chroma = primary.toHct().chroma
    val hue = Hct.from(baseHue, chroma, LIGHT_COLOR_TONE).toColor()
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

// CAM16's unique hues (HCT's hue), which look neither of their neighbours: a yellow that is no
// amber, a red that is no pink; the chroma comes from the theme
internal const val RED_HUE = 20.14
internal const val YELLOW_HUE = 90.0
internal const val GREEN_HUE = 164.25
internal const val BLUE_HUE = 237.53

private const val LIGHT_CONTAINER_TONE = 90.0
private const val LIGHT_ON_COLOR_TONE = 100.0
private const val DARK_ON_COLOR_TONE = 20.0
private const val MAX_CUSTOM_CHROMA = 48.0
private const val LIGHT_CONTENT_TONE = 10.0
private const val DARK_CONTAINER_TONE = 30.0
private const val DARK_CONTENT_TONE = 90.0
private const val LIGHT_COLOR_TONE = 40.0
private const val DARK_COLOR_TONE = 80.0
