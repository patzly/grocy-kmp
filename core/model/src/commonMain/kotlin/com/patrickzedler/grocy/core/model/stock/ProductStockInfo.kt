package com.patrickzedler.grocy.core.model.stock

/**
 * What Grocy's stock overview shows of a product besides its amounts, each value as the server
 * computed it: null when the server has none.
 */
data class ProductStockInfo(
    val productId: Int,
    /** How much is missing up to the min. stock amount, null when nothing is. */
    val amountMissing: Double? = null,
    /** True when the product is on any shopping list. */
    val onShoppingList: Boolean = false,
    /** Date of the last purchase ("YYYY-MM-DD"). */
    val lastPurchased: String? = null,
    /** Price of the last purchase per stock unit. */
    val lastPrice: Double? = null,
    /** Average purchase price per stock unit. */
    val averagePrice: Double? = null,
)

/** The amount of a product at one location, Grocy's `stock_current_locations`. */
data class StockLocationAmount(
    val productId: Int,
    val locationId: Int,
    val amount: Double,
)

/** A product's last purchase, Grocy's `products_last_purchased`. */
data class ProductPurchase(
    val productId: Int,
    /** "YYYY-MM-DD". */
    val date: String?,
    /** Per stock unit. */
    val price: Double?,
)
