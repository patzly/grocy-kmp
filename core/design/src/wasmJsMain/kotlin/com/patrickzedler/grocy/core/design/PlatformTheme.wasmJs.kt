package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null

@Composable
internal actual fun SystemBarAppearance(isDark: Boolean) = Unit
