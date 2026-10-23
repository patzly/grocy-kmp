package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.patrickzedler.grocy.core.design.extensions.toCssHex
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.allDrawableResources
import com.patrickzedler.grocy.core.resources.allPluralStringResources
import com.patrickzedler.grocy.core.resources.allStringResources
import com.patrickzedler.grocy.core.resources.google_sans_flex_medium
import com.patrickzedler.grocy.core.resources.google_sans_flex_regular
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.preloadFont
import org.jetbrains.compose.resources.preloadImageVector

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
actual fun rememberResourcesReady(): Boolean {
    // Same weights as in grocyTypography, so the cached fonts are reused there
    val regular by preloadFont(
        Res.font.google_sans_flex_regular, FontWeight.Normal
    )
    val medium by preloadFont(
        Res.font.google_sans_flex_medium, FontWeight.Medium
    )
    // Browsers load every resource asynchronously: stringResource and painterResource show
    // nothing in their first frame unless the resource is cached already, so new screens
    // would fill in their texts and icons while sliding in
    val icons = Res.allDrawableResources.values.map { preloadImageVector(it) }
    val iconsReady = icons.all { it.value != null }
    val textsReady by produceState(false) {
        Res.allStringResources.values.forEach { getString(it) }
        Res.allPluralStringResources.values.forEach { getPluralString(it, quantity = 1) }
        value = true
    }
    return regular != null && medium != null && iconsReady && textsReady
}

actual val isDynamicColorAvailable: Boolean = false

actual val dynamicColorFollowsSystemContrast: Boolean = false
