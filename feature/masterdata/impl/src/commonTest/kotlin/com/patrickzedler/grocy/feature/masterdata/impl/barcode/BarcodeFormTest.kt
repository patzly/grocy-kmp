package com.patrickzedler.grocy.feature.masterdata.impl.barcode

import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarcodeFormTest {

    @Test
    fun barcodeIsRequiredAndTheAmountOptional() {
        val errors = BarcodeFormValues(barcode = " ").validate(decimals = 2)

        assertEquals(mapOf(BarcodeField.BARCODE to FieldError.Required), errors)
    }

    @Test
    fun amountMustBePositive() {
        val errors = BarcodeFormValues(barcode = "123", amount = "0").validate(decimals = 2)

        assertEquals(FieldError.InvalidNumber("0.01", 2), errors[BarcodeField.AMOUNT])
    }

    @Test
    fun savedBarcodeKeepsItsLastPrice() {
        val original = ProductBarcode(4, 1, "old", storeId = 2, lastPrice = 1.5)
        val values = BarcodeFormValues(" 123 ", "6", quId = 2, storeId = 1, note = " ")

        val barcode = values.toBarcode(4, productId = 1, original, showsStore = true)

        assertEquals(ProductBarcode(4, 1, "123", 2, 6.0, 1, 1.5, null), barcode)
    }

    @Test
    fun withoutPriceTrackingTheStoreStays() {
        val original = ProductBarcode(4, 1, "old", storeId = 2)

        val barcode = BarcodeFormValues("123").toBarcode(4, 1, original, showsStore = false)

        assertEquals(2, barcode.storeId)
    }

    @Test
    fun barcodeIsEditedAsTyped() {
        val values = ProductBarcode(4, 1, "123", 2, 1.5, 1, note = "Six")
            .toFormValues(decimals = 2, decimalSeparator = ',')

        assertEquals(BarcodeFormValues("123", "1,5", 2, 1, "Six"), values)
    }

    @Test
    fun itemsShowGrocysColumnsOfTheProductByBarcode() {
        val barcodes = listOf(
            ProductBarcode(1, 1, "999", quId = 2, amount = 6.0, storeId = 1, lastPrice = 3.0),
            ProductBarcode(2, 1, "111"),
            ProductBarcode(3, 2, "555"),
        )

        val items = buildBarcodeItems(
            productId = 1,
            barcodes = barcodes,
            units = listOf(QuantityUnit(2, "Pack", "Packs")),
            stores = listOf(Store(1, "Supermarket")),
            config = ServerConfig(currency = "EUR"),
            settings = UserSettings(),
            decimalSeparator = '.',
            language = "en",
            formatPrice = { value, currency, decimals -> "$value $currency $decimals" },
        )

        assertEquals(listOf("111", "999"), items.map { it.barcode })
        assertEquals("6 Packs", items[1].amount)
        assertEquals("Supermarket", items[1].storeName)
        assertEquals("3.0 EUR 2", items[1].lastPrice)
        assertNull(items[0].amount)
    }

    @Test
    fun withoutPriceTrackingNoStoreOrPriceIsShown() {
        val items = buildBarcodeItems(
            productId = 1,
            barcodes = listOf(ProductBarcode(1, 1, "1", storeId = 1, lastPrice = 3.0)),
            units = emptyList(),
            stores = listOf(Store(1, "Supermarket")),
            config = ServerConfig(stockPriceTracking = false),
            settings = UserSettings(),
            decimalSeparator = '.',
            language = "en",
        )

        assertTrue(items.single().storeName == null && items.single().lastPrice == null)
    }
}
