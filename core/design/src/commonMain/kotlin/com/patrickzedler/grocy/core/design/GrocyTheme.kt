package com.patrickzedler.grocy.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.rememberDynamicColorScheme
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings

/** Material 3 Expressive theme of the app in the user's [appearance]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GrocyTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit,
) {
    val isDark = when (appearance.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val dynamicColorScheme = if (appearance.color == AppColor.DYNAMIC) {
        platformDynamicColorScheme(isDark = isDark)
    } else {
        null
    }
    val colorScheme = dynamicColorScheme ?: run {
        val seedColor = remember(appearance.hue) { seedColorOf(appearance.hue) }
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = appearance.style.paletteStyle,
            contrastLevel = appearance.contrast.level,
            modifyColorScheme = ColorScheme::harmonizeError,
        )
    }

    val shownColorScheme = animateColorScheme(
        target = colorScheme,
        transitionKey = ColorTransitionKey(
            isDark = isDark,
            color = appearance.color,
            style = appearance.style,
            contrast = appearance.contrast,
        ),
    )

    SystemBarAppearance(isDark = isDark, backgroundColor = shownColorScheme.surfaceContainer)

    // Custom color roles take their tones like the theme's own roles; the wallpaper's colors
    // are tonal spot ones in the system's contrast
    val systemContrast = platformContrastLevel()
    val isWallpaper = dynamicColorScheme != null
    val roleSpec = remember(appearance, isDark, isWallpaper, systemContrast) {
        if (isWallpaper) {
            ColorRoleSpec(isDark = isDark, contrastLevel = systemContrast)
        } else {
            colorRoleSpecOf(appearance, isDark)
        }
    }
    CompositionLocalProvider(LocalColorRoleSpec provides roleSpec) {
        MaterialExpressiveTheme(
            colorScheme = shownColorScheme,
            typography = grocyTypography(),
            shapes = GrocyShapes,
            content = content,
        )
    }
}

/** The primary color [GrocyTheme] shows for [appearance] without colors of the wallpaper. */
fun seededPrimaryColor(appearance: AppearanceSettings, isDark: Boolean): Color =
    seededColorScheme(appearance, isDark).primary

/** The colors [GrocyTheme] shows for [appearance] without colors of the wallpaper. */
internal fun seededColorScheme(appearance: AppearanceSettings, isDark: Boolean): ColorScheme =
    dynamicColorScheme(
        seedColor = seedColorOf(appearance.hue),
        isDark = isDark,
        style = appearance.style.paletteStyle,
        contrastLevel = appearance.contrast.level,
    ).harmonizeError()

private fun seedColorOf(hue: Float): Color =
    Hct.from(hue.toDouble(), SEED_CHROMA, SEED_TONE).toColor()

/** The parts of the appearance whose changes fade the screen into its new colors. */
private data class ColorTransitionKey(
    val isDark: Boolean,
    val color: AppColor,
    val style: AppPaletteStyle,
    val contrast: AppContrast,
)

/** Chroma and tone of the seed color, the hue comes from the settings. */
internal const val SEED_CHROMA = 70.0
internal const val SEED_TONE = 60.0

private val AppPaletteStyle.paletteStyle: PaletteStyle
    get() = when (this) {
        AppPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
        AppPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
        AppPaletteStyle.VIBRANT -> PaletteStyle.Vibrant
        AppPaletteStyle.RAINBOW -> PaletteStyle.Rainbow
        AppPaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
    }

