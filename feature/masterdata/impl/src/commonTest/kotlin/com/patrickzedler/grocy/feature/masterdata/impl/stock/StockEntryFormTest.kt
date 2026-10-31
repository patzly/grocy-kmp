package com.patrickzedler.grocy.feature.masterdata.impl.stock

import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.StockEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StockEntryFormTest {

    private val entry = StockEntry(
        id = 1,
        productId = 2,
        amount = 1.5,
        bestBeforeDate = "2026-11-01",
        purchasedDate = "2026-10-01",
        stockId = "abc",
        price = 0.5,
        locationId = 3,
        storeId = 4,
    )

    private fun values() = entry.toValues(decimals = 2, priceDecimals = 2, decimalSeparator = '.')

    @Test
    fun entryIsEditedAsStored() {
        assertEquals(
            StockEntryValues("1.5", "2026-10-01", "2026-11-01", false, "0.5", 4, 3, "", false),
            values(),
        )
    }

    @Test
    fun neverOverdueShowsNoDate() {
        val values = entry.copy(bestBeforeDate = StockEntry.NEVER_OVERDUE)
            .toValues(2, 2, '.')

        assertTrue(values.neverOverdue)
        assertEquals("", values.dueDate)
    }

    @Test
    fun amountDatesAndLocationAreRequired() {
        val errors = values()
            .copy(amount = "", purchasedDate = "", dueDate = "2026-13-01", locationId = null)
            .validate(ServerConfig(), decimals = 2, priceDecimals = 2)

        assertEquals(
            mapOf(
                StockEntryField.AMOUNT to FieldError.Required,
                StockEntryField.PURCHASED_DATE to FieldError.Required,
                StockEntryField.DUE_DATE to FieldError.InvalidDate,
                StockEntryField.LOCATION to FieldError.Required,
            ),
            errors,
        )
    }

    @Test
    fun neverOverdueNeedsNoDueDate() {
        val errors = values().copy(dueDate = "", neverOverdue = true)
            .validate(ServerConfig(), decimals = 2, priceDecimals = 2)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun priceIsOptionalButNotNegative() {
        val config = ServerConfig()

        assertTrue(values().copy(price = "").validate(config, 2, 2).isEmpty())
        assertEquals(
            FieldError.InvalidNumber("0", 2),
            values().copy(price = "-1").validate(config, 2, 2)[StockEntryField.PRICE],
        )
    }

    @Test
    fun savedEntryTakesTheValues() {
        val saved = values()
            .copy(amount = "1", dueDate = "", neverOverdue = true, price = "", note = " Half ")
            .toEntry(entry, ServerConfig())

        assertEquals(1.0, saved.amount)
        assertEquals(StockEntry.NEVER_OVERDUE, saved.bestBeforeDate)
        assertEquals(null, saved.price)
        assertEquals("Half", saved.note)
        assertEquals("abc", saved.stockId)
    }

    @Test
    fun hiddenFieldsKeepTheirValues() {
        val config = ServerConfig(
            stockBestBeforeDateTracking = false,
            stockPriceTracking = false,
            stockLocationTracking = false,
        )

        val saved = values().copy(dueDate = "", price = "", storeId = null, locationId = null)
            .toEntry(entry, config)

        assertEquals("2026-11-01", saved.bestBeforeDate)
        assertEquals(0.5, saved.price)
        assertEquals(4, saved.storeId)
        assertEquals(3, saved.locationId)
    }
}
