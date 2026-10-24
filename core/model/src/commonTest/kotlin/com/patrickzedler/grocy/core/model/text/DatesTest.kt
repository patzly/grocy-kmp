package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatesTest {

    @Test
    fun isoDatesBecomeEpochDays() {
        assertEquals(0, epochDayOf("1970-01-01"))
        assertEquals(-1, epochDayOf("1969-12-31"))
        assertEquals(19_782, epochDayOf("2024-02-29"))
        // Grocy sometimes sends a time after the date
        assertEquals(20_750, epochDayOf("2026-10-24 13:45:00"))
    }

    @Test
    fun invalidDatesAreNone() {
        assertNull(epochDayOf("2026-02-29"))
        assertNull(epochDayOf("2026-13-01"))
        assertNull(epochDayOf("24.10.2026"))
        assertNull(epochDayOf(""))
    }

    @Test
    fun spansAreRoundedLikeGrocysWebFrontend() {
        assertEquals(TimeSpan(0, TimeSpanUnit.DAY), timeSpanBetween(100, 100))
        assertEquals(TimeSpan(-1, TimeSpanUnit.DAY), timeSpanBetween(100, 99))
        assertEquals(TimeSpan(-25, TimeSpanUnit.DAY), timeSpanBetween(100, 75))
        assertEquals(TimeSpan(-1, TimeSpanUnit.MONTH), timeSpanBetween(100, 74))
        assertEquals(TimeSpan(3, TimeSpanUnit.MONTH), timeSpanBetween(0, 90))
        assertEquals(TimeSpan(-10, TimeSpanUnit.MONTH), timeSpanBetween(400, 81))
        assertEquals(TimeSpan(-1, TimeSpanUnit.YEAR), timeSpanBetween(400, 80))
        assertEquals(TimeSpan(2, TimeSpanUnit.YEAR), timeSpanBetween(0, 700))
    }

    @Test
    fun durationsArePositive() {
        assertEquals(TimeSpan(12, TimeSpanUnit.DAY), durationOf(12.4))
        assertEquals(TimeSpan(2, TimeSpanUnit.MONTH), durationOf(-60.0))
    }

    @Test
    fun datesAreWrittenWithTheirYear() {
        assertTrue("2026" in formatDate(20_750), formatDate(20_750))
    }
}
