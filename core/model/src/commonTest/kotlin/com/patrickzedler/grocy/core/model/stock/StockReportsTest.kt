package com.patrickzedler.grocy.core.model.stock

import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals

class StockReportsTest {

    private fun row(
        id: Int,
        type: StockTransactionType,
        amount: Double,
        price: Double? = 2.0,
        stockId: String = "s$id",
        date: String = "2026-11-03",
        productId: Int = 1,
        storeId: Int? = null,
        undone: Boolean = false,
    ) = PriceLogRow(id, productId, stockId, type.value, amount, price, date, storeId, undone)

    @Test
    fun purchasesWithPriceCountUndoneAndFreeOnesNot() {
        val rows = listOf(
            row(1, StockTransactionType.PURCHASE, 3.0),
            row(2, StockTransactionType.PURCHASE, 1.0, undone = true),
            row(3, StockTransactionType.PURCHASE, 1.0, price = null),
            row(4, StockTransactionType.CONSUME, -1.0, stockId = "s1"),
            row(5, StockTransactionType.INVENTORY_CORRECTION, 2.0),
        )

        val purchases = pricedPurchasesOf(rows)

        assertEquals(listOf(3.0, 2.0), purchases.map { it.amount })
    }

    @Test
    fun editedEntryCountsItsNewestEditWithWhatWasConsumedBefore() {
        val rows = listOf(
            row(1, StockTransactionType.PURCHASE, 5.0, stockId = "a", date = "2026-11-01"),
            row(2, StockTransactionType.CONSUME, -2.0, stockId = "a"),
            row(3, StockTransactionType.STOCK_EDIT_NEW, 2.0, price = 4.0, stockId = "a"),
            row(4, StockTransactionType.CONSUME, -1.0, stockId = "a"),
            row(5, StockTransactionType.STOCK_EDIT_NEW, 1.0, price = 3.0, stockId = "a"),
        )

        val purchases = pricedPurchasesOf(rows)

        // The newest edit: its 1 plus the 2 and 1 consumed before, at its price
        assertEquals(
            listOf(PricedPurchase(1, 3.0, 4.0, "2026-11-03", null, "stock-edit-new")),
            purchases,
        )
    }

    @Test
    fun undoneNewestEditLeavesTheEntryOutLikeGrocy() {
        val rows = listOf(
            row(1, StockTransactionType.PURCHASE, 5.0, stockId = "a"),
            row(2, StockTransactionType.STOCK_EDIT_NEW, 5.0, stockId = "a", undone = true),
        )

        assertEquals(emptyList(), pricedPurchasesOf(rows))
    }

    private val products = listOf(
        Product(id = 1, name = "milk", productGroupId = 10, locationId = 100),
        Product(id = 2, name = "Bread", locationId = 100),
        Product(id = 3, name = "Apples", productGroupId = 11, locationId = 101),
        Product(id = 4, name = "Old", active = false, locationId = 100),
    )
    private val groups = listOf(ProductGroup(10, "Dairy"), ProductGroup(11, "Fruit"))
    private val stores = listOf(Store(20, "Market"))
    private val purchases = listOf(
        PricedPurchase(1, 1.5, 2.0, "2026-11-02", 20, "purchase"),
        PricedPurchase(1, 1.0, 1.0, "2026-11-05", null, "purchase"),
        PricedPurchase(2, 3.0, 1.0, "2026-11-04", 20, "purchase"),
        PricedPurchase(3, 2.0, 1.0, "2026-10-31", 20, "purchase"),
        PricedPurchase(2, 9.0, 1.0, "2026-11-04", 20, "self-production"),
        PricedPurchase(99, 9.0, 1.0, "2026-11-04", 20, "purchase"),
    )

    private fun spendings(
        grouping: SpendingsGrouping,
        filter: ProductGroupFilter = ProductGroupFilter.All,
    ) = spendingsOf(
        purchases, "2026-11-01", "2026-11-30", grouping, filter, products, groups, stores,
    )

    @Test
    fun spendingsByProductInTheRangeOrderedByName() {
        assertEquals(
            listOf(
                SpendingsRow(2, "Bread", 3.0),
                SpendingsRow(1, "milk", 4.0, productGroup = "Dairy"),
            ),
            spendings(SpendingsGrouping.PRODUCT),
        )
    }

    @Test
    fun productGroupFilterChoosesTheProducts() {
        assertEquals(
            listOf("Bread"),
            spendings(SpendingsGrouping.PRODUCT, ProductGroupFilter.Ungrouped).map { it.name },
        )
        assertEquals(
            listOf("milk"),
            spendings(SpendingsGrouping.PRODUCT, ProductGroupFilter.Group(10)).map { it.name },
        )
    }

    @Test
    fun spendingsByGroupAndStoreStartWithTheOnesWithout() {
        assertEquals(
            listOf(SpendingsRow(null, null, 3.0), SpendingsRow(10, "Dairy", 4.0)),
            spendings(SpendingsGrouping.PRODUCT_GROUP),
        )
        assertEquals(
            listOf(SpendingsRow(null, null, 1.0), SpendingsRow(20, "Market", 6.0)),
            spendings(SpendingsGrouping.STORE),
        )
    }

    @Test
    fun rangesFollowGrocysPicker() {
        val today = epochDayOf("2026-03-15")!!

        fun dates(preset: SpendingsRangePreset) =
            SpendingsRange.of(preset, today).let { it.from to it.to }

        assertEquals("2026-03-01" to "2026-03-31", dates(SpendingsRangePreset.THIS_MONTH))
        assertEquals("2026-02-01" to "2026-02-28", dates(SpendingsRangePreset.LAST_MONTH))
        assertEquals("2026-03-09" to "2026-03-15", dates(SpendingsRangePreset.LAST_7_DAYS))
        assertEquals("2026-03-14" to "2026-03-14", dates(SpendingsRangePreset.YESTERDAY))
        assertEquals("2025-01-01" to "2025-12-31", dates(SpendingsRangePreset.LAST_YEAR))
        assertEquals(
            "2026-01-02" to "2026-01-05",
            SpendingsRange.custom(epochDayOf("2026-01-05")!!, epochDayOf("2026-01-02")!!)
                .let { it.from to it.to },
        )
    }

    private val locations = listOf(Location(101, "pantry"), Location(100, "Fridge"))

    private fun entry(
        id: Int,
        productId: Int,
        location: Int,
        amount: Double,
        open: Boolean = false,
    ) = StockEntry(
        id = id,
        productId = productId,
        amount = amount,
        bestBeforeDate = null,
        purchasedDate = null,
        stockId = "e$id",
        open = open,
        locationId = location,
    )

    @Test
    fun locationSheetListsWhatIsWhereWithTheOpenedPart() {
        val entries = listOf(
            entry(1, 1, 100, 2.0),
            entry(2, 1, 100, 1.0, open = true),
            entry(3, 1, 101, 1.0),
            entry(4, 4, 100, 1.0),
        )

        val sheet = locationContentOf(entries, products, locations, includeOutOfStock = false)

        assertEquals(listOf("Fridge", "pantry"), sheet.map { it.location.name })
        assertEquals(
            listOf(LocationContentItem(products[0], 3.0, 1.0)),
            sheet[0].items,
        )
    }

    @Test
    fun outOfStockProductsShowAtTheirDefaultLocation() {
        val entries = listOf(entry(1, 1, 101, 2.0))

        val sheet = locationContentOf(entries, products, locations, includeOutOfStock = true)

        assertEquals(listOf("Bread"), sheet[0].items.map { it.product.name })
        assertEquals(listOf("Apples", "milk"), sheet[1].items.map { it.product.name })
        assertEquals(0.0, sheet[0].items.single().amount)
    }
}
