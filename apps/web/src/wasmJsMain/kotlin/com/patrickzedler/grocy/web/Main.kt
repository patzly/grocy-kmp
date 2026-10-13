package com.patrickzedler.grocy.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.GrocyApp
import com.patrickzedler.grocy.apps.shared.WebPlatformDependencies

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val graph = AppGraph(WebPlatformDependencies(versionName = APP_VERSION_NAME))
    ComposeViewport(viewportContainerId = "composeApp") {
        GrocyApp(
            graph = graph,
            onContentReady = ::hideLoadingScreen,
        )
    }
}

/** Removes the HTML loading screen from index.html once Compose shows the first screen. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => document.getElementById('loading')?.remove()")
private external fun hideLoadingScreen()
