package com.patrickzedler.grocy.feature.start.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionSwipeTest {

    @Test
    fun theCircleFollowsTheFingerSlower() {
        assertEquals(6f, swipeOffsetOf(10f, distance = 100f))
    }

    @Test
    fun itStopsAtTheFullWay() {
        assertEquals(100f, swipeOffsetOf(1000f, distance = 100f))
    }

    @Test
    fun aSwipeUpMovesNothing() {
        assertEquals(0f, swipeOffsetOf(-50f, distance = 100f))
    }

    @Test
    fun releasedPastHalfTheWayItGoesOn() {
        assertTrue(swipeCommits(50f, distance = 100f))
    }

    @Test
    fun releasedBeforeThatItFallsBack() {
        assertFalse(swipeCommits(49f, distance = 100f))
    }
}
