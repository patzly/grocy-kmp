package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShoppingListItemFormTest {

    @Test
    fun listAndAmountAreRequired() {
        val errors = ShoppingListItemValues(null, "", null, "").validate(decimals = 2)

        assertEquals(
            mapOf(
                ShoppingListItemField.LIST to FieldError.Required,
                ShoppingListItemField.AMOUNT to FieldError.Required,
            ),
            errors,
        )
    }

    @Test
    fun zeroIsAnAmountLikeInGrocy() {
        assertTrue(ShoppingListItemValues(1, "0", null, "").validate(decimals = 2).isEmpty())
    }

    @Test
    fun amountIsStoredInTheStockUnit() {
        val pack = AmountUnit(QuantityUnit(2, "Pack"), factor = 1.0 / 6)

        assertEquals(12.0, ShoppingListItemValues(1, "2", 2, "").stockAmount(pack), 1e-9)
        assertEquals(2.0, ShoppingListItemValues(1, "2", null, "").stockAmount(null))
    }
}
