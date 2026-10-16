package com.patrickzedler.grocy.web

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.ComposeViewport
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.GrocyApp
import com.patrickzedler.grocy.apps.shared.WebPlatformDependencies
import com.patrickzedler.grocy.core.design.component.observeBrowserKeyboard

private const val VIEWPORT_CONTAINER_ID = "composeApp"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val graph = AppGraph(
        WebPlatformDependencies(versionName = APP_VERSION_NAME, buildId = APP_BUILD_ID),
    )
    observeBrowserKeyboard(VIEWPORT_CONTAINER_ID)
    ComposeViewport(viewportContainerId = VIEWPORT_CONTAINER_ID) {
        CompositionLocalProvider(LocalHapticFeedback provides NoHapticFeedback) {
            GrocyApp(
                graph = graph,
                onContentReady = ::hideLoadingScreen,
            )
        }
    }
}

/**
 * Browsers can only vibrate at full strength without amplitude control, so even the shortest
 * pattern feels like a harsh buzz instead of a subtle click. The web app therefore stays silent.
 */
private object NoHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/** Removes the HTML loading screen from index.html once Compose shows the first screen. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => document.getElementById('loading')?.remove()")
private external fun hideLoadingScreen()
