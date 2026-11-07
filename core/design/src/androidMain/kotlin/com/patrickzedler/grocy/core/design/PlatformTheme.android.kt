package com.patrickzedler.grocy.core.design

import android.content.Context
import android.content.ContextWrapper
import android.app.UiModeManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings

@Composable
internal actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return remember(isDark, context) {
        val scheme = if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        scheme.harmonizeError()
    }
}

@Composable
internal actual fun platformContrastLevel(): Double {
    val context = LocalContext.current
    // Read again with the configuration, e.g. when the app comes back from the settings
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { systemContrastLevel(context) }
}

/** The system's contrast level of Android 14 and newer, which its dynamic colors follow. */
internal fun systemContrastLevel(context: Context): Double =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(UiModeManager::class.java).contrast.toDouble()
    } else {
        0.0
    }

@Composable
internal actual fun SystemBarAppearance(isDark: Boolean, backgroundColor: ComposeColor) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(isDark, activity) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
        activity?.enableEdgeToEdge(
            statusBarStyle = style,
            navigationBarStyle = style,
        )
    }
}

// Fonts are packaged in the APK and load synchronously
@Composable
actual fun rememberResourcesReady(): Boolean = true


/**
 * The primary color [GrocyTheme] shows for [appearance], for what is drawn outside of the app such
 * as the color of notifications.
 */
fun appPrimaryColor(context: Context, appearance: AppearanceSettings): ComposeColor {
    val isDark = when (appearance.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    return if (appearance.color == AppColor.DYNAMIC && isDynamicColorAvailable) {
        (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else {
        seededPrimaryColor(appearance, isDark)
    }
}

/**
 * What the custom color roles of [appearance]'s light and dark colors are computed from (see
 * [appColorSchemes]): the wallpaper's colors follow the system's contrast, not the app's.
 */
fun appColorRoleSpecs(
    context: Context,
    appearance: AppearanceSettings,
): Pair<ColorRoleSpec, ColorRoleSpec> =
    if (appearance.color == AppColor.DYNAMIC && isDynamicColorAvailable) {
        val contrast = systemContrastLevel(context)
        ColorRoleSpec(isDark = false, contrastLevel = contrast) to
            ColorRoleSpec(isDark = true, contrastLevel = contrast)
    } else {
        colorRoleSpecOf(appearance, isDark = false) to colorRoleSpecOf(appearance, isDark = true)
    }

/**
 * The light and the dark colors [GrocyTheme] shows for [appearance], for what follows the system's
 * dark theme outside of the app such as widgets.
 */
fun appColorSchemes(
    context: Context,
    appearance: AppearanceSettings,
): Pair<ColorScheme, ColorScheme> =
    if (appearance.color == AppColor.DYNAMIC && isDynamicColorAvailable) {
        dynamicLightColorScheme(context).harmonizeError() to
            dynamicDarkColorScheme(context).harmonizeError()
    } else {
        seededColorScheme(appearance, isDark = false) to
            seededColorScheme(appearance, isDark = true)
    }

private fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

actual val isDynamicColorAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual val dynamicColorFollowsSystemContrast: Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
