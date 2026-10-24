package com.patrickzedler.grocy.feature.masterdata.impl.overview

import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.PriceHistoryEntry
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductOverviewStateTest {

    private val milk = Product(
        id = 1,
        name = "Milk",
        description = "<p>Fresh &amp; cold</p>",
        locationId = 1,
        quIdStock = 1,
        quIdPrice = 2,
        pictureFileName = "milk.jpg",
    )
    private val units = listOf(
        QuantityUnit(1, "Piece", namePlural = "Pieces"),
        QuantityUnit(2, "Pack", namePlural = "Packs"),
    )
    private val locations = listOf(Location(1, "Fridge"))
    private val stores = listOf(Store(1, "Supermarket"), Store(2, "Bakery"))

    private fun build(
        product: Product = milk,
        overview: ProductOverview? = ProductOverview(productId = 1),
        config: ServerConfig = ServerConfig(),
    ) = buildProductOverview(
        product = product,
        overview = overview,
        quantityUnits = units,
        locations = locations,
        stores = stores,
        config = config,
        settings = UserSettings(stockDecimalPlacesAmounts = 2),
        decimalSeparator = ',',
    )

    @Test
    fun storedProductShowsBeforeTheFirstDownload() {
        val state = build(overview = null)

        assertEquals("Milk", state.name)
        assertEquals("Fresh & cold", state.description)
        assertEquals("milk.jpg", state.pictureFileName)
        assertEquals("Fridge", state.locationName)
        assertNull(state.details)
    }

    @Test
    fun amountsUseTheStockUnitAndItsPlural() {
        val details = build(
            overview = ProductOverview(
                productId = 1,
                stockAmount = 2.5,
                stockAmountOpened = 1.0,
                stockAmountAggregated = 1.0,
                isAggregatedAmount = true,
            ),
        ).details!!

        assertEquals(AmountText("2,5 Pieces", opened = "1"), details.stockAmount)
        assertEquals(AmountText("1 Piece", opened = null), details.aggregatedAmount)
    }

    @Test
    fun withoutOwnStockOnlyTheSubProductsCount() {
        val details = build(
            product = milk.copy(noOwnStock = true),
            overview = ProductOverview(productId = 1, isAggregatedAmount = true),
        ).details!!

        assertNull(details.stockAmount)
        assertEquals("0 Pieces", details.aggregatedAmount?.amount)
    }

    @Test
    fun pricesArePerPriceUnitAndPerStockUnitWhenTheyDiffer() {
        val details = build(
            overview = ProductOverview(productId = 1, lastPrice = 0.5, priceToStockFactor = 6.0),
        ).details!!

        assertEquals(UnitPrice(3.0, "Pack", pricePerStockUnit = 0.5, "Piece"), details.lastPrice)
        assertNull(details.averagePrice)
    }

    @Test
    fun samePriceAndStockUnitShowsOnePrice() {
        val details = build(
            product = milk.copy(quIdPrice = 1),
            overview = ProductOverview(productId = 1, averagePrice = 2.0),
        ).details!!

        assertEquals(UnitPrice(2.0, "Piece", null, null), details.averagePrice)
    }

    @Test
    fun shelfLifeIsUnknownUnlimitedOrDaysLikeGrocy() {
        fun shelfLife(days: Double) =
            build(overview = ProductOverview(productId = 1, averageShelfLifeDays = days))
                .details!!.averageShelfLife

        assertEquals(ShelfLife.Unknown, shelfLife(-1.0))
        assertEquals(ShelfLife.Unlimited, shelfLife(80_000.0))
        assertEquals(ShelfLife.Days(12.0), shelfLife(12.0))
    }

    @Test
    fun datesAndSpoilRateAreReadyToFormat() {
        val details = build(
            overview = ProductOverview(
                productId = 1,
                lastPurchased = "1970-01-11",
                spoilRatePercent = 12.5,
            ),
        ).details!!

        assertEquals(10, details.lastPurchased)
        assertNull(details.lastUsed)
        assertEquals(0.125, details.spoilRate)
    }

    @Test
    fun disabledFeaturesAreHiddenLikeInGrocy() {
        val state = build(
            config = ServerConfig(
                shoppingList = false,
                stockPriceTracking = false,
                stockLocationTracking = false,
                stockBestBeforeDateTracking = false,
                currency = "CHF",
            ),
        )

        assertFalse(state.showShoppingList)
        assertFalse(state.showPrices)
        assertFalse(state.showLocation)
        assertFalse(state.showShelfLife)
        assertEquals("CHF", state.currency)
    }

    @Test
    fun priceHistoryHasOneSeriesPerStoreOldestFirst() {
        val history = buildPriceHistory(
            entries = listOf(
                PriceHistoryEntry("2026-10-03", 1.0, storeId = 2),
                PriceHistoryEntry("2026-10-02", 2.0),
                PriceHistoryEntry("2026-10-01", 1.5, storeId = 1),
                PriceHistoryEntry("2026-09-01", 1.0, storeId = 1),
            ),
            stores = stores,
            priceToStockFactor = 2.0,
        )

        assertEquals(
            listOf(
                PriceSeries(
                    SeriesStore.Named("Supermarket"),
                    colorIndex = 0,
                    points = listOf(PricePoint(20_697, 2.0), PricePoint(20_727, 3.0)),
                ),
                PriceSeries(
                    SeriesStore.Named("Bakery"),
                    colorIndex = 1,
                    points = listOf(PricePoint(20_729, 2.0)),
                ),
                PriceSeries(SeriesStore.Unknown, 2, listOf(PricePoint(20_728, 4.0))),
            ),
            history.series,
        )
    }

    @Test
    fun storesBeyondTheColorsAreDrawnTogether() {
        val manyStores = (1..10).map { Store(it, "Store $it") }
        val history = buildPriceHistory(
            entries = manyStores.map { PriceHistoryEntry("2026-10-01", 1.0, storeId = it.id) },
            stores = manyStores,
            priceToStockFactor = 1.0,
        )

        assertEquals(MAX_PRICE_SERIES, history.series.size)
        assertEquals(SeriesStore.Named("Store 7"), history.series[6].store)
        assertEquals(SeriesStore.Others, history.series.last().store)
        assertEquals(3, history.series.last().points.size)
    }

    @Test
    fun trendIsTheLeastSquaresLine() {
        val history = buildPriceHistory(
            entries = listOf(
                PriceHistoryEntry("1970-01-01", 1.0),
                PriceHistoryEntry("1970-01-02", 3.0),
                PriceHistoryEntry("1970-01-03", 2.0),
            ),
            stores = emptyList(),
            priceToStockFactor = 1.0,
        )

        assertEquals(PriceTrend(0, 1.5, 2, 2.5), history.trend)
    }

    @Test
    fun singleDayHasNoTrend() {
        val history = buildPriceHistory(
            entries = listOf(PriceHistoryEntry("2026-10-01", 1.0)),
            stores = emptyList(),
            priceToStockFactor = 1.0,
        )

        assertNull(history.trend)
        assertFalse(history.isEmpty)
        assertTrue(buildPriceHistory(emptyList(), stores, 1.0).isEmpty)
    }
}
