package com.patrickzedler.grocy.feature.masterdata.impl.products

import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductListTest {

    private val data = ProductListData(
        products = listOf(
            Product(id = 1, name = "Milk", productGroupId = 1, locationId = 1, quIdStock = 1,
                minStockAmount = 2.0),
            Product(id = 2, name = "bread", locationId = 2, quIdStock = 1, minStockAmount = 1.0),
            Product(id = 3, name = "Old cheese", active = false, productGroupId = 1),
            Product(id = 4, name = "Butter", productGroupId = 2, minStockAmount = 0.5),
            Product(id = 5, name = "Juice", parentProductId = null),
        ),
        productGroups = listOf(ProductGroup(1, "Dairy"), ProductGroup(2, "Hidden", active = false)),
        locations = listOf(Location(1, "Fridge"), Location(2, "Pantry")),
        quantityUnits = listOf(QuantityUnit(1, "Piece", namePlural = "Pieces")),
        barcodes = listOf(ProductBarcode(1, productId = 4, barcode = "4006381333931")),
        stock = listOf(
            StockAmount(1, amount = 1.0, amountAggregated = 1.0),
            // A disabled product never counts as in stock
            StockAmount(3, amount = 1.0, amountAggregated = 1.0),
            // A parent with stock only in its sub products is in stock
            StockAmount(5, amount = 0.0, amountAggregated = 2.0),
        ),
    )

    private fun names(
        filter: ProductFilter = ProductFilter(),
        sort: ProductSort = ProductSort(),
        query: String = "",
    ) = buildProductList(data, filter, sort, query, 4, '.', showLocation = true, showStore = true)
        .map { it.name }

    @Test
    fun disabledProductsAreHiddenByDefault() {
        assertEquals(listOf("bread", "Butter", "Juice", "Milk"), names())
    }

    @Test
    fun showDisabledAddsThem() {
        assertEquals(
            listOf("bread", "Butter", "Juice", "Milk", "Old cheese"),
            names(ProductFilter(showDisabled = true)),
        )
    }

    @Test
    fun productGroupFilterKeepsOnlyItsProducts() {
        assertEquals(listOf("Milk"), names(ProductFilter(productGroupId = 1)))
    }

    @Test
    fun inStockCountsSubProductsButNeverDisabledProducts() {
        assertEquals(
            listOf("Juice", "Milk"),
            names(ProductFilter(stockStatus = StockStatusFilter.IN_STOCK, showDisabled = true)),
        )
    }

    @Test
    fun outOfStockIncludesDisabledProducts() {
        assertEquals(
            listOf("bread", "Butter", "Old cheese"),
            names(ProductFilter(stockStatus = StockStatusFilter.OUT_OF_STOCK, showDisabled = true)),
        )
    }

    @Test
    fun searchForgivesTyposAndLooksAtTheGroup() {
        assertEquals(listOf("Milk"), names(query = "mlik"))
        assertEquals(listOf("Milk"), names(query = "dairy"))
    }

    @Test
    fun completeBarcodeFindsItsProduct() {
        assertEquals(listOf("Butter"), names(query = "4006381333931"))
    }

    @Test
    fun sortByMinStockAmountDescending() {
        assertEquals(
            listOf("Milk", "bread", "Butter", "Juice"),
            names(sort = ProductSort(ProductSortField.MIN_STOCK_AMOUNT, ascending = false)),
        )
    }

    @Test
    fun sortByLocationPutsProductsWithoutOneLast() {
        assertEquals(
            listOf("Milk", "bread", "Butter", "Juice"),
            names(sort = ProductSort(ProductSortField.LOCATION)),
        )
        assertEquals(
            listOf("bread", "Milk", "Butter", "Juice"),
            names(sort = ProductSort(ProductSortField.LOCATION, ascending = false)),
        )
    }

    @Test
    fun itemsShowTheColumnsOfGrocysTable() {
        val items = buildProductList(
            data, ProductFilter(), ProductSort(), "", 4, ',', showLocation = true, showStore = true,
        ).associateBy { it.name }

        assertEquals("Fridge", items.getValue("Milk").locationName)
        assertEquals("Dairy", items.getValue("Milk").productGroupName)
        assertEquals("2", items.getValue("Milk").minStockAmount)
        assertEquals("Pieces", items.getValue("Milk").minStockUnit)
        assertEquals("Piece", items.getValue("bread").minStockUnit)
        assertEquals("0,5", items.getValue("Butter").minStockAmount)
        // Inactive groups are not named, like in Grocy's list
        assertNull(items.getValue("Butter").productGroupName)
        assertNull(items.getValue("Juice").minStockAmount)
    }

    @Test
    fun locationIsHiddenWithoutLocationTracking() {
        val item = buildProductList(
            data,
            ProductFilter(),
            ProductSort(),
            query = "",
            decimalPlaces = 4,
            decimalSeparator = '.',
            showLocation = false,
            showStore = true,
        ).first { it.name == "Milk" }

        assertNull(item.locationName)
    }

    @Test
    fun filterIsActiveOnlyWhenSomethingDiffersFromTheDefaults() {
        assertEquals(false, ProductFilter().isActive)
        assertEquals(true, ProductFilter(showDisabled = true).isActive)
    }
}
