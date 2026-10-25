package com.patrickzedler.grocy.core.model.stock

/**
 * One entry of a product's stock, a row of Grocy's `stock` table: what was purchased once,
 * with its own due date, location and price.
 */
data class StockEntry(
    val id: Int,
    val productId: Int,
    val amount: Double,
    /** Date as "YYYY-MM-DD", "2999-12-31" for never overdue. */
    val bestBeforeDate: String?,
    /** Date as "YYYY-MM-DD". */
    val purchasedDate: String?,
    /** Grocy's id of the entry across transactions, also of its userfields and grocycode. */
    val stockId: String,
    /** Per stock unit. */
    val price: Double? = null,
    val open: Boolean = false,
    val openedDate: String? = null,
    val locationId: Int? = null,
    val storeId: Int? = null,
    val note: String? = null,
    /** Date and time as "YYYY-MM-DD HH:MM:SS". */
    val createdTimestamp: String? = null,
    val userfields: Map<String, String?> = emptyMap(),
) {
    companion object {
        /** Grocy's due date of entries that never become overdue. */
        const val NEVER_OVERDUE = "2999-12-31"
    }
}
