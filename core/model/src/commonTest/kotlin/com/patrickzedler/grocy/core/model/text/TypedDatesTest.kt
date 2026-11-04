package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TypedDatesTest {

    @Test
    fun theOrderComesFromHowALanguageWritesADate() {
        assertEquals(DateFieldOrder.DAY_MONTH_YEAR, dateOrderOf("22.11.2003"))
        assertEquals(DateFieldOrder.DAY_MONTH_YEAR, dateOrderOf("22 Nov 2003"))
        assertEquals(DateFieldOrder.MONTH_DAY_YEAR, dateOrderOf("Nov 22, 2003"))
        assertEquals(DateFieldOrder.MONTH_DAY_YEAR, dateOrderOf("11/22/2003"))
        assertEquals(DateFieldOrder.YEAR_MONTH_DAY, dateOrderOf("2003-11-22"))
        assertEquals(DateFieldOrder.YEAR_MONTH_DAY, dateOrderOf("2003年11月22日"))
    }

    @Test
    fun aTwoDigitYearIsOfThisCentury() {
        assertEquals(
            "2026-12-31",
            grocyDateOfTyped("311226", DateFieldOrder.DAY_MONTH_YEAR, withTime = false),
        )
        assertEquals(
            "2026-12-31",
            grocyDateOfTyped("123126", DateFieldOrder.MONTH_DAY_YEAR, withTime = false),
        )
        assertEquals(
            "2026-12-31",
            grocyDateOfTyped("261231", DateFieldOrder.YEAR_MONTH_DAY, withTime = false),
        )
    }

    @Test
    fun aFourDigitYearCanBeTyped() {
        assertEquals(
            "2999-12-31",
            grocyDateOfTyped("31122999", DateFieldOrder.DAY_MONTH_YEAR, withTime = false),
        )
        assertEquals(
            "2026-12-31",
            grocyDateOfTyped("20261231", DateFieldOrder.YEAR_MONTH_DAY, withTime = false),
        )
    }

    @Test
    fun whatNamesNoDateIsNullAndNothingTypedIsEmpty() {
        val order = DateFieldOrder.DAY_MONTH_YEAR

        assertEquals("", grocyDateOfTyped("", order, withTime = false))
        assertNull(grocyDateOfTyped("3112", order, withTime = false))
        assertNull(grocyDateOfTyped("321226", order, withTime = false))
        assertNull(grocyDateOfTyped("311326", order, withTime = false))
    }

    @Test
    fun aDateWithoutTimeIsItsMidnight() {
        val order = DateFieldOrder.DAY_MONTH_YEAR

        assertEquals("2026-12-31 00:00:00", grocyDateOfTyped("311226", order, withTime = true))
        assertEquals("2026-12-31 13:45:00", grocyDateOfTyped("3112261345", order, withTime = true))
        assertEquals(
            "2026-12-31 13:45:00",
            grocyDateOfTyped("311220261345", order, withTime = true),
        )
        assertNull(grocyDateOfTyped("3112262545", order, withTime = true))
    }

    @Test
    fun aDateOfGrocyBecomesItsDigitsAndBack() {
        for (order in DateFieldOrder.entries) {
            val typed = typedDateOf("2026-12-31", order, withTime = false)

            assertEquals("2026-12-31", grocyDateOfTyped(typed, order, withTime = false))
        }
        assertEquals(
            "2026-12-31 13:45:00",
            grocyDateOfTyped(
                typedDateOf("2026-12-31 13:45:10", DateFieldOrder.MONTH_DAY_YEAR, withTime = true),
                DateFieldOrder.MONTH_DAY_YEAR,
                withTime = true,
            ),
        )
    }

    @Test
    fun aYearOfAnotherCenturyKeepsAllItsDigits() {
        assertEquals(
            "31122999",
            typedDateOf("2999-12-31", DateFieldOrder.DAY_MONTH_YEAR, withTime = false),
        )
    }

    @Test
    fun separatorsFollowOrderAndLength() {
        val day = DateFieldOrder.DAY_MONTH_YEAR

        assertEquals(emptyList(), typedDateSeparators(2, day, withTime = false))
        assertEquals(listOf(2 to "."), typedDateSeparators(3, day, withTime = false))
        assertEquals(listOf(2 to ".", 4 to "."), typedDateSeparators(6, day, withTime = false))
        assertEquals(
            listOf(4 to "-", 6 to "-"),
            typedDateSeparators(8, DateFieldOrder.YEAR_MONTH_DAY, withTime = false),
        )
        assertEquals(
            listOf(2 to ".", 4 to ".", 6 to " ", 8 to ":"),
            typedDateSeparators(10, day, withTime = true),
        )
    }
}
