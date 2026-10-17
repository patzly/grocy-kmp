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
    /** Writes the back stack into the browser history; null in tests. */
    internal val backStackCodec: BackStackCodec? = null,
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
     * Makes [route] the only entry, e.g. after login or logout, so back never crosses that
     * boundary.
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
 *
 * On the web a reload restores the back stack the browser history kept, if it starts with
 * [startRoute]; otherwise, e.g. after a logout in another tab, the app starts on [startRoute].
 */
@Composable
fun rememberNavigator(
    startRoute: Route,
    routeSerializers: SerializersModule,
): Navigator {
    val configuration = remember(routeSerializers) {
        SavedStateConfiguration { serializersModule = routeSerializers }
    }
    val codec = remember(routeSerializers) { BackStackCodec(routeSerializers) }
    val initialBackStack = remember(codec, startRoute) {
        restoredBackStack(codec)?.takeIf { it.first() == startRoute } ?: listOf(startRoute)
    }
    val backStack = rememberNavBackStack(configuration, *initialBackStack.toTypedArray())
    return remember(backStack, codec) { Navigator(backStack, codec) }
}

/** The back stack the browser history kept for the current page, null on Android. */
internal expect fun restoredBackStack(codec: BackStackCodec): List<NavKey>?
