package com.patrickzedler.grocy.feature.masterdata.impl.overview

import com.patrickzedler.grocy.core.design.component.CHART_COLOR_COUNT
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.PriceHistoryEntry
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.feature.masterdata.impl.product.htmlToPlainText
import com.patrickzedler.grocy.core.model.masterdata.nameFor

/**
 * Grocy's "Product overview" of one product: what the stored product says at once, what the
 * server computes ([details]) once it was downloaded. Fields of features the server turned off
 * are hidden like in its web frontend.
 */
internal data class ProductOverviewUiState(
    val productId: Int,
    val name: String,
    /** Plain text, empty without description. */
    val description: String,
    val pictureFileName: String?,
    val locationName: String?,
    val showShoppingList: Boolean,
    val showPrices: Boolean,
    val showLocation: Boolean,
    val showShelfLife: Boolean,
    /** ISO 4217 code of the server's prices. */
    val currency: String,
    val priceDecimals: Int,
    /** Null until the overview was downloaded once. */
    val details: OverviewDetails?,
)

internal data class OverviewDetails(
    /** Null for products without own stock, which only sum up their sub products. */
    val stockAmount: AmountText?,
    /** Including sub products, only for parents whose sub products are in stock. */
    val aggregatedAmount: AmountText?,
    val stockValue: Double,
    /** Days since 1970-01-01, null if never. */
    val lastPurchased: Long?,
    /** Days since 1970-01-01, null if never. */
    val lastUsed: Long?,
    val lastPrice: UnitPrice?,
    val averagePrice: UnitPrice?,
    val averageShelfLife: ShelfLife,
    /** 0.1 for 10 %. */
    val spoilRate: Double,
    val priceHistory: PriceHistory,
)

/** An amount with its unit, e.g. "3 Pieces", and the opened part, e.g. "1". */
internal data class AmountText(val amount: String, val opened: String?)

/**
 * A price per [unit], the product's unit for prices, and per [stockUnit] when that differs, like
 * the tooltip of Grocy's prices.
 */
internal data class UnitPrice(
    val price: Double,
    val unit: String,
    val pricePerStockUnit: Double?,
    val stockUnit: String?,
)

internal sealed interface ShelfLife {
    data object Unknown : ShelfLife

    data object Unlimited : ShelfLife

    data class Days(val days: Double) : ShelfLife
}

/** Purchase prices per price unit, one series per store, oldest first. */
internal data class PriceHistory(
    val series: List<PriceSeries>,
    /** Grocy's dotted linear trend over all prices, null below two days with prices. */
    val trend: PriceTrend?,
) {
    val isEmpty: Boolean
        get() = series.isEmpty()
}

/**
 * The prices of one [store]; [colorIndex] follows the store (ordered by id), so a store keeps
 * its color in every product.
 */
internal data class PriceSeries(
    val store: SeriesStore,
    val colorIndex: Int,
    val points: List<PricePoint>,
)

internal sealed interface SeriesStore {
    data class Named(val name: String) : SeriesStore

    /** Purchases without store, Grocy's "Unknown store". */
    data object Unknown : SeriesStore

    /** Stores beyond the chart's colors together. */
    data object Others : SeriesStore
}

internal data class PricePoint(val epochDay: Long, val price: Double)

internal data class PriceTrend(
    val startDay: Long,
    val startPrice: Double,
    val endDay: Long,
    val endPrice: Double,
)

/** Distinct colors of the chart, more stores are drawn together as [SeriesStore.Others]. */
internal const val MAX_PRICE_SERIES = CHART_COLOR_COUNT

internal fun buildProductOverview(
    product: Product,
    overview: ProductOverview?,
    quantityUnits: List<QuantityUnit>,
    locations: List<Location>,
    stores: List<Store>,
    config: ServerConfig,
    settings: UserSettings,
    decimalSeparator: Char,
    language: String,
): ProductOverviewUiState {
    val units = quantityUnits.associateBy { it.id }
    val stockUnit = product.quIdStock?.let { units[it] }
    val priceUnit = product.quIdPrice?.let { units[it] }
    val decimals = settings.stockDecimalPlacesAmounts
    fun amount(value: Double, opened: Double) = AmountText(
        amount = listOfNotNull(
            formatDecimal(value, decimals, decimalSeparator),
            stockUnit?.nameFor(value, language),
        ).joinToString(" "),
        opened = if (opened > 0) formatDecimal(opened, decimals, decimalSeparator) else null,
    )
    fun price(perStockUnit: Double?, factor: Double) = perStockUnit?.let {
        val differs = stockUnit != null && priceUnit != null && stockUnit.id != priceUnit.id
        UnitPrice(
            price = it * factor,
            unit = priceUnit?.name ?: stockUnit?.name.orEmpty(),
            pricePerStockUnit = if (differs) it else null,
            stockUnit = if (differs) stockUnit.name else null,
        )
    }

    val details = overview?.let {
        OverviewDetails(
            stockAmount = if (product.noOwnStock) {
                null
            } else {
                amount(it.stockAmount, it.stockAmountOpened)
            },
            aggregatedAmount = if (it.isAggregatedAmount) {
                amount(it.stockAmountAggregated, it.stockAmountOpenedAggregated)
            } else {
                null
            },
            stockValue = it.stockValue,
            lastPurchased = it.lastPurchased?.let(::epochDayOf),
            lastUsed = it.lastUsed?.let(::epochDayOf),
            lastPrice = price(it.lastPrice, it.priceToStockFactor),
            averagePrice = price(it.averagePrice, it.priceToStockFactor),
            averageShelfLife = when {
                it.averageShelfLifeDays < 0 -> ShelfLife.Unknown
                it.averageShelfLifeDays > ProductOverview.UNLIMITED_SHELF_LIFE_DAYS ->
                    ShelfLife.Unlimited
                else -> ShelfLife.Days(it.averageShelfLifeDays)
            },
            spoilRate = it.spoilRatePercent / PERCENT,
            priceHistory = buildPriceHistory(it.priceHistory, stores, it.priceToStockFactor),
        )
    }

    return ProductOverviewUiState(
        productId = product.id,
        name = product.name,
        description = htmlToPlainText(product.description.orEmpty()),
        pictureFileName = product.pictureFileName,
        locationName = product.locationId?.let { id -> locations.find { it.id == id }?.name },
        showShoppingList = config.shoppingList,
        showPrices = config.stockPriceTracking,
        showLocation = config.stockLocationTracking,
        showShelfLife = config.stockBestBeforeDateTracking,
        currency = config.currency,
        priceDecimals = settings.stockDecimalPlacesPricesDisplay,
        details = details,
    )
}

/** Prices per price unit like Grocy's chart, which multiplies them by the unit factor. */
internal fun buildPriceHistory(
    entries: List<PriceHistoryEntry>,
    stores: List<Store>,
    priceToStockFactor: Double,
): PriceHistory {
    val points = entries.mapNotNull { entry ->
        epochDayOf(entry.date)?.let { day ->
            entry.storeId to PricePoint(day, entry.price * priceToStockFactor)
        }
    }
    val storeNames = stores.associate { it.id to it.name }
    // Stores by id, then purchases without (known) store
    val keys = points.map { (storeId, _) -> storeId?.takeIf { it in storeNames } }
        .distinct()
        .sortedWith(compareBy(nullsLast()) { it })
    val hasOthers = keys.size > MAX_PRICE_SERIES
    val shown = if (hasOthers) keys.take(MAX_PRICE_SERIES - 1) else keys

    val series = points
        .groupBy { (storeId, _) ->
            storeId?.takeIf { it in storeNames }.let { if (it in shown) it else OTHERS }
        }
        .map { (key, storePoints) ->
            PriceSeries(
                store = when (key) {
                    OTHERS -> SeriesStore.Others
                    null -> SeriesStore.Unknown
                    else -> SeriesStore.Named(storeNames.getValue(key))
                },
                colorIndex = if (key == OTHERS) MAX_PRICE_SERIES - 1 else shown.indexOf(key),
                points = storePoints.map { it.second }.sortedBy { it.epochDay },
            )
        }
        .sortedBy { it.colorIndex }

    return PriceHistory(series = series, trend = linearTrend(points.map { it.second }))
}

/** Least squares line through [points], from their first to their last day. */
private fun linearTrend(points: List<PricePoint>): PriceTrend? {
    if (points.map { it.epochDay }.distinct().size < 2) return null
    val first = points.minOf { it.epochDay }
    val last = points.maxOf { it.epochDay }
    // Relative to the first day, so the sums stay small
    val xs = points.map { (it.epochDay - first).toDouble() }
    val ys = points.map { it.price }
    val meanX = xs.average()
    val meanY = ys.average()
    val slope = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) } /
        xs.sumOf { (it - meanX) * (it - meanX) }
    val intercept = meanY - slope * meanX
    return PriceTrend(
        startDay = first,
        startPrice = intercept,
        endDay = last,
        endPrice = intercept + slope * (last - first),
    )
}

private const val PERCENT = 100.0

// No store id is negative
private const val OTHERS = -1
