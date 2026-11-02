package com.patrickzedler.grocy.feature.chores.impl

import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresJournalRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.masterdata.api.ChoreRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChoresEntriesTest {

    @Test
    fun pagesOfOneChoreMarkItInTheList() {
        assertEquals(3, selectedChoreOf(ChoresJournalRoute(3)))
        assertEquals(3, selectedChoreOf(ChoreTrackingRoute(3)))
    }

    @Test
    fun pagesOfAllChoresMarkNone() {
        assertNull(selectedChoreOf(ChoresOverviewRoute))
        assertNull(selectedChoreOf(ChoresJournalRoute()))
        assertNull(selectedChoreOf(ChoreTrackingRoute()))
    }

    @Test
    fun theOverviewCountsAsShownBesideItsPages() {
        assertTrue(isInChoresScene(ChoresOverviewRoute))
        assertTrue(isInChoresScene(ChoresJournalRoute()))
        assertTrue(isInChoresScene(ChoreTrackingRoute()))
    }

    @Test
    fun theOverviewIsHiddenBelowPagesOnTop() {
        assertFalse(isInChoresScene(ChoreRoute(1)))
        assertFalse(isInChoresScene(ChoreTrackingRoute(inList = false)))
    }
}
