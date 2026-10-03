package com.patrickzedler.grocy.apps.shared

import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.navigation.GrocyNavDisplay
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.start.api.startRouteSerializers
import com.patrickzedler.grocy.feature.start.impl.startEntry

/**
 * Shared entry point of the Android and web apps.
 */
@Composable
fun GrocyApp(
    graph: AppGraph,
) {
    GrocyTheme {
        GrocyNavDisplay(
            startRoute = StartRoute,
            routeSerializers = startRouteSerializers,
        ) {
            startEntry()
        }
    }
}