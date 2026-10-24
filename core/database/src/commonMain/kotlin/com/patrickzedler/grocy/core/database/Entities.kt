package com.patrickzedler.grocy.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

// Copies of Grocy's tables as the server sent them, the source of truth for the UI

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
    @ColumnInfo(name = "parent_product_id") val parentProductId: Int?,
    @ColumnInfo(name = "location_id") val locationId: Int?,
    @ColumnInfo(name = "default_consume_location_id") val defaultConsumeLocationId: Int?,
    @ColumnInfo(name = "move_on_open") val moveOnOpen: Boolean,
    @ColumnInfo(name = "store_id") val storeId: Int?,
    @ColumnInfo(name = "min_stock_amount") val minStockAmount: Double,
    @ColumnInfo(name = "cumulate_min_stock_amount_of_sub_products")
    val cumulateMinStockAmountOfSubProducts: Boolean,
    @ColumnInfo(name = "treat_opened_as_out_of_stock") val treatOpenedAsOutOfStock: Boolean,
    @ColumnInfo(name = "due_type") val dueType: Int,
    @ColumnInfo(name = "default_due_days") val defaultDueDays: Int,
    @ColumnInfo(name = "default_due_days_after_open") val defaultDueDaysAfterOpen: Int,
    @ColumnInfo(name = "default_due_days_after_freezing") val defaultDueDaysAfterFreezing: Int,
    @ColumnInfo(name = "default_due_days_after_thawing") val defaultDueDaysAfterThawing: Int,
    @ColumnInfo(name = "should_not_be_frozen") val shouldNotBeFrozen: Boolean,
    @ColumnInfo(name = "product_group_id") val productGroupId: Int?,
    @ColumnInfo(name = "qu_id_stock") val quIdStock: Int?,
    @ColumnInfo(name = "qu_id_purchase") val quIdPurchase: Int?,
    @ColumnInfo(name = "qu_id_consume") val quIdConsume: Int?,
    @ColumnInfo(name = "qu_id_price") val quIdPrice: Int?,
    @ColumnInfo(name = "enable_tare_weight_handling") val enableTareWeightHandling: Boolean,
    @ColumnInfo(name = "tare_weight") val tareWeight: Double,
    @ColumnInfo(name = "not_check_stock_fulfillment_for_recipes")
    val notCheckStockFulfillmentForRecipes: Boolean,
    val calories: Double?,
    @ColumnInfo(name = "quick_consume_amount") val quickConsumeAmount: Double,
    @ColumnInfo(name = "quick_open_amount") val quickOpenAmount: Double,
    @ColumnInfo(name = "default_stock_label_type") val defaultStockLabelType: Int,
    @ColumnInfo(name = "auto_reprint_stock_label") val autoReprintStockLabel: Boolean,
    @ColumnInfo(name = "default_purchase_price_type") val defaultPurchasePriceType: Int,
    @ColumnInfo(name = "disable_open") val disableOpen: Boolean,
    @ColumnInfo(name = "hide_on_stock_overview") val hideOnStockOverview: Boolean,
    @ColumnInfo(name = "no_own_stock") val noOwnStock: Boolean,
    @ColumnInfo(name = "picture_file_name") val pictureFileName: String?,
    /** Userfield values by name as JSON object. */
    val userfields: String,
)

@Entity(tableName = "product_groups")
data class ProductGroupEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "is_freezer") val isFreezer: Boolean,
    val active: Boolean,
)

@Entity(tableName = "quantity_units")
data class QuantityUnitEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "name_plural") val namePlural: String?,
    val description: String?,
    val active: Boolean,
)

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
)

@Entity(tableName = "product_barcodes")
data class ProductBarcodeEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id", index = true) val productId: Int,
    val barcode: String,
    @ColumnInfo(name = "qu_id") val quId: Int?,
    val amount: Double?,
    @ColumnInfo(name = "store_id") val storeId: Int?,
    @ColumnInfo(name = "last_price") val lastPrice: Double?,
    val note: String?,
)

@Entity(tableName = "stock")
data class StockEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    val amount: Double,
    @ColumnInfo(name = "amount_aggregated") val amountAggregated: Double,
)

@Entity(tableName = "userfields")
data class UserfieldEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(index = true) val entity: String,
    val name: String,
    val caption: String,
    val type: String,
    @ColumnInfo(name = "show_as_column_in_tables") val showAsColumnInTables: Boolean,
    @ColumnInfo(name = "input_required") val inputRequired: Boolean,
    /** Options of the preset types, one per line like Grocy stores them. */
    val options: String,
    @ColumnInfo(name = "default_value") val defaultValue: String?,
    @ColumnInfo(name = "sort_number") val sortNumber: Int?,
)

/** Small values of the server as JSON, e.g. its feature flags and the user settings. */
@Entity(tableName = "server_values")
data class ServerValueEntity(
    @PrimaryKey val key: String,
    val json: String,
)

/**
 * A product's picture as the server scaled it for the product list, kept for offline use.
 * Grocy gives every new picture a new file name, so a stored one never gets outdated.
 */
@Entity(tableName = "product_thumbnails")
class ProductThumbnailEntity(
    @PrimaryKey @ColumnInfo(name = "file_name") val fileName: String,
    val bytes: ByteArray,
)

/**
 * The last downloaded "Product overview" of a product as JSON, so it opens offline too; computed
 * by the server (averages, spoil rate, price history), so never derived from the other tables.
 */
@Entity(tableName = "product_overviews")
data class ProductOverviewEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    val json: String,
)
