package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable

class NavigatorTest {

    private val backStack = NavBackStack<NavKey>(First)
    private val navigator = Navigator(backStack)

    @Test
    fun navigateAddsRouteOnTop() {
        navigator.navigate(Second)

        assertEquals(listOf(First, Second), backStack.toList())
    }

    @Test
    fun goBackRemovesTopRoute() {
        navigator.navigate(Second)
        navigator.navigate(Third)

        navigator.goBack()

        assertEquals(listOf(First, Second), backStack.toList())
    }

    @Test
    fun goBackKeepsTheLastRoute() {
        navigator.goBack()

        assertEquals(listOf(First), backStack.toList())
    }

    @Test
    fun replaceAllLeavesOnlyTheNewRoute() {
        navigator.navigate(Second)

        navigator.replaceAll(Third)

        assertEquals(listOf(Third), backStack.toList())
    }

    @Test
    fun replaceAllWithCurrentRouteKeepsOneEntry() {
        navigator.replaceAll(First)

        assertEquals(listOf(First), backStack.toList())
    }

    @Test
    fun openDetailFromTheListAddsIt() {
        navigator.navigate(Second)

        navigator.openDetail(Detail(1))

        assertEquals(listOf(First, Second, Detail(1)), backStack.toList())
    }

    @Test
    fun openDetailReplacesTheDetailBesideTheList() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))

        navigator.openDetail(Detail(2))

        assertEquals(listOf(First, Second, Detail(2)), backStack.toList())
    }

    @Test
    fun openDetailOfAListOpenedFromADetailAddsIt() {
        // The start screen with the master data beside it, then the products
        navigator.openDetail(Detail(1))
        navigator.navigate(Second)

        navigator.openDetail(Detail(2))

        assertEquals(listOf(First, Detail(1), Second, Detail(2)), backStack.toList())
    }

    @Test
    fun openDetailAsksADetailWithUnsavedChangesFirst() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))
        var pendingLeave: (() -> Unit)? = null
        navigator.register(Detail(1), LeaveGuard().apply { onLeaveRequest = { pendingLeave = it } })

        navigator.openDetail(Detail(2))

        assertEquals(listOf(First, Second, Detail(1)), backStack.toList())
        pendingLeave?.invoke()
        assertEquals(listOf(First, Second, Detail(2)), backStack.toList())
    }

    @Test
    fun openDetailReplacesADetailAndTheOneOpenedFromIt() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))
        navigator.navigate(Detail(3))

        navigator.openDetail(Detail(2))

        assertEquals(listOf(First, Second, Detail(2)), backStack.toList())
    }

    @Test
    fun openDetailAsksADetailBelowTheTopWithUnsavedChanges() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))
        navigator.navigate(Detail(3))
        var pendingLeave: (() -> Unit)? = null
        navigator.register(Detail(1), LeaveGuard().apply { onLeaveRequest = { pendingLeave = it } })

        navigator.openDetail(Detail(2))

        assertEquals(listOf(First, Second, Detail(1), Detail(3)), backStack.toList())
        pendingLeave?.invoke()
        assertEquals(listOf(First, Second, Detail(2)), backStack.toList())
    }

    @Test
    fun goBackFromAListAlsoLeavesItsDetail() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))

        navigator.goBack(from = Second)

        assertEquals(listOf(First), backStack.toList())
    }

    @Test
    fun goBackFromAListAsksItsDetailWithUnsavedChanges() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))
        var asked = false
        navigator.register(Detail(1), LeaveGuard().apply { onLeaveRequest = { asked = true } })

        navigator.goBack(from = Second)

        assertTrue(asked)
        assertEquals(listOf(First, Second, Detail(1)), backStack.toList())
    }

    @Test
    fun unregisteredGuardsAreNotAsked() {
        navigator.navigate(Second)
        navigator.openDetail(Detail(1))
        val guard = LeaveGuard().apply { onLeaveRequest = { error("asked") } }
        navigator.register(Detail(1), guard)

        navigator.unregister(Detail(1), guard)
        navigator.openDetail(Detail(2))

        assertEquals(listOf(First, Second, Detail(2)), backStack.toList())
    }

    @Serializable
    private data class Detail(val id: Int) : DetailRoute

    @Serializable
    private data object First : Route

    @Serializable
    private data object Second : Route

    @Serializable
    private data object Third : Route
}
