package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.patrickzedler.grocy.core.design.extensions.toCssHex
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.google_sans_flex_medium
import com.patrickzedler.grocy.core.resources.google_sans_flex_regular
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

@Composable
internal actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null

@Composable
internal actual fun SystemBarAppearance(isDark: Boolean, backgroundColor: Color) {
    val cssColor = backgroundColor.toCssHex()
    SideEffect {
        // Around the app, e.g. in the safe areas of a browser tab, the page shows
        setPageBackground(cssColor)
        // Screens without top app bar keep the status bar in the screen background
        statusBarColors.default = backgroundColor
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(color) => {
        document.documentElement.style.backgroundColor = color;
        document.body.style.backgroundColor = color;
    }""",
)
private external fun setPageBackground(color: String)

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun rememberThemeFontsReady(): Boolean {
    // Same weights as in grocyTypography, so the cached fonts are reused there
    val regular by preloadFont(
        Res.font.google_sans_flex_regular, FontWeight.Normal
    )
    val medium by preloadFont(
        Res.font.google_sans_flex_medium, FontWeight.Medium
    )
    return regular != null && medium != null
}

actual val isDynamicColorAvailable: Boolean = false

actual val dynamicColorFollowsSystemContrast: Boolean = false
