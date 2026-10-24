package com.patrickzedler.grocy.feature.masterdata.impl.product

import kotlin.test.Test
import kotlin.test.assertEquals

class DatePickerTextTest {

    @Test
    fun pickedDateReplacesTheTextOfADateField() {
        assertEquals("2026-10-24", withPickedDate("2025-01-01", 20_750, withTime = false))
    }

    @Test
    fun pickedDateKeepsTheTimeOfADateTimeField() {
        val text = withPickedDate("2025-01-01 08:30:15", 20_750, withTime = true)

        assertEquals("2026-10-24 08:30:00", text)
    }

    @Test
    fun pickedDateWithoutTimeStartsAtMidnight() {
        assertEquals("2026-10-24 00:00:00", withPickedDate("", 20_750, withTime = true))
    }

    @Test
    fun pickedTimeKeepsTheDate() {
        val text = withPickedTime("2026-10-24 00:00:00", hour = 7, minute = 5, today = 0)

        assertEquals("2026-10-24 07:05:00", text)
    }

    @Test
    fun pickedTimeOfAFieldWithoutDateIsToday() {
        assertEquals("1970-01-02 18:45:00", withPickedTime("abc", 18, 45, today = 1))
    }
}
