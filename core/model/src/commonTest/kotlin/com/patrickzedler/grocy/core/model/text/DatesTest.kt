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
    fun weekdaysCountFromSundayLikeJavaScript() {
        // 1970-01-01 was a Thursday, 2026-11-01 a Sunday
        assertEquals(4, weekdayOf(0))
        assertEquals(0, weekdayOf(epochDayOf("2026-11-01")!!))
        assertEquals(1, weekdayOf(epochDayOf("2026-11-02")!!))
        assertEquals(3, weekdayOf(-1))
    }

    @Test
    fun yearWeeksAreSqlitesMondayWeeks() {
        // 2026 starts on a Thursday, its first Monday (January 5) starts week 01
        assertEquals("2026-00", yearWeekOf(epochDayOf("2026-01-04")!!))
        assertEquals("2026-01", yearWeekOf(epochDayOf("2026-01-05")!!))
        assertEquals("2026-44", yearWeekOf(epochDayOf("2026-11-02")!!))
        assertEquals("2026-44", yearWeekOf(epochDayOf("2026-11-08")!!))
        assertEquals("2026-45", yearWeekOf(epochDayOf("2026-11-09")!!))
        // 2024 starts on a Monday
        assertEquals("2024-01", yearWeekOf(epochDayOf("2024-01-01")!!))
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

    @Test
    fun localTimesBecomeGrocyDateTimes() {
        val day = epochDayOf("2026-10-24")!!
        val millis = day * 86_400_000L + ((14 * 60 + 5) * 60 + 9) * 1_000L

        assertEquals("2026-10-24 14:05:09", grocyDateTimeOf(millis))
    }

    @Test
    fun grocyDateTimesHandleLeapDaysAndEarlyDates() {
        val leapDay = epochDayOf("2024-02-29")!! * 86_400_000L

        assertEquals("2024-02-29 00:00:00", grocyDateTimeOf(leapDay))
        assertEquals("1969-12-31 23:59:59", grocyDateTimeOf(-1_000L))
    }

    @Test
    fun nowIsAValidGrocyDateTime() {
        val now = grocyDateTimeOf(localNowMillis())

        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""").matches(now), now)
    }

    @Test
    fun epochDaysBecomeIsoDates() {
        assertEquals("1970-01-01", isoDateOf(0))
        assertEquals("2024-02-29", isoDateOf(19_782))
        assertEquals("1969-12-31", isoDateOf(-1))
    }

    @Test
    fun isoDatesSurviveTheRoundTrip() {
        for (day in listOf(-300_000L, -1L, 0L, 20_750L, 2_932_896L)) {
            assertEquals(day, epochDayOf(isoDateOf(day)))
        }
    }

    @Test
    fun timeOfDayIsReadFromGrocyDateTimes() {
        assertEquals(8 to 30, timeOfDayOf("2026-10-24 08:30:15"))
        assertEquals(23 to 59, timeOfDayOf("2026-10-24 23:59"))
    }

    @Test
    fun datesWithoutValidTimeHaveNoTimeOfDay() {
        assertNull(timeOfDayOf("2026-10-24"))
        assertNull(timeOfDayOf("2026-10-24 24:00:00"))
        assertNull(timeOfDayOf("08:30"))
    }

    @Test
    fun labelsStartWithACapitalLetter() {
        assertEquals("Morgen", capitalizeStart("morgen"))
        assertEquals("In 3 Tagen", capitalizeStart("in 3 Tagen"))
        assertEquals("Вчера", capitalizeStart("вчера"))
    }

    @Test
    fun labelsWithoutLetterCaseStayTheSame() {
        assertEquals("3 gün sonra", capitalizeStart("3 gün sonra"))
        assertEquals("明天", capitalizeStart("明天"))
        assertEquals("", capitalizeStart(""))
    }
}
