@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.patrickzedler.grocy.core.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListDetailTest {

    @Test
    fun panesStartHalfAndHalf() {
        assertEquals(0.5f, PaneAnchors.first().proportion)
    }

    @Test
    fun arrowKeysMoveToTheNeighbouringAnchor() {
        val half = PaneAnchors.first()

        assertEquals(2f / 3, neighbourAnchor(half, towardsEnd = true)?.proportion)
        assertEquals(1f / 3, neighbourAnchor(half, towardsEnd = false)?.proportion)
    }

    @Test
    fun arrowKeysStopAtTheOuterAnchors() {
        assertNull(neighbourAnchor(PaneAnchorsInOrder.last(), towardsEnd = true))
        assertNull(neighbourAnchor(PaneAnchorsInOrder.first(), towardsEnd = false))
    }

    @Test
    fun handleWithoutAnchorGoesToHalfAndHalf() {
        assertEquals(0.5f, neighbourAnchor(null, towardsEnd = true)?.proportion)
    }
}
