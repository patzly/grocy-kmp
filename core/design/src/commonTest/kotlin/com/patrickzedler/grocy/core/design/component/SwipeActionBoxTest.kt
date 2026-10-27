package com.patrickzedler.grocy.core.design.component

import kotlin.test.Test
import kotlin.test.assertEquals

class SwipeActionBoxTest {

    @Test
    fun itemFollowsTheDragUpToItsWidth() {
        assertEquals(30f, swipeOffset(10f, 20f, 300f, true, true))
        assertEquals(300f, swipeOffset(290f, 50f, 300f, true, true))
        assertEquals(-300f, swipeOffset(-290f, -50f, 300f, true, true))
    }

    @Test
    fun itemOnlyMovesTowardsSidesWithAnAction() {
        assertEquals(0f, swipeOffset(0f, 40f, 300f, canSwipeTowardsEnd = false, true))
        assertEquals(0f, swipeOffset(0f, -40f, 300f, true, canSwipeTowardsStart = false))
        assertEquals(-40f, swipeOffset(0f, -40f, 300f, canSwipeTowardsEnd = false, true))
    }

    @Test
    fun thresholdStaysWithinReachOfNarrowItems() {
        assertEquals(96f, swipeThreshold(width = 400f, preferred = 96f))
        assertEquals(80f, swipeThreshold(width = 200f, preferred = 96f))
        // Before the item is measured
        assertEquals(96f, swipeThreshold(width = 0f, preferred = 96f))
    }

    @Test
    fun iconRestsNearTheEdgeUntilTheSurfacesMiddleReachesIt() {
        // Surface at the start: the icon is uncovered from the start without moving
        assertEquals(16f, swipeIconStart(30f, 24f, 16f, surfaceAtStart = true))
        assertEquals(16f, swipeIconStart(56f, 24f, 16f, surfaceAtStart = true))
        assertEquals(38f, swipeIconStart(100f, 24f, 16f, surfaceAtStart = true))
        // Surface at the end: the same distance from the end edge
        assertEquals(-10f, swipeIconStart(30f, 24f, 16f, surfaceAtStart = false))
        assertEquals(16f, swipeIconStart(56f, 24f, 16f, surfaceAtStart = false))
        assertEquals(38f, swipeIconStart(100f, 24f, 16f, surfaceAtStart = false))
    }
}
