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

/** True once the theme fonts can be drawn, so the first visible frame never uses a fallback font. */
@Composable
expect fun rememberThemeFontsReady(): Boolean
