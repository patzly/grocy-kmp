package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Wallpaper-based color scheme, or null where the platform has none. */
@Composable
internal expect fun platformDynamicColorScheme(isDark: Boolean): ColorScheme?

/** Light or dark system bar icons to match the theme, where the platform has system bars. */
@Composable
internal expect fun SystemBarAppearance(isDark: Boolean)

/** True once the theme fonts can be drawn, so the first visible frame never uses a fallback font. */
@Composable
expect fun rememberThemeFontsReady(): Boolean
