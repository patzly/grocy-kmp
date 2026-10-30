package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ObjectField
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.related.AmountUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShoppingListItemFormTest {

    private val piece = QuantityUnit(1, "Piece")
    private val pack = QuantityUnit(2, "Pack")
    private val milk = Product(id = 1, name = "Milk", quIdStock = 1, quIdPurchase = 2)

    // 1 Pack = 6 Pieces
    private val resolved = listOf(
        ResolvedQuantityUnitConversion(productId = 1, fromQuId = 1, toQuId = 1, 1.0),
        ResolvedQuantityUnitConversion(productId = 1, fromQuId = 1, toQuId = 2, 1.0 / 6),
    )

    private fun values(
        listId: Int? = 1,
        productId: Int? = 1,
        amount: String = "1",
        quId: Int? = 2,
        note: String = "",
    ) = ShoppingListItemValues(listId, productId, amount, quId, note)

    @Test
    fun listAndAmountAreRequired() {
        val errors = values(listId = null, amount = "").validate(decimals = 2)

        assertEquals(
            mapOf(
                ObjectField.SHOPPING_LIST to FieldError.Required,
                ObjectField.AMOUNT to FieldError.Required,
            ),
            errors,
        )
    }

    @Test
    fun zeroIsAnAmountLikeInGrocy() {
        assertTrue(values(amount = "0").validate(decimals = 2).isEmpty())
    }

    @Test
    fun productOrNoteIsRequiredWithOneMessageOnBoth() {
        val errors = values(productId = null, note = " ").validate(decimals = 2)

        assertEquals(FieldError.Required, errors[ObjectField.PRODUCT])
        assertEquals(FieldError.Required, errors[ObjectField.NOTE])
        assertTrue(values(productId = null, note = "Batteries").validate(2).isEmpty())
    }

    @Test
    fun amountIsStoredInTheStockUnit() {
        val packUnit = AmountUnit(pack, factor = 1.0 / 6)

        assertEquals(12.0, values(amount = "2").stockAmount(packUnit), 1e-9)
        assertEquals(2.0, values(amount = "2").stockAmount(null))
    }

    @Test
    fun itemKeepsItsUnitOnlyWithProduct() {
        val packUnit = AmountUnit(pack, factor = 1.0 / 6)

        val milkItem = values(amount = "2", note = " Low fat ").toItem(5, done = true, packUnit)
        val noteItem = values(productId = null, amount = "3", note = "Nails")
            .toItem(0, done = false, packUnit)

        assertEquals(ShoppingListItem(5, 1, 1, "Low fat", 12.0, 2, done = true), milkItem)
        assertEquals(ShoppingListItem(0, 1, null, "Nails", 3.0, null, done = false), noteItem)
    }

    @Test
    fun aChosenProductStartsWithItsPurchaseUnit() {
        val units = itemUnits(milk, resolved, listOf(piece, pack))

        assertEquals(listOf("Pack", "Piece"), units.map { it.unit.name })
        assertEquals(2, defaultItemQuId(milk, units))
        // Without a conversion into the purchase unit the stock unit
        assertEquals(1, defaultItemQuId(milk, units.filter { it.unit.id == 1 }))
        assertNull(defaultItemQuId(null, emptyList()))
    }

    @Test
    fun storedAmountIsShownInTheItemsUnit() {
        val units = itemUnits(milk, resolved, listOf(piece, pack))
        val item = ShoppingListItem(5, 1, 1, null, amount = 9.0, quId = 2, done = false)

        assertEquals("1.5", itemAmountText(item, units, decimals = 2, decimalSeparator = '.'))
        assertEquals(
            "9",
            itemAmountText(item.copy(quId = 3), units, decimals = 2, decimalSeparator = '.'),
        )
    }

    @Test
    fun scannedCodeFindsTheProductByGrocycodeOrBarcode() {
        val barcodes = listOf(ProductBarcode(1, 4, "4006381333931"))

        assertEquals(7, productIdForCode("grcy:p:7", barcodes))
        assertEquals(7, productIdForCode("grcy:p:7:abc", barcodes))
        assertEquals(4, productIdForCode(" 4006381333931 ", barcodes))
        assertNull(productIdForCode("123", barcodes))
    }
}
