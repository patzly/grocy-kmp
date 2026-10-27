package com.patrickzedler.grocy.core.model.stock

import com.patrickzedler.grocy.core.model.masterdata.DueType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DueStatusTest {

    private val today = 20_000L

    @Test
    fun dueDatesAreHighlightedLikeGrocy() {
        assertEquals(DueStatus.EXPIRED, dueStatusOf(today - 1, today, 5, DueType.EXPIRATION))
        assertEquals(DueStatus.OVERDUE, dueStatusOf(today - 1, today, 5, DueType.BEST_BEFORE))
        assertEquals(DueStatus.DUE_SOON, dueStatusOf(today, today, 5, DueType.BEST_BEFORE))
        assertEquals(DueStatus.DUE_SOON, dueStatusOf(today + 5, today, 5, DueType.BEST_BEFORE))
        assertNull(dueStatusOf(today + 6, today, 5, DueType.BEST_BEFORE))
    }
}
