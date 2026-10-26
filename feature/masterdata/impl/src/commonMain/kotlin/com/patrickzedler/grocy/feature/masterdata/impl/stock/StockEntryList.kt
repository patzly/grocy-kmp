package com.patrickzedler.grocy.feature.masterdata.impl.stock

import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal

/** How Grocy highlights an entry by its due date. */
internal enum class DueStatus {
    /** Due within the user's "Due soon days". */
    DUE_SOON,

    /** A best before date in the past: still usable, Grocy greys it. */
    OVERDUE,

    /** An expiration date in the past: Grocy shows it red. */
    EXPIRED,
}

/** The filters of Grocy's stock entries page: product and location. */
internal data class StockEntryFilter(
    val productId: Int? = null,
    val locationId: Int? = null,
) {
    val isActive: Boolean
        get() = productId != null || locationId != null
}

/** One row of Grocy's stock entries table. */
internal data class StockEntryItem(
    val entry: StockEntry,
    val productName: String,
    /** Amount and stock unit, e.g. "2 Pieces". */
    val amount: String,
    /** Days since 1970-01-01, null without due date tracking or date. */
    val dueDay: Long?,
    val neverOverdue: Boolean,
    val dueStatus: DueStatus?,
    val locationName: String?,
    val storeName: String?,
    /** Per price unit like Grocy's column, null without price or price tracking. */
    val price: EntryPrice?,
    val purchasedDay: Long?,
    /** Grocy's open button: tracked, not opened yet, and the product can be opened. */
    val canOpen: Boolean,
)

/** A price per unit, e.g. 6 per Pack; formatted by the screen. */
internal data class EntryPrice(val price: Double, val unit: String)

/** What the page shows: the filtered rows and what the filters offer. */
internal data class StockEntriesUiState(
    val items: List<StockEntryItem>,
    /** Products with stock entries, the product filter's choices. */
    val products: List<Product>,
    val locations: List<Location>,
    val filter: StockEntryFilter,
    val config: ServerConfig,
    val currency: String,
    val priceDecimals: Int,
    val hasEntries: Boolean,
)

/**
 * The rows of Grocy's stock entries page: entries of active products with stock, filtered,
 * by product name and then in the order they are consumed (due date, then purchase).
 */
internal fun buildStockEntryItems(
    entries: List<StockEntry>,
    products: List<Product>,
    units: List<QuantityUnit>,
    locations: List<Location>,
    stores: List<Store>,
    resolved: List<ResolvedQuantityUnitConversion>,
    config: ServerConfig,
    settings: UserSettings,
    filter: StockEntryFilter,
    today: Long,
    decimalSeparator: Char,
    language: String,
): List<StockEntryItem> {
    val productsById = products.associateBy { it.id }
    val unitsById = units.associateBy { it.id }
    val decimals = settings.stockDecimalPlacesAmounts
    val locationsById = locations.associateBy { it.id }
    val storesById = stores.associateBy { it.id }
    return entries
        .filter { it.amount > 0 }
        .filter { filter.productId == null || it.productId == filter.productId }
        .filter { filter.locationId == null || it.locationId == filter.locationId }
        .mapNotNull { entry ->
            val product = productsById[entry.productId]?.takeIf { it.active }
                ?: return@mapNotNull null
            val stockUnit = product.quIdStock?.let { unitsById[it] }
            val neverOverdue = entry.bestBeforeDate == StockEntry.NEVER_OVERDUE
            val dueDay = entry.bestBeforeDate
                ?.takeIf { config.stockBestBeforeDateTracking }
                ?.let(::epochDayOf)
            StockEntryItem(
                entry = entry,
                productName = product.name,
                amount = listOfNotNull(
                    formatDecimal(entry.amount, decimals, decimalSeparator),
                    stockUnit?.nameFor(entry.amount, language),
                ).joinToString(" "),
                dueDay = dueDay,
                neverOverdue = neverOverdue,
                dueStatus = dueDay?.takeUnless { neverOverdue }?.let { day ->
                    dueStatusOf(day, today, settings.stockDueSoonDays, product.dueType)
                },
                locationName = entry.locationId?.let { locationsById[it]?.name }
                    ?.takeIf { config.stockLocationTracking },
                storeName = entry.storeId?.let { storesById[it]?.name }
                    ?.takeIf { config.stockPriceTracking },
                price = entry.price?.takeIf { config.stockPriceTracking }?.let { price ->
                    val priceUnit = product.quIdPrice?.let { unitsById[it] } ?: stockUnit
                    EntryPrice(
                        price = price * priceToStockFactor(product, resolved),
                        unit = priceUnit?.name.orEmpty(),
                    )
                },
                purchasedDay = entry.purchasedDate?.let(::epochDayOf),
                canOpen = config.stockProductOpenedTracking && !entry.open &&
                    !product.enableTareWeightHandling && !product.disableOpen,
            )
        }
        .sortedWith(
            compareBy<StockEntryItem>(
                { it.productName.lowercase() },
                { it.dueDay ?: Long.MAX_VALUE },
                { it.purchasedDay ?: Long.MAX_VALUE },
                { it.entry.id },
            ),
        )
}

/**
 * Grocy's highlighting: a due date before today is overdue (best before) or expired
 * (expiration date), one within the next [dueSoonDays] days due soon.
 */
internal fun dueStatusOf(dueDay: Long, today: Long, dueSoonDays: Int, dueType: DueType) = when {
    dueDay < today -> if (dueType == DueType.EXPIRATION) DueStatus.EXPIRED else DueStatus.OVERDUE
    dueDay <= today + dueSoonDays -> DueStatus.DUE_SOON
    else -> null
}

/** Stock units in one price unit of the product, 1 without a known conversion. */
internal fun priceToStockFactor(
    product: Product,
    resolved: List<ResolvedQuantityUnitConversion>,
): Double = toStockFactor(product, product.quIdPrice, resolved)

/** Stock units in one unit [quId] of the product, 1 without a known conversion. */
internal fun toStockFactor(
    product: Product,
    quId: Int?,
    resolved: List<ResolvedQuantityUnitConversion>,
): Double {
    val from = quId ?: return 1.0
    val stock = product.quIdStock ?: return 1.0
    if (from == stock) return 1.0
    return resolved.firstOrNull {
        it.productId == product.id && it.fromQuId == from && it.toQuId == stock
    }?.factor ?: 1.0
}
