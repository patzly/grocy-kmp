package com.patrickzedler.grocy.apps.shared

import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import kotlin.test.Test

/**
 * A popup that closes while a predictive back gesture runs removes its input, but Android keeps
 * sending the gesture's progress to it. navigationevent before 1.2.0 threw then and the app
 * crashed (open an item's menu, swipe back right away).
 */
class NavigationEventVersionTest {

    private class GestureInput : NavigationEventInput() {
        fun progress() = dispatchOnBackProgressed(NavigationEvent())
    }

    @Test
    fun progressOfARemovedInputIsIgnored() {
        val dispatcher = NavigationEventDispatcher()
        val input = GestureInput()
        dispatcher.addInput(input)

        dispatcher.removeInput(input)

        input.progress()
    }
}
