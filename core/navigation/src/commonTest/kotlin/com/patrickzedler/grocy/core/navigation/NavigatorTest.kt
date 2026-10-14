package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Serializable
    private data object First : Route

    @Serializable
    private data object Second : Route

    @Serializable
    private data object Third : Route
}
