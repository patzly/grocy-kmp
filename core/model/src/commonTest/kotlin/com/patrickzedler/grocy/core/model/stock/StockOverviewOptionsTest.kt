package com.patrickzedler.grocy.core.model.stock

import kotlin.test.Test
import kotlin.test.assertEquals

class StockOverviewOptionsTest {

    @Test
    fun nothingStoredMeansGrocysDefaults() {
        val options = StockOverviewOptions.of(null, null, null, null)

        assertEquals(StockOverviewOptions(), options)
        assertEquals(
            setOf(StockOverviewField.AMOUNT, StockOverviewField.NEXT_DUE_DATE),
            options.fields,
        )
        assertEquals(StockOverviewField.NEXT_DUE_DATE, options.sortField)
    }

    @Test
    fun storedOptionsAreReadBack() {
        val options = StockOverviewOptions(
            fields = setOf(StockOverviewField.VALUE, StockOverviewField.PRODUCT_GROUP),
            grouping = StockOverviewGrouping.DEFAULT_LOCATION,
            sortField = StockOverviewField.VALUE,
            ascending = false,
        )

        val stored = StockOverviewOptions.storedFields(options.fields)

        assertEquals("PRODUCT_GROUP,VALUE", stored)
        assertEquals(
            options,
            StockOverviewOptions.of(stored, "DEFAULT_LOCATION", "VALUE", "false"),
        )
    }

    @Test
    fun noFieldsStaysNoFields() {
        val options = StockOverviewOptions.of(
            StockOverviewOptions.storedFields(emptySet()),
            null,
            null,
            null,
        )

        assertEquals(emptySet(), options.fields)
    }

    @Test
    fun unknownValuesOfANewerVersionAreLeftOutOrFallBack() {
        val options = StockOverviewOptions.of("AMOUNT,FUTURE_FIELD", "FUTURE", "FUTURE", "maybe")

        assertEquals(setOf(StockOverviewField.AMOUNT), options.fields)
        assertEquals(StockOverviewGrouping.NONE, options.grouping)
        assertEquals(StockOverviewField.NEXT_DUE_DATE, options.sortField)
        assertEquals(true, options.ascending)
    }
}
