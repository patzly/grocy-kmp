package com.patrickzedler.grocy.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.patrickzedler.grocy.apps.shared.AppGraph
import com.patrickzedler.grocy.apps.shared.GrocyApp
import com.patrickzedler.grocy.apps.shared.WebPlatformDependencies

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  val graph = AppGraph(WebPlatformDependencies())
  ComposeViewport {
    GrocyApp(graph)
  }
}
