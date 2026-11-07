package com.patrickzedler.grocy.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.Variant
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlin.math.min

/**
 * The color roles of a custom color, like the theme's primary roles: [color] for filled and small
 * marks with [onColor] on it, [container] for calmer areas with [onContainer] on it.
 */
@Immutable
data class CustomColorRoles(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

/**
 * What the theme computes its roles from besides its colors: dark or light, the contrast level
 * and the palette style's variant. Custom roles use the same, so their tones follow the theme
 * like its own roles do.
 */
@Immutable
data class ColorRoleSpec(
    val isDark: Boolean,
    val contrastLevel: Double = 0.0,
    val variant: Variant = Variant.TONAL_SPOT,
)

/** The spec of [GrocyTheme]'s roles for [appearance] in a light or dark ([isDark]) scheme. */
fun colorRoleSpecOf(appearance: AppearanceSettings, isDark: Boolean) = ColorRoleSpec(
    isDark = isDark,
    contrastLevel = appearance.contrast.level,
    variant = appearance.style.variant,
)

/** The spec of the current theme, provided by [GrocyTheme]. */
val LocalColorRoleSpec = staticCompositionLocalOf { ColorRoleSpec(isDark = false) }

/**
 * The roles of a custom color of [hue] (HCT), as colorful as the theme's [primary] color: only
 * the hue is chosen, Material's dynamic colors give every role its tone for the theme's light or
 * dark scheme and contrast level, exactly like they give the primary roles theirs. [chroma]
 * replaces primary's where a color needs its own, e.g. a calmer one the user chose.
 */
fun customColorRoles(
    hue: Double,
    primary: Color,
    spec: ColorRoleSpec,
    chroma: Double = primary.toHct().chroma,
): CustomColorRoles {
    val scheme = customScheme(hue, chroma, spec)
    val roles = MaterialDynamicColors()
    fun Int.color() = Color(this)
    return CustomColorRoles(
        color = roles.primary().getArgb(scheme).color(),
        onColor = roles.onPrimary().getArgb(scheme).color(),
        container = roles.primaryContainer().getArgb(scheme).color(),
        onContainer = roles.onPrimaryContainer().getArgb(scheme).color(),
    )
}

/**
 * The tone of the theme's surface in [spec]'s scheme, which marks like a chart's need their
 * contrast to.
 */
fun surfaceTone(spec: ColorRoleSpec): Double =
    MaterialDynamicColors().surface().getHct(customScheme(0.0, 0.0, spec)).tone

/** A scheme whose colorful palettes are all the custom color of [hue] and [chroma]. */
private fun customScheme(hue: Double, chroma: Double, spec: ColorRoleSpec): DynamicScheme {
    val palette = TonalPalette.fromHueAndChroma(hue, chroma)
    // Only the surfaces' tones matter for the contrast, they do not depend on the chroma
    val neutral = TonalPalette.fromHueAndChroma(hue, min(chroma, NEUTRAL_CHROMA))
    return DynamicScheme(
        Hct.from(hue, chroma, SOURCE_TONE),
        spec.variant,
        spec.isDark,
        spec.contrastLevel,
        palette,
        palette,
        palette,
        neutral,
        neutral,
    )
}

/** [hue] turned towards [primary] by Material's harmonization, so it fits the theme. */
fun harmonizedHue(hue: Double, primary: Color): Double =
    Hct.from(hue, HARMONIZE_CHROMA, SOURCE_TONE).toColor().harmonize(primary).toHct().hue

/** [customColorRoles] of [hue] in the current theme, harmonized with its primary color. */
@Composable
fun rememberCustomColorRoles(hue: Double): CustomColorRoles {
    val primary = MaterialTheme.colorScheme.primary
    val spec = LocalColorRoleSpec.current
    return remember(hue, primary, spec) {
        customColorRoles(harmonizedHue(hue, primary), primary, spec)
    }
}

internal val AppContrast.level: Double
    get() = when (this) {
        AppContrast.STANDARD -> 0.0
        AppContrast.MEDIUM -> 0.5
        AppContrast.HIGH -> 1.0
    }

internal val AppPaletteStyle.variant: Variant
    get() = when (this) {
        AppPaletteStyle.TONAL_SPOT -> Variant.TONAL_SPOT
        AppPaletteStyle.NEUTRAL -> Variant.NEUTRAL
        AppPaletteStyle.VIBRANT -> Variant.VIBRANT
        AppPaletteStyle.RAINBOW -> Variant.RAINBOW
        AppPaletteStyle.MONOCHROME -> Variant.MONOCHROME
    }

// The palette's key tone; the roles take their own tones from it
private const val SOURCE_TONE = 50.0

// Colorful enough that harmonizing finds the hue at every tone
private const val HARMONIZE_CHROMA = 48.0

// Material's neutral palettes stay this calm
private const val NEUTRAL_CHROMA = 6.0
