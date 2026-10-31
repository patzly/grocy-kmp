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
    /** Userfield values by name as JSON object. */
    @ColumnInfo(defaultValue = "{}") val userfields: String,
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "is_freezer") val isFreezer: Boolean,
    val active: Boolean,
    /** Userfield values by name as JSON object. */
    @ColumnInfo(defaultValue = "{}") val userfields: String,
)

@Entity(tableName = "quantity_units")
data class QuantityUnitEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "name_plural") val namePlural: String?,
    val description: String?,
    val active: Boolean,
    @ColumnInfo(name = "plural_forms") val pluralForms: String?,
    /** Userfield values by name as JSON object. */
    @ColumnInfo(defaultValue = "{}") val userfields: String,
)

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
    /** Userfield values by name as JSON object. */
    @ColumnInfo(defaultValue = "{}") val userfields: String,
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
    /** Userfield values by name as JSON object. */
    @ColumnInfo(defaultValue = "{}") val userfields: String,
)

/** A defined conversion: a default one without product, an override of a product with one. */
@Entity(tableName = "quantity_unit_conversions")
data class QuantityUnitConversionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "from_qu_id") val fromQuId: Int,
    @ColumnInfo(name = "to_qu_id") val toQuId: Int,
    val factor: Double,
    @ColumnInfo(name = "product_id", index = true) val productId: Int?,
    /** Userfield values by name as JSON object. */
    val userfields: String,
)

/** A conversion Grocy derived; contradictory definitions can derive a pair twice. */
@Entity(tableName = "quantity_unit_conversions_resolved")
data class ResolvedQuantityUnitConversionEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    @ColumnInfo(name = "product_id", index = true) val productId: Int?,
    @ColumnInfo(name = "from_qu_id") val fromQuId: Int,
    @ColumnInfo(name = "to_qu_id") val toQuId: Int,
    val factor: Double,
)

/** An entry of a product's stock as the stock entries last downloaded it. */
@Entity(tableName = "stock_entries")
data class StockEntryEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id", index = true) val productId: Int,
    val amount: Double,
    @ColumnInfo(name = "best_before_date") val bestBeforeDate: String?,
    @ColumnInfo(name = "purchased_date") val purchasedDate: String?,
    @ColumnInfo(name = "stock_id") val stockId: String,
    val price: Double?,
    val open: Boolean,
    @ColumnInfo(name = "opened_date") val openedDate: String?,
    @ColumnInfo(name = "location_id") val locationId: Int?,
    @ColumnInfo(name = "store_id") val storeId: Int?,
    val note: String?,
    @ColumnInfo(name = "created_timestamp") val createdTimestamp: String?,
    /** Userfield values by name as JSON object. */
    val userfields: String,
)

/** A booking of the stock journal as last downloaded. */
@Entity(tableName = "stock_log")
data class StockLogEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id", index = true) val productId: Int,
    val amount: Double,
    @ColumnInfo(name = "transaction_type") val transactionType: String,
    @ColumnInfo(name = "created_timestamp", index = true) val createdTimestamp: String,
    val spoiled: Boolean,
    val undone: Boolean,
    @ColumnInfo(name = "undone_timestamp") val undoneTimestamp: String?,
    @ColumnInfo(name = "location_id") val locationId: Int?,
    @ColumnInfo(name = "user_id") val userId: Int?,
    @ColumnInfo(name = "correlation_id") val correlationId: String?,
    @ColumnInfo(name = "stock_id") val stockId: String?,
    val note: String?,
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** The list's "Notes" as HTML. */
    val description: String? = null,
    /** Userfield values by name as JSON object. */
    @ColumnInfo(defaultValue = "{}") val userfields: String = "{}",
)

/** An item of a shopping list as the server has it, [done] as the user last set it here. */
@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "shopping_list_id", index = true) val listId: Int,
    @ColumnInfo(name = "product_id") val productId: Int?,
    val note: String?,
    /** In the product's stock unit. */
    val amount: Double,
    @ColumnInfo(name = "qu_id") val quId: Int?,
    val done: Boolean,
    /** Userfield values by name as JSON object. */
    val userfields: String,
)

/**
 * The user's own check off of an item, kept until the server took it: checking off works
 * offline and is sent once the server can be reached again.
 */
@Entity(tableName = "pending_shopping_list_done")
data class PendingShoppingListDoneEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: Int,
    val done: Boolean,
)

/**
 * The user's own "Notes" of a list as HTML, kept until the server took them: editing them works
 * offline and they are sent once the server can be reached again.
 */
@Entity(tableName = "pending_shopping_list_notes")
data class PendingShoppingListNotesEntity(
    @PrimaryKey @ColumnInfo(name = "list_id") val listId: Int,
    val description: String,
)

@Entity(tableName = "stock")
data class StockEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    val amount: Double,
    @ColumnInfo(name = "amount_aggregated") val amountAggregated: Double,
    @ColumnInfo(name = "amount_opened", defaultValue = "0") val amountOpened: Double,
    @ColumnInfo(name = "amount_opened_aggregated", defaultValue = "0")
    val amountOpenedAggregated: Double,
    @ColumnInfo(defaultValue = "0") val value: Double,
    @ColumnInfo(name = "best_before_date") val bestBeforeDate: String?,
    @ColumnInfo(name = "is_aggregated_amount", defaultValue = "0")
    val isAggregatedAmount: Boolean,
)

/** What Grocy's stock overview shows of a product besides its amounts, as last downloaded. */
@Entity(tableName = "product_stock_info")
data class ProductStockInfoEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "amount_missing") val amountMissing: Double?,
    @ColumnInfo(name = "on_shopping_list") val onShoppingList: Boolean,
    @ColumnInfo(name = "last_purchased") val lastPurchased: String?,
    @ColumnInfo(name = "last_price") val lastPrice: Double?,
    @ColumnInfo(name = "average_price") val averagePrice: Double?,
)

/** A product's amount at one location, Grocy's `stock_current_locations`. */
@Entity(tableName = "stock_locations", primaryKeys = ["product_id", "location_id"])
data class StockLocationEntity(
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "location_id") val locationId: Int,
    val amount: Double,
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

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
    @ColumnInfo(name = "period_type") val periodType: String,
    @ColumnInfo(name = "period_days") val periodDays: Int,
    @ColumnInfo(name = "period_interval") val periodInterval: Int,
    /** Weekdays of weekly chores, comma separated like Grocy's `period_config`. */
    val weekdays: String,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "assignment_type") val assignmentType: String,
    /** User ids, comma separated like Grocy's `assignment_config`. */
    @ColumnInfo(name = "assigned_user_ids") val assignedUserIds: String,
    @ColumnInfo(name = "track_date_only") val trackDateOnly: Boolean,
    val rollover: Boolean,
    @ColumnInfo(name = "consume_product_on_execution") val consumeProductOnExecution: Boolean,
    @ColumnInfo(name = "product_id") val productId: Int?,
    @ColumnInfo(name = "product_amount") val productAmount: Double?,
    val userfields: String,
)

@Entity(tableName = "batteries")
data class BatteryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
    @ColumnInfo(name = "used_in") val usedIn: String?,
    @ColumnInfo(name = "charge_interval_days") val chargeIntervalDays: Int,
    val userfields: String,
)

@Entity(tableName = "task_categories")
data class TaskCategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val active: Boolean,
    val userfields: String,
)

@Entity(tableName = "userentities")
data class UserentityEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val caption: String,
    val description: String?,
    @ColumnInfo(name = "show_in_sidebar_menu") val showInSidebarMenu: Boolean,
    @ColumnInfo(name = "icon_css_class") val iconCssClass: String?,
)

/** The server's users, e.g. to assign chores to. */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    @ColumnInfo(name = "display_name") val displayName: String,
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
