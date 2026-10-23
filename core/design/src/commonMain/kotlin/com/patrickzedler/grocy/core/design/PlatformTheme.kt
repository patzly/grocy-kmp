package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Wallpaper-based color scheme, or null where the platform has none. */
@Composable
internal expect fun platformDynamicColorScheme(isDark: Boolean): ColorScheme?

/**
 * Matches the window around the app to the theme: light or dark system bar icons on Android,
 * page background and default status bar color ([backgroundColor]) on the web.
 */
@Composable
internal expect fun SystemBarAppearance(isDark: Boolean, backgroundColor: Color)

/**
 * True once fonts, texts and icons can be drawn right away, so the first visible frame never
 * uses a fallback font and no screen shows empty texts or icons while it slides in.
 */
@Composable
expect fun rememberResourcesReady(): Boolean

/** True where the system offers colors from the wallpaper (Android 12 and newer). */
expect val isDynamicColorAvailable: Boolean

/** True where dynamic colors follow the system's contrast setting (Android 14 and newer). */
expect val dynamicColorFollowsSystemContrast: Boolean
