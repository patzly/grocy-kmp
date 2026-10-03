package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule

/**
 * The only way to change the back stack, so features never touch it directly.
 */
@Stable
class Navigator internal constructor(
    internal val backStack: NavBackStack<NavKey>,
) {

    fun navigate(route: Route) {
        backStack.add(route)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /**
     * Makes [route] the only entry, e.g. after login or logout, so back never crosses that boundary.
     */
    fun replaceAll(route: Route) {
        // Add first: the back stack must never be empty, NavDisplay cannot show nothing
        backStack.add(route)
        backStack.subList(0, backStack.lastIndex).clear()
    }
}

/**
 * [routeSerializers] must register every route under NavKey, because Web has no reflection for
 * state saving.
 */
@Composable
fun rememberNavigator(
    startRoute: Route,
    routeSerializers: SerializersModule,
): Navigator {
    val configuration = remember(routeSerializers) {
        SavedStateConfiguration { serializersModule = routeSerializers }
    }
    val backStack = rememberNavBackStack(configuration, startRoute)
    return remember(backStack) { Navigator(backStack) }
}
