package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SliderStepTrackerTest {

    @Test
    fun everyNewStepCountsOnce() {
        val tracker = SliderStepTracker(value = 0f, stepSize = 18f)

        val moves = listOf(18f, 18f, 18f, 36f, 36f, 54f).map { tracker.moveTo(it) }

        assertEquals(listOf(true, false, false, true, false, true), moves)
    }

    @Test
    fun valuesOfOneStepWithRoundingErrorsAreTheSameStep() {
        val tracker = SliderStepTracker(value = 36f, stepSize = 18f)

        assertFalse(tracker.moveTo(36.00001f))
        assertFalse(tracker.moveTo(35.99998f))
    }

    @Test
    fun movingBackToTheFormerStepCountsAgain() {
        val tracker = SliderStepTracker(value = 0f, stepSize = 18f)
        tracker.moveTo(18f)

        assertTrue(tracker.moveTo(0f))
    }

    @Test
    fun valueSetFromElsewhereIsNoMove() {
        val tracker = SliderStepTracker(value = 0f, stepSize = 18f)

        tracker.reset(90f)

        assertFalse(tracker.moveTo(90f))
        assertTrue(tracker.moveTo(108f))
    }
}
