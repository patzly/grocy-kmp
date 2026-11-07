package com.patrickzedler.grocy.apps.shared.widget

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import com.patrickzedler.grocy.core.design.ColorRoleSpec
import com.patrickzedler.grocy.core.design.component.StatusColor
import com.patrickzedler.grocy.core.design.component.customStatusColorOf
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.argbOfHexColor
import com.patrickzedler.grocy.core.model.server.UserSettings

/**
 * The app's colors in the widgets, each with its light and its dark value: the system switches
 * between them by itself when the device's dark theme changes (from Android 12 on; before, the next
 * redraw does). The background is the theme's secondary color at tone 95 and 20 like Google's
 * widgets.
 */
internal class WidgetColors(
    private val light: ColorScheme,
    private val dark: ColorScheme,
    private val lightSpec: ColorRoleSpec,
    private val darkSpec: ColorRoleSpec,
) {

    private fun of(role: (ColorScheme) -> Color): ColorProvider =
        ColorProvider(day = role(light), night = role(dark))

    val background = ColorProvider(
        day = toneOf(light.secondary, LIGHT_BACKGROUND_TONE),
        night = toneOf(dark.secondary, DARK_BACKGROUND_TONE),
    )
    val onBackground = of { it.onSurface }
    val onBackgroundVariant = of { it.onSurfaceVariant }
    val primary = of { it.primary }
    val onPrimary = of { it.onPrimary }
    val tile = of { it.secondaryContainer }
    val onTile = of { it.onSecondaryContainer }
    val line = of { it.outlineVariant }
    val error = of { it.error }

    /** The filled colors of events of [kind], harmonized like in the app's calendar. */
    fun event(kind: CalendarEventKind, settings: UserSettings): EventColors {
        val hex = kind.color.valueOf(settings)
        val day = eventColorOf(hex, light, lightSpec)
        val night = eventColorOf(hex, dark, darkSpec)
        return EventColors(
            container = ColorProvider(day.container, night.container),
            content = ColorProvider(day.content, night.content),
        )
    }
}

internal class EventColors(val container: ColorProvider, val content: ColorProvider)

/**
 * The color of an event as the app's calendar shows it: the user's color of its kind harmonized
 * with [scheme]'s primary, the theme's secondary without a valid one.
 */
internal fun eventColorOf(hex: String, scheme: ColorScheme, spec: ColorRoleSpec): StatusColor =
    argbOfHexColor(hex)?.let { customStatusColorOf(Color(it), scheme.primary, spec) }
        ?: StatusColor(scheme.secondary, scheme.onSecondary, scheme.secondary)

private fun toneOf(color: Color, tone: Double): Color {
    val hct = color.toHct()
    return Hct.from(hct.hue, hct.chroma, tone).toColor()
}

private const val LIGHT_BACKGROUND_TONE = 95.0
private const val DARK_BACKGROUND_TONE = 20.0
