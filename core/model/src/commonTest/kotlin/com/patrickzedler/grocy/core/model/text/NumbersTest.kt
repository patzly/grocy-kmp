package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumbersTest {

    @Test
    fun trailingZerosAreDropped() {
        assertEquals("2", formatDecimal(2.0, maxDecimals = 4))
        assertEquals("1.5", formatDecimal(1.50, maxDecimals = 4))
    }

    @Test
    fun valuesAreRoundedToTheAllowedDecimals() {
        assertEquals("0.33", formatDecimal(1.0 / 3, maxDecimals = 2))
        assertEquals("1", formatDecimal(0.9999, maxDecimals = 2))
    }

    @Test
    fun smallValuesAreNotWrittenWithExponents() {
        assertEquals("0.0001", formatDecimal(0.0001, maxDecimals = 4))
    }

    @Test
    fun negativeValuesKeepTheirSign() {
        assertEquals("-1", formatDecimal(-1.0, maxDecimals = 0))
        assertEquals("0", formatDecimal(-0.0001, maxDecimals = 2))
    }

    @Test
    fun separatorFollowsTheLanguage() {
        assertEquals("1,25", formatDecimal(1.25, maxDecimals = 4, decimalSeparator = ','))
    }

    @Test
    fun commaAndDotAreAcceptedAsSeparator() {
        assertEquals(1.5, parseDecimal("1,5"))
        assertEquals(1.5, parseDecimal(" 1.5 "))
        assertEquals(-1.0, parseDecimal("-1"))
        assertEquals(0.5, parseDecimal(",5"))
    }

    @Test
    fun otherTextIsNoNumber() {
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("1.2.3"))
        assertNull(parseDecimal("1e5"))
        assertNull(parseDecimal("1'000"))
        assertNull(parseDecimal("abc"))
    }

    @Test
    fun typedDecimalPlacesAreCounted() {
        assertEquals(0, decimalPlaces("12"))
        assertEquals(2, decimalPlaces("1,25"))
    }
}
