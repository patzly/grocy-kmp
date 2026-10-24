package com.patrickzedler.grocy.core.model.masterdata

import kotlinx.serialization.Serializable

/**
 * What Grocy computes about a product for its "Product overview" (`/stock/products/{id}` and
 * its price history). Names, units and the picture come from the stored product instead.
 * Stored as JSON for offline use, so new fields need defaults.
 */
@Serializable
data class ProductOverview(
    val productId: Int,
    val stockAmount: Double = 0.0,
    val stockAmountOpened: Double = 0.0,
    /** Including the stock of sub products, only meaningful when [isAggregatedAmount]. */
    val stockAmountAggregated: Double = 0.0,
    val stockAmountOpenedAggregated: Double = 0.0,
    /** True for a parent product whose sub products are in stock. */
    val isAggregatedAmount: Boolean = false,
    val stockValue: Double = 0.0,
    /** Date as "YYYY-MM-DD", null if never purchased. */
    val lastPurchased: String? = null,
    /** Date as "YYYY-MM-DD", null if never used. */
    val lastUsed: String? = null,
    /** Per stock unit, null if unknown. */
    val lastPrice: Double? = null,
    /** Per stock unit, null if unknown. */
    val averagePrice: Double? = null,
    /** Stock units in one price unit, prices per stock unit times this are per price unit. */
    val priceToStockFactor: Double = 1.0,
    /** -1 if unknown, more than [UNLIMITED_SHELF_LIFE_DAYS] means it never spoils. */
    val averageShelfLifeDays: Double = UNKNOWN_SHELF_LIFE,
    val spoilRatePercent: Double = 0.0,
    /** Newest first, like Grocy sends it; empty without price tracking. */
    val priceHistory: List<PriceHistoryEntry> = emptyList(),
) {
    companion object {
        const val UNKNOWN_SHELF_LIFE = -1.0

        /** Grocy shows "Unlimited" above 200 years. */
        const val UNLIMITED_SHELF_LIFE_DAYS = 73_000.0
    }
}

/** One purchase price of a product, per stock unit. */
@Serializable
data class PriceHistoryEntry(
    /** Date as "YYYY-MM-DD". */
    val date: String,
    val price: Double,
    val storeId: Int? = null,
)
