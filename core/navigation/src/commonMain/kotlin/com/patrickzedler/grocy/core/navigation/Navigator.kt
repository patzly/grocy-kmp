package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

    /** The route on top, e.g. the detail shown beside its list. */
    val currentRoute: NavKey
        get() = backStack.last()

    fun navigate(route: Route) {
        backStack.add(route)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    // Asked before an entry with unsaved changes is replaced or removed from below
    private val leaveGuards = mutableMapOf<NavKey, LeaveGuard>()

    /**
     * Shows [route] as detail of the list on top: replaces the detail shown beside the list,
     * otherwise opens it. A replaced detail with unsaved changes is asked first.
     */
    fun openDetail(route: DetailRoute) {
        val current = backStack.last()
        if (current !is DetailRoute) {
            backStack.add(route)
        } else if (current != route) {
            guarded(current) { backStack[backStack.lastIndexOf(current)] = route }
        }
    }

    /**
     * Leaves [route] and everything opened on top of it, e.g. a list with its detail beside it;
     * entries with unsaved changes on top are asked first.
     */
    fun goBack(from: Route) {
        val index = backStack.lastIndexOf(from)
        if (index < 1) return
        val removed = backStack.subList(index, backStack.size).toList()
        val guarded = removed.lastOrNull { it in leaveGuards }
        val leave = {
            val start = backStack.lastIndexOf(from)
            if (start >= 1) backStack.subList(start, backStack.size).clear()
        }
        if (guarded == null) leave() else guarded(guarded, leave)
    }

    private fun guarded(route: NavKey, leave: () -> Unit) {
        val guard = leaveGuards[route]?.onLeaveRequest
        if (guard == null) leave() else guard(leave)
    }

    internal fun register(route: NavKey, guard: LeaveGuard) {
        leaveGuards[route] = guard
    }

    internal fun unregister(route: NavKey, guard: LeaveGuard) {
        if (leaveGuards[route] === guard) leaveGuards.remove(route)
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

/**
 * Lets the screen of a route ask before the navigator leaves it from elsewhere, e.g. when
 * another item of the list beside it is chosen. While [onLeaveRequest] is set, leaving calls it
 * with the leave action instead; the screen runs it once the user agrees.
 */
@Stable
class LeaveGuard {
    var onLeaveRequest: ((leave: () -> Unit) -> Unit)? = null
}

/** A [LeaveGuard] for [route], registered while the route's content is shown. */
@Composable
fun Navigator.rememberLeaveGuard(route: NavKey): LeaveGuard {
    val guard = remember(route) { LeaveGuard() }
    DisposableEffect(this, route, guard) {
        register(route, guard)
        onDispose { unregister(route, guard) }
    }
    return guard
}
