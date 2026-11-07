package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.toHct
import com.patrickzedler.grocy.core.design.ColorRoleSpec
import com.patrickzedler.grocy.core.design.LocalColorRoleSpec
import com.patrickzedler.grocy.core.design.customColorRoles
import com.patrickzedler.grocy.core.design.harmonizedHue
import kotlin.math.min

/**
 * A container color with the color of content on it, like a color role pair of the theme, and
 * [color] for small marks on surfaces, like the theme's primary role.
 */
@Immutable
data class StatusColor(val container: Color, val content: Color, val color: Color)

/**
 * Colors that mark a state like Grocy's highlighted rows, custom colors of the theme
 * ([customColorRoles]): [positive] (green, e.g. enough in stock), [info] (blue, e.g. below the
 * min. stock amount), [warning] (yellow, due soon) and [critical] (red, overdue or expired).
 * Always together with a text, never color alone.
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
    val spec = LocalColorRoleSpec.current
    return remember(primary, spec) {
        StatusColors(
            positive = statusColorOf(GREEN_HUE, primary, spec),
            info = statusColorOf(BLUE_HUE, primary, spec),
            warning = statusColorOf(YELLOW_HUE, primary, spec),
            critical = statusColorOf(RED_HUE, primary, spec),
        )
    }
}

/** Whether the theme is a dark one, for colors the theme does not derive itself. */
@Composable
fun isDarkColorScheme(): Boolean = LocalColorRoleSpec.current.isDark

/** The current theme's [customStatusColorOf] of [color]. */
@Composable
fun rememberCustomStatusColor(color: Color): StatusColor {
    val primary = MaterialTheme.colorScheme.primary
    val spec = LocalColorRoleSpec.current
    return remember(color, primary, spec) { customStatusColorOf(color, primary, spec) }
}

/**
 * The filled roles of a [color] the user chose, like the theme's primary and on primary: its
 * hue turned towards [primary] (harmonized), so it fits the theme and stays recognizable, never
 * more colorful than primary, so a calm choice stays calm. [StatusColor.container] and
 * [StatusColor.color] are the color role, [StatusColor.content] the role on it.
 */
fun customStatusColorOf(color: Color, primary: Color, spec: ColorRoleSpec): StatusColor {
    val hct = color.toHct()
    val roles = customColorRoles(
        hue = harmonizedHue(hct.hue, primary),
        primary = primary,
        spec = spec,
        chroma = min(hct.chroma, primary.toHct().chroma),
    )
    return StatusColor(container = roles.color, content = roles.onColor, color = roles.color)
}

/**
 * The custom color of [baseHue] harmonized with [primary]: its container and on container roles
 * for the highlighted areas, its color role for small marks.
 */
internal fun statusColorOf(baseHue: Double, primary: Color, spec: ColorRoleSpec): StatusColor {
    val roles = customColorRoles(harmonizedHue(baseHue, primary), primary, spec)
    return StatusColor(
        container = roles.container,
        content = roles.onContainer,
        color = roles.color,
    )
}

// CAM16's unique hues (HCT's hue), which look neither of their neighbours: a yellow that is no
// amber, a red that is no pink; the chroma comes from the theme
internal const val RED_HUE = 20.14
internal const val YELLOW_HUE = 90.0
internal const val GREEN_HUE = 164.25
internal const val BLUE_HUE = 237.53
