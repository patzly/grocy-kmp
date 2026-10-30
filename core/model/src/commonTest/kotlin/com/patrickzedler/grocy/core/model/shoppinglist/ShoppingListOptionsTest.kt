package com.patrickzedler.grocy.core.model.shoppinglist

import kotlin.test.Test
import kotlin.test.assertEquals

class ShoppingListOptionsTest {

    @Test
    fun nothingStoredMeansGrocysDefaults() {
        val options = ShoppingListOptions.of(null, null)

        assertEquals(
            setOf(ShoppingListField.AMOUNT, ShoppingListField.USERFIELDS),
            options.fields,
        )
        assertEquals(ShoppingListGrouping.PRODUCT_GROUP, options.grouping)
    }

    @Test
    fun storedOptionsAreReadBack() {
        val options = ShoppingListOptions(
            fields = setOf(ShoppingListField.BARCODES, ShoppingListField.LAST_PRICE_UNIT),
            grouping = ShoppingListGrouping.DEFAULT_STORE,
        )

        val stored = ShoppingListOptions.storedFields(options.fields)

        assertEquals("LAST_PRICE_UNIT,BARCODES", stored)
        assertEquals(options, ShoppingListOptions.of(stored, "DEFAULT_STORE"))
    }

    @Test
    fun unknownValuesOfANewerVersionAreLeftOut() {
        val options = ShoppingListOptions.of("AMOUNT,PICTURE", "CALENDAR")

        assertEquals(setOf(ShoppingListField.AMOUNT), options.fields)
        assertEquals(ShoppingListGrouping.PRODUCT_GROUP, options.grouping)
    }

    @Test
    fun noFieldsStaysNoFields() {
        assertEquals(emptySet(), ShoppingListOptions.of("", null).fields)
    }
}
