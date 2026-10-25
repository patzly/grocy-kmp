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

    @Test
    fun steppingAddsOrSubtractsOne() {
        assertEquals("3", steppedDecimal("2", step = 1))
        assertEquals("1", steppedDecimal("2", step = -1))
    }

    @Test
    fun steppingKeepsTheTypedDecimalsAndSeparator() {
        assertEquals("2,5", steppedDecimal("1,5", step = 1))
        assertEquals("0.25", steppedDecimal("1.25", step = -1, decimalSeparator = ','))
    }

    @Test
    fun steppingAnEmptyFieldStartsLikeGrocy() {
        assertEquals("1", steppedDecimal("", step = 1))
        assertEquals("0", steppedDecimal(" ", step = -1))
    }

    @Test
    fun steppingUsesTheGivenSeparatorWithoutOneInTheText() {
        assertEquals("3", steppedDecimal("2", step = 1, decimalSeparator = ','))
    }

    @Test
    fun steppingStopsAtTheRange() {
        assertNull(steppedDecimal("-1", step = -1, min = -1.0))
        assertNull(steppedDecimal("31", step = 1, max = 31.0))
        // Positive amounts: 1 can not go down to 0
        assertNull(steppedDecimal("1", step = -1, min = Double.MIN_VALUE))
        assertEquals("0.5", steppedDecimal("1.5", step = -1, min = Double.MIN_VALUE))
    }

    @Test
    fun steppingMovesValuesOutsideTheRangeTowardsIt() {
        assertEquals("-4", steppedDecimal("-5", step = 1, min = 0.0))
        assertNull(steppedDecimal("-5", step = -1, min = 0.0))
        assertEquals("40", steppedDecimal("41", step = -1, max = 31.0))
    }

    @Test
    fun steppingTextThatIsNoNumberDoesNothing() {
        assertNull(steppedDecimal("abc", step = 1))
    }
}
