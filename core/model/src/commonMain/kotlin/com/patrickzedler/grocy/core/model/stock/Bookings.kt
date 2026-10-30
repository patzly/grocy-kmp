package com.patrickzedler.grocy.core.model.stock

/**
 * What Grocy's stock pages read of a product from `/stock/products/{id}` besides its stored
 * master data: amounts in the stock unit, the last purchase and the next due date.
 */
data class ProductStockDetails(
    val productId: Int,
    val stockAmount: Double = 0.0,
    val stockAmountOpened: Double = 0.0,
    /** Per stock unit, null if never purchased with a price. */
    val lastPrice: Double? = null,
    val lastStoreId: Int? = null,
    /** Date as "YYYY-MM-DD", null without stock. */
    val nextDueDate: String? = null,
    /** True for a parent product, whose sub products can be consumed in its place. */
    val hasSubProducts: Boolean = false,
)

/** A product's amount at one location (`/stock/products/{id}/locations`), in the stock unit. */
data class ProductLocationAmount(
    val locationId: Int,
    val locationName: String,
    val amount: Double,
    val isFreezer: Boolean = false,
)

/** Grocy's "Purchase" of a product; amounts and prices in and per the stock unit. */
data class PurchaseBooking(
    val amount: Double,
    /** Null without price tracking, sent as 0 like Grocy's page. */
    val price: Double?,
    /** "YYYY-MM-DD", null lets the server take the product's default due days. */
    val bestBeforeDate: String?,
    /** "YYYY-MM-DD", null for today. */
    val purchasedDate: String?,
    val storeId: Int?,
    /** Null for the product's default location. */
    val locationId: Int?,
    /** Grocy's `stock_label_type`, null without label printer. */
    val labelType: Int?,
    val note: String,
)

/** Grocy's "Consume" of a product; [amount] in the stock unit (gross with tare handling). */
data class ConsumeBooking(
    val amount: Double,
    val spoiled: Boolean,
    /** Tare weight handling: [amount] is the net amount to remove, not the gross weight. */
    val exactAmount: Boolean = false,
    /** A specific stock entry instead of Grocy's default consume rule. */
    val stockEntryId: String? = null,
    val locationId: Int? = null,
    /** For Grocy's statistics only. */
    val recipeId: Int? = null,
)

/** Grocy's "Mark as opened" of the consume page; [amount] in the stock unit. */
data class OpenBooking(
    val amount: Double,
    val stockEntryId: String? = null,
    val locationId: Int? = null,
)

/** Grocy's "Transfer" between two locations; [amount] in the stock unit. */
data class TransferBooking(
    val amount: Double,
    val fromLocationId: Int,
    val toLocationId: Int,
    val stockEntryId: String? = null,
)

/**
 * Grocy's "Inventory": the product's [newAmount] in the stock unit; the other values only
 * matter for what an increase adds.
 */
data class InventoryBooking(
    val newAmount: Double,
    /** Null lets the server take the product's default due days. */
    val bestBeforeDate: String?,
    val purchasedDate: String?,
    /** Per stock unit; null lets the server take the product's last price. */
    val price: Double?,
    val storeId: Int?,
    val locationId: Int?,
    val labelType: Int?,
    val note: String,
)

/** The result of a booking: Grocy's transaction to undo it and the first stock entry's id. */
data class BookingResult(val transactionId: String?, val stockId: String?)

/**
 * A scanned grocycode of a product, "grcy:p:<product id>" or of one of its stock entries with
 * its [stockId] after another colon.
 */
data class ProductGrocycode(val productId: Int, val stockId: String?) {
    companion object {
        private val PATTERN = Regex("""grcy:p:(\d+)(?::([^:\s]+))?""")

        /** The grocycode [code] stands for, null for anything else (e.g. a barcode). */
        fun parse(code: String): ProductGrocycode? {
            val match = PATTERN.matchEntire(code.trim()) ?: return null
            val productId = match.groupValues[1].toIntOrNull() ?: return null
            return ProductGrocycode(productId, match.groupValues[2].ifEmpty { null })
        }
    }
}
