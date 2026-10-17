package com.patrickzedler.grocy.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.patrickzedler.grocy.core.design.extensions.toCssHex

/** Chrome paints the status bar of an installed web app (and its own toolbar) in theme-color. */
internal val statusBarColors = StatusBarColorRequests { setThemeColor(it.toCssHex()) }

@Composable
internal actual fun StatusBarColorEffect(color: () -> Color) {
    val request = remember { StatusBarColorRequest() }
    // A new theme brings a new lambda with other colors, the running flow must read it
    val currentColor = rememberUpdatedState(color)
    DisposableEffect(request) {
        statusBarColors.add(request)
        onDispose { statusBarColors.remove(request) }
    }
    LaunchedEffect(request) {
        snapshotFlow { currentColor.value() }.collect { statusBarColors.setColor(request, it) }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(color) => document.querySelectorAll('meta[name="theme-color"]')
        .forEach((meta) => meta.setAttribute('content', color))""",
)
private external fun setThemeColor(color: String)
