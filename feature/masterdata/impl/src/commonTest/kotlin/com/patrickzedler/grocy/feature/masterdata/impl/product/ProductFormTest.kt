package com.patrickzedler.grocy.feature.masterdata.impl.product

import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.server.ProductPresets
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductFormTest {

    private val allFeatures = ProductFormFeatures.of(ServerConfig(labelPrinter = true))
    private val valid = ProductFormValues(
        name = "Milk",
        locationId = 1,
        quIdStock = 1,
        quIdPurchase = 1,
        quIdConsume = 1,
        quIdPrice = 1,
    )

    @Test
    fun validValuesHaveNoErrors() {
        assertEquals(emptyMap(), valid.validate(allFeatures, decimals = 4))
    }

    @Test
    fun nameLocationAndUnitsAreRequired() {
        val errors = ProductFormValues(name = " ").validate(allFeatures, decimals = 4)

        assertEquals(
            setOf(
                ProductField.NAME, ProductField.LOCATION, ProductField.QU_STOCK,
                ProductField.QU_PURCHASE, ProductField.QU_CONSUME, ProductField.QU_PRICE,
            ),
            errors.keys,
        )
        assertTrue(errors.values.all { it == FieldError.Required })
    }

    @Test
    fun locationIsOptionalWithoutLocationTracking() {
        val features = ProductFormFeatures.of(ServerConfig(stockLocationTracking = false))

        val errors = valid.copy(locationId = null).validate(features, decimals = 4)

        assertTrue(ProductField.LOCATION !in errors)
    }

    @Test
    fun numbersFollowGrocysRangesAndDecimalPlaces() {
        val errors = valid.copy(
            minStockAmount = "1,555",
            defaultDueDays = "-2",
            defaultDueDaysAfterOpen = "1.5",
            quickConsumeAmount = "0",
            calories = "abc",
        ).validate(allFeatures, decimals = 2)

        assertEquals(FieldError.InvalidNumber("0", 2), errors[ProductField.MIN_STOCK_AMOUNT])
        assertEquals(FieldError.InvalidNumber("-1", 0), errors[ProductField.DUE_DAYS])
        assertEquals(FieldError.InvalidNumber("0", 0), errors[ProductField.DUE_DAYS_AFTER_OPEN])
        assertEquals(
            FieldError.InvalidNumber("0.01", 2),
            errors[ProductField.QUICK_CONSUME_AMOUNT],
        )
        assertEquals(FieldError.InvalidNumber("0", 2), errors[ProductField.CALORIES])
    }

    @Test
    fun neverOverdueIsAllowed() {
        val errors = valid.copy(defaultDueDays = "-1", defaultDueDaysAfterFreezing = "-1")
            .validate(allFeatures, decimals = 4)

        assertEquals(emptyMap(), errors)
    }

    @Test
    fun emptyEnergyIsAllowed() {
        assertTrue(ProductField.CALORIES !in valid.copy(calories = "").validate(allFeatures, 4))
    }

    @Test
    fun tareWeightIsCheckedOnlyWithTareHandling() {
        val values = valid.copy(tareWeight = "")

        assertTrue(ProductField.TARE_WEIGHT !in values.validate(allFeatures, 4))
        assertTrue(
            ProductField.TARE_WEIGHT in
                values.copy(enableTareWeightHandling = true).validate(allFeatures, 4),
        )
    }

    @Test
    fun minStockAmountOfACumulatingParentIsNotChecked() {
        val errors = valid.copy(minStockAmount = "x")
            .validate(allFeatures, decimals = 4, minStockEditable = false)

        assertTrue(ProductField.MIN_STOCK_AMOUNT !in errors)
    }

    @Test
    fun productTakesTheTypedValues() {
        val product = valid.copy(
            name = " Milk ",
            minStockAmount = "1,5",
            dueType = DueType.EXPIRATION,
            defaultDueDays = "7",
            calories = "",
        ).toProduct(id = 0, original = null, originalDescriptionText = "", allFeatures)

        assertEquals("Milk", product.name)
        assertEquals(1.5, product.minStockAmount)
        assertEquals(DueType.EXPIRATION, product.dueType)
        assertEquals(7, product.defaultDueDays)
        assertNull(product.calories)
    }

    @Test
    fun hiddenFeaturesKeepTheOriginalValues() {
        val original = Product(
            id = 3,
            name = "Milk",
            storeId = 5,
            defaultDueDaysAfterFreezing = 30,
            defaultStockLabelType = StockLabelType.SINGLE,
        )
        val features = ProductFormFeatures.of(
            ServerConfig(stockPriceTracking = false, stockProductFreezing = false),
        )

        val product = valid.copy(storeId = null, defaultDueDaysAfterFreezing = "0")
            .toProduct(id = 3, original, originalDescriptionText = "", features)

        assertEquals(5, product.storeId)
        assertEquals(30, product.defaultDueDaysAfterFreezing)
        assertEquals(StockLabelType.SINGLE, product.defaultStockLabelType)
    }

    @Test
    fun unchangedDescriptionKeepsItsHtml() {
        val original = Product(id = 3, name = "Milk", description = "<p><b>Fresh</b></p>")
        val values = original.toFormValues(decimals = 4, decimalSeparator = '.')

        val product = values.toProduct(3, original, values.description, allFeatures)

        assertEquals("<p><b>Fresh</b></p>", product.description)
    }

    @Test
    fun changedDescriptionBecomesHtmlParagraphs() {
        val product = valid.copy(description = "Line 1\nLine 2\n\nA & B")
            .toProduct(id = 0, original = null, originalDescriptionText = "", allFeatures)

        assertEquals("<p>Line 1<br>Line 2</p><p>A &amp; B</p>", product.description)
    }

    @Test
    fun htmlIsShownAsPlainText() {
        assertEquals(
            "Fresh\nmilk\n\n• one\n\n• two\n\n1 < 2 & 3",
            htmlToPlainText(
                "<p>Fresh<br>milk</p><ul><li>one</li><li>two</li></ul>1 &lt; 2 &amp; 3",
            ),
        )
    }

    @Test
    fun existingProductIsShownInTheUsersNotation() {
        val values = Product(id = 1, name = "Milk", minStockAmount = 2.5, calories = null)
            .toFormValues(decimals = 4, decimalSeparator = ',')

        assertEquals("2,5", values.minStockAmount)
        assertEquals("", values.calories)
    }

    @Test
    fun newProductsGetThePresets() {
        val values = newProductValues(
            ProductPresets(
                locationId = 2,
                productGroupId = 3,
                quantityUnitId = 4,
                defaultDueDays = 5,
                treatOpenedAsOutOfStock = false,
                defaultStockLabelType = 2,
            ),
            allFeatures,
        )

        assertEquals(2, values.locationId)
        assertEquals(3, values.productGroupId)
        assertEquals(listOf(4, 4, 4, 4), listOf(values.quIdStock, values.quIdPurchase,
            values.quIdConsume, values.quIdPrice))
        assertEquals("5", values.defaultDueDays)
        assertEquals(false, values.treatOpenedAsOutOfStock)
        assertEquals(StockLabelType.PER_UNIT, values.defaultStockLabelType)
    }

    @Test
    fun otherUnitsFollowTheStockUnitUnlessChosen() {
        assertEquals(2, linkedQuantityUnit(current = null, previousStock = null, newStock = 2))
        assertEquals(2, linkedQuantityUnit(current = 1, previousStock = 1, newStock = 2))
        assertEquals(3, linkedQuantityUnit(current = 3, previousStock = 1, newStock = 2))
    }

    @Test
    fun formStateLinksTheUnitsLikeGrocy() {
        val form = ProductFormState(ProductFormValues(quIdStock = 1, quIdPurchase = 1,
            quIdConsume = 3, quIdPrice = null))

        form.changeStockUnit(2)

        assertEquals(2, form.quIdPurchase)
        assertEquals(3, form.quIdConsume)
        assertEquals(2, form.quIdPrice)
    }

    @Test
    fun smallestQuickAmountDependsOnTheDecimals() {
        assertEquals(1.0, minimumAmount(0))
        assertEquals(0.01, minimumAmount(2))
    }
}
