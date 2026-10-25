package com.patrickzedler.grocy.core.model.stock

/** One booking of Grocy's stock journal (`stock_log`). */
data class StockLogEntry(
    val id: Int,
    val productId: Int,
    /** In the product's stock unit, negative for what left the stock. */
    val amount: Double,
    /** Grocy's `transaction_type`, see [StockTransactionType]. */
    val transactionType: String,
    /** Date and time as "YYYY-MM-DD HH:MM:SS". */
    val createdTimestamp: String,
    val spoiled: Boolean = false,
    val undone: Boolean = false,
    val undoneTimestamp: String? = null,
    val locationId: Int? = null,
    val userId: Int? = null,
    /** Bookings of one action (e.g. a transfer) share it and are undone together. */
    val correlationId: String? = null,
    val stockId: String? = null,
    val note: String? = null,
)

/** Grocy's transaction types, in the order of its filter. */
enum class StockTransactionType(val value: String) {
    CONSUME("consume"),
    INVENTORY_CORRECTION("inventory-correction"),
    PRODUCT_OPENED("product-opened"),
    PURCHASE("purchase"),
    SELF_PRODUCTION("self-production"),
    STOCK_EDIT_NEW("stock-edit-new"),
    STOCK_EDIT_OLD("stock-edit-old"),
    TRANSFER_FROM("transfer_from"),
    TRANSFER_TO("transfer_to"),
    ;

    companion object {
        fun of(value: String): StockTransactionType? = entries.firstOrNull { it.value == value }
    }
}
