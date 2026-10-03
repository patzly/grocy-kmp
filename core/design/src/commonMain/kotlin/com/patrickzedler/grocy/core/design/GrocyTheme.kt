package com.patrickzedler.grocy.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.rememberDynamicColorScheme
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppTheme

/** Hue of the default seed color. */
const val DEFAULT_THEME_HUE = 154f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GrocyTheme(
    color: AppColor = AppColor.STATIC,
    hue: Float = DEFAULT_THEME_HUE,
    theme: AppTheme = AppTheme.SYSTEM,
    contrast: AppContrast = AppContrast.STANDARD,
    content: @Composable () -> Unit,
) {
    val isDark = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    SystemBarAppearance(isDark = isDark)

    val dynamicColorScheme = if (color == AppColor.DYNAMIC) {
        platformDynamicColorScheme(isDark = isDark)
    } else {
        null
    }
    val colorScheme = dynamicColorScheme ?: run {
        val seedColor = remember(hue) {
            Hct.from(hue.toDouble(), SEED_CHROMA, SEED_TONE).toColor()
        }
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            contrastLevel = contrast.level,
            modifyColorScheme = ColorScheme::harmonizeError,
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = grocyTypography(),
        shapes = GrocyShapes,
        content = content,
    )
}

private const val SEED_CHROMA = 70.0
private const val SEED_TONE = 60.0

private val AppContrast.level: Double
    get() = when (this) {
        AppContrast.STANDARD -> 0.0
        AppContrast.MEDIUM -> 0.5
        AppContrast.HIGH -> 1.0
    }
