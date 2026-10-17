package com.patrickzedler.grocy.core.model.masterdata

/**
 * A product of Grocy's master data, with every property the product form edits. Ids of other
 * objects are null when nothing is chosen.
 */
data class Product(
    val id: Int,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val parentProductId: Int? = null,
    val locationId: Int? = null,
    val defaultConsumeLocationId: Int? = null,
    val moveOnOpen: Boolean = false,
    val storeId: Int? = null,
    val minStockAmount: Double = 0.0,
    val cumulateMinStockAmountOfSubProducts: Boolean = false,
    val treatOpenedAsOutOfStock: Boolean = true,
    val dueType: DueType = DueType.BEST_BEFORE,
    val defaultDueDays: Int = 0,
    val defaultDueDaysAfterOpen: Int = 0,
    val defaultDueDaysAfterFreezing: Int = 0,
    val defaultDueDaysAfterThawing: Int = 0,
    val shouldNotBeFrozen: Boolean = false,
    val productGroupId: Int? = null,
    val quIdStock: Int? = null,
    val quIdPurchase: Int? = null,
    val quIdConsume: Int? = null,
    val quIdPrice: Int? = null,
    val enableTareWeightHandling: Boolean = false,
    val tareWeight: Double = 0.0,
    val notCheckStockFulfillmentForRecipes: Boolean = false,
    val calories: Double? = null,
    val quickConsumeAmount: Double = 1.0,
    val quickOpenAmount: Double = 1.0,
    val defaultStockLabelType: StockLabelType = StockLabelType.NONE,
    val autoReprintStockLabel: Boolean = false,
    val defaultPurchasePriceType: PurchasePriceType = PurchasePriceType.UNSPECIFIED,
    val disableOpen: Boolean = false,
    val hideOnStockOverview: Boolean = false,
    val noOwnStock: Boolean = false,
    val pictureFileName: String? = null,
    /** Values of the product's userfields by field name. */
    val userfields: Map<String, String?> = emptyMap(),
)

/** How the due date of a product's stock entries is meant, Grocy's `due_type`. */
enum class DueType(val value: Int) {
    BEST_BEFORE(1),
    EXPIRATION(2),
    ;

    companion object {
        fun of(value: Int?): DueType = entries.firstOrNull { it.value == value } ?: BEST_BEFORE
    }
}

/** Label printed on purchase, Grocy's `default_stock_label_type`. */
enum class StockLabelType(val value: Int) {
    NONE(0),
    SINGLE(1),
    PER_UNIT(2),
    ;

    companion object {
        fun of(value: Int?): StockLabelType = entries.firstOrNull { it.value == value } ?: NONE
    }
}

/** Preselected price type on purchase, Grocy's `default_purchase_price_type`. */
enum class PurchasePriceType(val value: Int) {
    UNSPECIFIED(1),
    UNIT_PRICE(2),
    TOTAL_PRICE(3),
    ;

    companion object {
        fun of(value: Int?): PurchasePriceType =
            entries.firstOrNull { it.value == value } ?: UNSPECIFIED
    }
}
