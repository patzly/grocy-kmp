package com.patrickzedler.grocy.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.PaletteStyle
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
        val seedColor = remember(appearance.hue) {
            Hct.from(appearance.hue.toDouble(), SEED_CHROMA, SEED_TONE).toColor()
        }
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

    MaterialExpressiveTheme(
        colorScheme = shownColorScheme,
        typography = grocyTypography(),
        shapes = GrocyShapes,
        content = content,
    )
}

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
        AppPaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
        AppPaletteStyle.RAINBOW -> PaletteStyle.Rainbow
        AppPaletteStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
        AppPaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
        AppPaletteStyle.FIDELITY -> PaletteStyle.Fidelity
        AppPaletteStyle.CONTENT -> PaletteStyle.Content
    }

private val AppContrast.level: Double
    get() = when (this) {
        AppContrast.STANDARD -> 0.0
        AppContrast.MEDIUM -> 0.5
        AppContrast.HIGH -> 1.0
    }
