package com.patrickzedler.grocy.feature.calendar.impl

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarListStickyTest {

    @Test
    fun aRowBelowTheTopKeepsItsBadgeInPlace() {
        assertEquals(0, stuckBadgeShift(itemOffset = 40, itemSize = 300, badgeHeight = 100))
        assertEquals(0, stuckBadgeShift(itemOffset = 0, itemSize = 300, badgeHeight = 100))
    }

    @Test
    fun theBadgeFollowsTheTopWhileItsRowScrollsPast() {
        assertEquals(120, stuckBadgeShift(itemOffset = -120, itemSize = 300, badgeHeight = 100))
    }

    @Test
    fun theEndOfTheRowPushesTheBadgeAway() {
        assertEquals(200, stuckBadgeShift(itemOffset = -200, itemSize = 300, badgeHeight = 100))
        assertEquals(200, stuckBadgeShift(itemOffset = -260, itemSize = 300, badgeHeight = 100))
    }

    @Test
    fun aRowLowerThanItsBadgeNeverMovesIt() {
        assertEquals(0, stuckBadgeShift(itemOffset = -50, itemSize = 80, badgeHeight = 100))
    }
}
