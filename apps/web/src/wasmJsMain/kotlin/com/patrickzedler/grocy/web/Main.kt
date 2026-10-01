package com.patrickzedler.grocy.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.navigation.GrocyNavDisplay
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.start.api.startRouteSerializers
import com.patrickzedler.grocy.feature.start.impl.startEntry

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  ComposeViewport {
    GrocyTheme {
      GrocyNavDisplay(
        startRoute = StartRoute,
        routeSerializers = startRouteSerializers,
      ) {
        startEntry()
      }
    }
  }
}