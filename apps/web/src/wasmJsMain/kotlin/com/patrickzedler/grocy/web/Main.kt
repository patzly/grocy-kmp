package com.patrickzedler.grocy.web

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ComposeViewport
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.GrocyApp
import com.patrickzedler.grocy.apps.shared.WebPlatformDependencies
import com.patrickzedler.grocy.core.design.component.observeBrowserKeyboard
import com.patrickzedler.grocy.core.model.isRightToLeft

private const val VIEWPORT_CONTAINER_ID = "composeApp"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val graph = AppGraph(
        WebPlatformDependencies(versionName = APP_VERSION_NAME, buildId = APP_BUILD_ID),
    )
    observeBrowserKeyboard(VIEWPORT_CONTAINER_ID)
    // Android takes the direction from the language itself, Compose on the web does not
    val layoutDirection = if (isRightToLeft(browserLanguage())) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    setDocumentDirection(if (layoutDirection == LayoutDirection.Rtl) "rtl" else "ltr")
    ComposeViewport(viewportContainerId = VIEWPORT_CONTAINER_ID) {
        CompositionLocalProvider(
            LocalHapticFeedback provides NoHapticFeedback,
            LocalLayoutDirection provides layoutDirection,
        ) {
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

/** Also for the accessibility tree, which Compose mirrors into the page. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(direction) => { document.documentElement.dir = direction; }")
private external fun setDocumentDirection(direction: String)

/** The language Compose uses for the texts, index.html puts the one chosen in the app there. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => navigator.language || 'en'")
private external fun browserLanguage(): String
