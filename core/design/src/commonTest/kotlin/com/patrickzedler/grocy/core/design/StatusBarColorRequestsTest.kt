package com.patrickzedler.grocy.core.design

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusBarColorRequestsTest {

    private val applied = mutableListOf<Color>()
    private val requests = StatusBarColorRequests { applied += it }

    @Test
    fun screenBackgroundAppliesWithoutRequests() {
        requests.default = Color.Gray

        assertEquals(listOf(Color.Gray), applied)
    }

    @Test
    fun newestScreenWins() {
        requests.default = Color.Gray
        val leaving = StatusBarColorRequest().also(requests::add)
        requests.setColor(leaving, Color.Red)
        val entering = StatusBarColorRequest().also(requests::add)

        requests.setColor(entering, Color.Blue)

        assertEquals(Color.Blue, applied.last())
    }

    @Test
    fun removedScreenHandsBackToTheOneBelow() {
        val below = StatusBarColorRequest().also(requests::add)
        requests.setColor(below, Color.Red)
        val top = StatusBarColorRequest().also(requests::add)
        requests.setColor(top, Color.Blue)

        requests.remove(top)

        assertEquals(Color.Red, applied.last())
    }

    @Test
    fun requestWithoutColorYetKeepsTheCurrentOne() {
        requests.default = Color.Gray

        requests.add(StatusBarColorRequest())

        assertEquals(listOf(Color.Gray), applied)
    }

    @Test
    fun unchangedColorIsNotAppliedAgain() {
        val request = StatusBarColorRequest().also(requests::add)
        requests.setColor(request, Color.Red)

        requests.setColor(request, Color.Red)

        assertEquals(listOf(Color.Red), applied)
    }
}
