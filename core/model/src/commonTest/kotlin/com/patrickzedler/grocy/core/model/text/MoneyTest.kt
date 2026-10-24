package com.patrickzedler.grocy.core.model.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyTest {

    private val separator = platformDecimalSeparator()

    @Test
    fun pricesKeepTheirDecimalPlaces() {
        val price = formatCurrency(1.5, "EUR", 2)

        assertTrue("1${separator}50" in price, price)
    }

    @Test
    fun unknownCurrenciesShowTheirCode() {
        assertEquals("1${separator}50 XTS", fallbackCurrency(1.5, "XTS", 2))
        assertEquals("2${separator}000 XTS", fallbackCurrency(2.0, "XTS", 3))
        assertEquals("2 XTS", fallbackCurrency(2.4, "XTS", 0))
    }

    @Test
    fun percentsAreWhole() {
        assertTrue("25" in formatPercent(0.25), formatPercent(0.25))
    }
}
