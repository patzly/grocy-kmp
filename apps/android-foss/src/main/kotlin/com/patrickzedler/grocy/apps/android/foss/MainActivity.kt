package com.patrickzedler.grocy.apps.android.foss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.navigation.GrocyNavDisplay
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.start.api.startRouteSerializers
import com.patrickzedler.grocy.feature.start.impl.startEntry

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
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
}