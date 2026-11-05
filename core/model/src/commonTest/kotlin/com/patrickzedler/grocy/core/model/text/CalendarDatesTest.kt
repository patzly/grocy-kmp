package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarDatesTest {

    private fun day(iso: String) = epochDayOf(iso)!!

    @Test
    fun monthsHaveTheirFirstAndLastDay() {
        assertEquals("2026-11-01", isoDateOf(firstDayOfMonth(day("2026-11-14"))))
        assertEquals("2026-11-30", isoDateOf(lastDayOfMonth(day("2026-11-14"))))
        assertEquals("2028-02-29", isoDateOf(lastDayOfMonth(day("2028-02-01"))))
    }

    @Test
    fun aDayKnowsItsYearAndItsDayOfTheMonth() {
        assertEquals(2026, yearOfDay(day("2026-11-14")))
        assertEquals(14, dayOfMonthOf(day("2026-11-14")))
    }

    @Test
    fun addingMonthsStaysWithinTheNewMonth() {
        assertEquals("2026-02-28", isoDateOf(addMonths(day("2026-01-31"), 1)))
        assertEquals("2027-01-15", isoDateOf(addMonths(day("2026-12-15"), 1)))
        assertEquals("2025-12-15", isoDateOf(addMonths(day("2026-01-15"), -1)))
    }

    @Test
    fun weeksFromMondayAreCountedLikeIso8601() {
        // The week of January 1st 2026 (a Thursday) is week 1, 2027 starts in week 53 of 2026
        assertEquals(1, weekOfYear(day("2025-12-29"), firstDayOfWeek = 1))
        assertEquals(45, weekOfYear(day("2026-11-02"), firstDayOfWeek = 1))
        assertEquals(53, weekOfYear(day("2026-12-28"), firstDayOfWeek = 1))
    }

    @Test
    fun weeksFromSundayStartWithTheWeekOfJanuaryFirst() {
        assertEquals(1, weekOfYear(day("2025-12-28"), firstDayOfWeek = 0))
        assertEquals(2, weekOfYear(day("2026-01-04"), firstDayOfWeek = 0))
    }
}
