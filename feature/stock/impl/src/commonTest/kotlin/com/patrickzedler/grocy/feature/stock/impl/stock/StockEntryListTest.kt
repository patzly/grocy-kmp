package com.patrickzedler.grocy.feature.stock.impl.stock

import com.patrickzedler.grocy.core.forms.EntryPrice
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.epochDayOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StockEntryListTest {

    private val today = epochDayOf("2026-10-25")!!
    private val milk = Product(
        id = 1,
        name = "Milk",
        quIdStock = 1,
        quIdPrice = 2,
        dueType = DueType.EXPIRATION,
    )
    private val bread = Product(id = 2, name = "Bread", quIdStock = 1, quIdPrice = 1)
    private val old = Product(id = 3, name = "Old", active = false, quIdStock = 1)
    private val units = listOf(QuantityUnit(1, "Piece", "Pieces"), QuantityUnit(2, "Pack"))
    private val resolved = listOf(ResolvedQuantityUnitConversion(1, 2, 1, 6.0))

    private fun entry(
        id: Int,
        productId: Int = 1,
        due: String? = "2026-11-30",
        purchased: String = "2026-10-01",
        locationId: Int = 1,
    ) = StockEntry(
        id = id,
        productId = productId,
        amount = 2.0,
        bestBeforeDate = due,
        purchasedDate = purchased,
        stockId = "s$id",
        locationId = locationId,
    )

    private fun build(
        entries: List<StockEntry>,
        filter: StockEntryFilter = StockEntryFilter(),
        config: ServerConfig = ServerConfig(),
    ) = buildStockEntryItems(
        entries = entries,
        products = listOf(milk, bread, old),
        units = units,
        locations = listOf(Location(1, "Fridge"), Location(2, "Pantry")),
        stores = listOf(Store(1, "Supermarket")),
        resolved = resolved,
        config = config,
        settings = UserSettings(stockDueSoonDays = 5),
        filter = filter,
        today = today,
        decimalSeparator = '.',
        language = "en",
    )

    @Test
    fun entriesOfActiveProductsAreSortedByProductAndConsumeOrder() {
        val items = build(
            listOf(
                entry(1, due = "2026-12-01"),
                entry(2, productId = 2),
                entry(3, due = "2026-11-01"),
                entry(4, productId = 3),
            ),
        )

        assertEquals(listOf(2, 3, 1), items.map { it.entry.id })
        assertEquals("2 Pieces", items.first().amount)
    }

    @Test
    fun filtersKeepTheChosenProductAndLocation() {
        val entries = listOf(entry(1), entry(2, productId = 2), entry(3, locationId = 2))

        val items = build(entries, StockEntryFilter(productId = 1, locationId = 2))

        assertEquals(listOf(3), items.map { it.entry.id })
    }

    @Test
    fun neverOverdueEntriesAreNeverHighlighted() {
        val item = build(listOf(entry(1, due = StockEntry.NEVER_OVERDUE))).single()

        assertTrue(item.neverOverdue)
        assertNull(item.dueStatus)
    }

    @Test
    fun priceIsShownPerPriceUnit() {
        val items = build(listOf(entry(1).copy(price = 0.5, storeId = 1)))

        assertEquals(EntryPrice(3.0, "Pack"), items.single().price)
        assertEquals("Supermarket", items.single().storeName)
    }

    @Test
    fun hiddenFeaturesShowNothingOfThem() {
        val config = ServerConfig(
            stockBestBeforeDateTracking = false,
            stockPriceTracking = false,
            stockLocationTracking = false,
            stockProductOpenedTracking = false,
        )

        val item = build(listOf(entry(1).copy(price = 1.0, storeId = 1)), config = config).single()

        assertNull(item.dueDay)
        assertNull(item.price)
        assertNull(item.storeName)
        assertNull(item.locationName)
        assertFalse(item.canOpen)
    }

    @Test
    fun openedEntriesAndUnopenableProductsCannotBeOpened() {
        val items = build(listOf(entry(1).copy(open = true), entry(2)))

        assertFalse(items.first { it.entry.id == 1 }.canOpen)
        assertTrue(items.first { it.entry.id == 2 }.canOpen)
    }

    @Test
    fun priceFactorIsOneWithoutConversion() {
        assertEquals(1.0, priceToStockFactor(milk.copy(quIdPrice = 3), resolved))
        assertEquals(6.0, priceToStockFactor(milk, resolved))
    }
}
