package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.text.input.TextFieldState
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DueDateScanTest {

    private val today = epochDayOf("2026-11-08")!!

    @Test
    fun scannedDateGivesTheDaysUntilIt() {
        assertEquals("332", dueDaysOfScanned(epochDayOf("2027-10-06")!!, today))
        assertEquals("1", dueDaysOfScanned(today + 1, today))
    }

    @Test
    fun dateNotAfterTodayGivesNoDays() {
        assertNull(dueDaysOfScanned(today, today))
        assertNull(dueDaysOfScanned(today - 30, today))
    }

    @Test
    fun scanWritesTheDaysAndClosesTheCamera() {
        val field = TextFieldState("7")
        val scan = DueDaysScan(field)
        scan.isOpen = true

        scan.onDate(today + 365, today)

        assertEquals("365", field.text.toString())
        assertFalse(scan.isOpen)
        assertNull(scan.invalidDate)
    }

    @Test
    fun pastDateKeepsTheDaysAndExplainsWhy() {
        val field = TextFieldState("7")
        val scan = DueDaysScan(field)
        scan.isOpen = true

        scan.onDate(today - 2, today)

        assertEquals("7", field.text.toString())
        assertFalse(scan.isOpen)
        assertEquals(today - 2, scan.invalidDate)
    }
}
