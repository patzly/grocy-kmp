package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.ChoreAssignmentType
import com.patrickzedler.grocy.core.model.masterdata.ChorePeriodType
import com.patrickzedler.grocy.core.model.masterdata.DueType
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.PriceHistoryEntry
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.ProductOverview
import com.patrickzedler.grocy.core.model.masterdata.PurchasePriceType
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.StockLabelType
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userentity
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.Weekday
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class ProductDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    @SerialName("parent_product_id") val parentProductId: FlexInt = null,
    @SerialName("location_id") val locationId: FlexInt = null,
    @SerialName("default_consume_location_id") val defaultConsumeLocationId: FlexInt = null,
    @SerialName("move_on_open") val moveOnOpen: FlexBoolean = null,
    @SerialName("shopping_location_id") val shoppingLocationId: FlexInt = null,
    @SerialName("min_stock_amount") val minStockAmount: FlexDouble = null,
    @SerialName("cumulate_min_stock_amount_of_sub_products")
    val cumulateMinStockAmountOfSubProducts: FlexBoolean = null,
    @SerialName("treat_opened_as_out_of_stock") val treatOpenedAsOutOfStock: FlexBoolean = null,
    @SerialName("due_type") val dueType: FlexInt = null,
    @SerialName("default_best_before_days") val defaultDueDays: FlexInt = null,
    @SerialName("default_best_before_days_after_open") val defaultDueDaysAfterOpen: FlexInt = null,
    @SerialName("default_best_before_days_after_freezing")
    val defaultDueDaysAfterFreezing: FlexInt = null,
    @SerialName("default_best_before_days_after_thawing")
    val defaultDueDaysAfterThawing: FlexInt = null,
    @SerialName("should_not_be_frozen") val shouldNotBeFrozen: FlexBoolean = null,
    @SerialName("product_group_id") val productGroupId: FlexInt = null,
    @SerialName("qu_id_stock") val quIdStock: FlexInt = null,
    @SerialName("qu_id_purchase") val quIdPurchase: FlexInt = null,
    @SerialName("qu_id_consume") val quIdConsume: FlexInt = null,
    @SerialName("qu_id_price") val quIdPrice: FlexInt = null,
    @SerialName("enable_tare_weight_handling") val enableTareWeightHandling: FlexBoolean = null,
    @SerialName("tare_weight") val tareWeight: FlexDouble = null,
    @SerialName("not_check_stock_fulfillment_for_recipes")
    val notCheckStockFulfillmentForRecipes: FlexBoolean = null,
    val calories: FlexDouble = null,
    @SerialName("quick_consume_amount") val quickConsumeAmount: FlexDouble = null,
    @SerialName("quick_open_amount") val quickOpenAmount: FlexDouble = null,
    @SerialName("default_stock_label_type") val defaultStockLabelType: FlexInt = null,
    @SerialName("auto_reprint_stock_label") val autoReprintStockLabel: FlexBoolean = null,
    @SerialName("default_purchase_price_type") val defaultPurchasePriceType: FlexInt = null,
    @SerialName("disable_open") val disableOpen: FlexBoolean = null,
    @SerialName("hide_on_stock_overview") val hideOnStockOverview: FlexBoolean = null,
    @SerialName("no_own_stock") val noOwnStock: FlexBoolean = null,
    @SerialName("picture_file_name") val pictureFileName: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Product(
        id = id,
        name = name,
        description = description,
        active = active ?: true,
        parentProductId = parentProductId,
        locationId = locationId,
        defaultConsumeLocationId = defaultConsumeLocationId,
        moveOnOpen = moveOnOpen ?: false,
        storeId = shoppingLocationId,
        minStockAmount = minStockAmount ?: 0.0,
        cumulateMinStockAmountOfSubProducts = cumulateMinStockAmountOfSubProducts ?: false,
        treatOpenedAsOutOfStock = treatOpenedAsOutOfStock ?: true,
        dueType = DueType.of(dueType),
        defaultDueDays = defaultDueDays ?: 0,
        defaultDueDaysAfterOpen = defaultDueDaysAfterOpen ?: 0,
        defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing ?: 0,
        defaultDueDaysAfterThawing = defaultDueDaysAfterThawing ?: 0,
        shouldNotBeFrozen = shouldNotBeFrozen ?: false,
        productGroupId = productGroupId,
        quIdStock = quIdStock,
        quIdPurchase = quIdPurchase,
        quIdConsume = quIdConsume,
        quIdPrice = quIdPrice,
        enableTareWeightHandling = enableTareWeightHandling ?: false,
        tareWeight = tareWeight ?: 0.0,
        notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes ?: false,
        calories = calories,
        quickConsumeAmount = quickConsumeAmount ?: 1.0,
        quickOpenAmount = quickOpenAmount ?: 1.0,
        defaultStockLabelType = StockLabelType.of(defaultStockLabelType),
        autoReprintStockLabel = autoReprintStockLabel ?: false,
        defaultPurchasePriceType = PurchasePriceType.of(defaultPurchasePriceType),
        disableOpen = disableOpen ?: false,
        hideOnStockOverview = hideOnStockOverview ?: false,
        noOwnStock = noOwnStock ?: false,
        pictureFileName = pictureFileName,
        userfields = userfields.orEmpty(),
    )
}

/**
 * The product as Grocy's `products` table expects it; flags as 0 and 1 like the web frontend
 * sends them. Userfields are saved separately.
 */
internal fun Product.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("active", flag(active))
    put("parent_product_id", parentProductId)
    put("location_id", locationId)
    put("default_consume_location_id", defaultConsumeLocationId)
    put("move_on_open", flag(moveOnOpen))
    put("shopping_location_id", storeId)
    put("min_stock_amount", minStockAmount)
    put("cumulate_min_stock_amount_of_sub_products", flag(cumulateMinStockAmountOfSubProducts))
    put("treat_opened_as_out_of_stock", flag(treatOpenedAsOutOfStock))
    put("due_type", dueType.value)
    put("default_best_before_days", defaultDueDays)
    put("default_best_before_days_after_open", defaultDueDaysAfterOpen)
    put("default_best_before_days_after_freezing", defaultDueDaysAfterFreezing)
    put("default_best_before_days_after_thawing", defaultDueDaysAfterThawing)
    put("should_not_be_frozen", flag(shouldNotBeFrozen))
    put("product_group_id", productGroupId)
    put("qu_id_stock", quIdStock)
    put("qu_id_purchase", quIdPurchase)
    put("qu_id_consume", quIdConsume)
    put("qu_id_price", quIdPrice)
    put("enable_tare_weight_handling", flag(enableTareWeightHandling))
    put("tare_weight", tareWeight)
    put("not_check_stock_fulfillment_for_recipes", flag(notCheckStockFulfillmentForRecipes))
    put("calories", calories)
    put("quick_consume_amount", quickConsumeAmount)
    put("quick_open_amount", quickOpenAmount)
    put("default_stock_label_type", defaultStockLabelType.value)
    put("auto_reprint_stock_label", flag(autoReprintStockLabel))
    put("default_purchase_price_type", defaultPurchasePriceType.value)
    put("disable_open", flag(disableOpen))
    put("hide_on_stock_overview", flag(hideOnStockOverview))
    put("no_own_stock", flag(noOwnStock))
    put("picture_file_name", pictureFileName)
}

@Serializable
internal data class ProductGroupDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = ProductGroup(id, name, description, active ?: true, userfields.orEmpty())
}

internal fun ProductGroup.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("active", flag(active))
}

@Serializable
internal data class LocationDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    @SerialName("is_freezer") val isFreezer: FlexBoolean = null,
    val active: FlexBoolean = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() =
        Location(id, name, description, isFreezer ?: false, active ?: true, userfields.orEmpty())
}

internal fun Location.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("is_freezer", flag(isFreezer))
    put("active", flag(active))
}

@Serializable
internal data class QuantityUnitDto(
    val id: Int,
    val name: String,
    @SerialName("name_plural") val namePlural: FlexString = null,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    @SerialName("plural_forms") val pluralForms: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = QuantityUnit(
        id = id,
        name = name,
        namePlural = namePlural,
        description = description,
        active = active ?: true,
        pluralForms = pluralForms,
        userfields = userfields.orEmpty(),
    )
}

internal fun QuantityUnit.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("name_plural", namePlural.orEmpty())
    put("plural_forms", pluralForms)
    put("description", description.orEmpty())
    put("active", flag(active))
}

@Serializable
internal data class StoreDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Store(id, name, description, active ?: true, userfields.orEmpty())
}

internal fun Store.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("active", flag(active))
}

@Serializable
internal data class ChoreDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    @SerialName("period_type") val periodType: FlexString = null,
    @SerialName("period_days") val periodDays: FlexInt = null,
    @SerialName("period_interval") val periodInterval: FlexInt = null,
    @SerialName("period_config") val periodConfig: FlexString = null,
    @SerialName("start_date") val startDate: FlexString = null,
    @SerialName("assignment_type") val assignmentType: FlexString = null,
    @SerialName("assignment_config") val assignmentConfig: FlexString = null,
    @SerialName("track_date_only") val trackDateOnly: FlexBoolean = null,
    val rollover: FlexBoolean = null,
    @SerialName("consume_product_on_execution") val consumeProductOnExecution: FlexBoolean = null,
    @SerialName("product_id") val productId: FlexInt = null,
    @SerialName("product_amount") val productAmount: FlexDouble = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Chore(
        id = id,
        name = name,
        description = description,
        active = active ?: true,
        periodType = ChorePeriodType.of(periodType),
        periodDays = periodDays ?: 0,
        periodInterval = periodInterval ?: 1,
        weekdays = Weekday.parse(periodConfig),
        startDate = startDate,
        assignmentType = ChoreAssignmentType.of(assignmentType),
        assignedUserIds = assignmentConfig.orEmpty().split(",")
            .mapNotNull { it.trim().toIntOrNull() },
        trackDateOnly = trackDateOnly ?: false,
        rollover = rollover ?: false,
        consumeProductOnExecution = consumeProductOnExecution ?: false,
        productId = productId,
        productAmount = productAmount,
        userfields = userfields.orEmpty(),
    )
}

/** Like Grocy's form: the weekdays only for weekly chores, the product only when consumed. */
internal fun Chore.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("active", flag(active))
    put("period_type", periodType.value)
    put("period_days", periodDays)
    put("period_interval", periodInterval)
    put(
        "period_config",
        if (periodType == ChorePeriodType.WEEKLY) Weekday.format(weekdays) else "",
    )
    put("start_date", startDate)
    put("assignment_type", assignmentType.value)
    put("assignment_config", assignedUserIds.joinToString(","))
    put("track_date_only", flag(trackDateOnly))
    put("rollover", flag(rollover))
    put("consume_product_on_execution", flag(consumeProductOnExecution))
    put("product_id", productId)
    put("product_amount", productAmount)
}

@Serializable
internal data class BatteryDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    @SerialName("used_in") val usedIn: FlexString = null,
    @SerialName("charge_interval_days") val chargeIntervalDays: FlexInt = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Battery(
        id = id,
        name = name,
        description = description,
        active = active ?: true,
        usedIn = usedIn,
        chargeIntervalDays = chargeIntervalDays ?: 0,
        userfields = userfields.orEmpty(),
    )
}

internal fun Battery.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("active", flag(active))
    put("used_in", usedIn.orEmpty())
    put("charge_interval_days", chargeIntervalDays)
}

@Serializable
internal data class TaskCategoryDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = TaskCategory(id, name, description, active ?: true, userfields.orEmpty())
}

internal fun TaskCategory.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("active", flag(active))
}

@Serializable
internal data class UserentityDto(
    val id: Int,
    val name: String,
    val caption: FlexString = null,
    val description: FlexString = null,
    @SerialName("show_in_sidebar_menu") val showInSidebarMenu: FlexBoolean = null,
    @SerialName("icon_css_class") val iconCssClass: FlexString = null,
) {
    fun toModel() = Userentity(
        id = id,
        name = name,
        caption = caption?.takeIf { it.isNotBlank() } ?: name,
        description = description,
        showInSidebarMenu = showInSidebarMenu ?: false,
        iconCssClass = iconCssClass,
    )
}

internal fun Userentity.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("caption", caption)
    put("description", description.orEmpty())
    put("show_in_sidebar_menu", flag(showInSidebarMenu))
    put("icon_css_class", iconCssClass.orEmpty())
}

/** Flags as 0 and 1, like Grocy's web frontend sends them. */
internal fun flag(value: Boolean) = JsonPrimitive(if (value) 1 else 0)

@Serializable
internal data class ProductBarcodeDto(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val barcode: String,
    @SerialName("qu_id") val quId: FlexInt = null,
    val amount: FlexDouble = null,
    @SerialName("shopping_location_id") val storeId: FlexInt = null,
    @SerialName("last_price") val lastPrice: FlexDouble = null,
    val note: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = ProductBarcode(
        id = id,
        productId = productId,
        barcode = barcode,
        quId = quId,
        amount = amount,
        storeId = storeId,
        lastPrice = lastPrice,
        note = note,
        userfields = userfields.orEmpty(),
    )
}

/**
 * The barcode as Grocy's barcode form sends it; the last price stays the purchases' own.
 * Userfields are saved separately.
 */
internal fun ProductBarcode.toJson(): JsonObject = buildJsonObject {
    put("product_id", productId)
    put("barcode", barcode)
    put("qu_id", quId)
    put("amount", amount)
    put("shopping_location_id", storeId)
    put("note", note.orEmpty())
}

@Serializable
internal data class StockDto(
    @SerialName("product_id") val productId: Int,
    val amount: FlexDouble = null,
    @SerialName("amount_aggregated") val amountAggregated: FlexDouble = null,
    @SerialName("amount_opened") val amountOpened: FlexDouble = null,
    @SerialName("amount_opened_aggregated") val amountOpenedAggregated: FlexDouble = null,
    val value: FlexDouble = null,
    @SerialName("best_before_date") val bestBeforeDate: FlexString = null,
    @SerialName("is_aggregated_amount") val isAggregatedAmount: FlexBoolean = null,
) {
    fun toModel() = StockAmount(
        productId = productId,
        amount = amount ?: 0.0,
        amountAggregated = amountAggregated ?: amount ?: 0.0,
        amountOpened = amountOpened ?: 0.0,
        amountOpenedAggregated = amountOpenedAggregated ?: amountOpened ?: 0.0,
        value = value ?: 0.0,
        bestBeforeDate = bestBeforeDate?.takeIf { it.isNotBlank() },
        isAggregatedAmount = isAggregatedAmount ?: false,
    )
}

@Serializable
internal data class UserfieldDto(
    val id: Int,
    val entity: String,
    val name: String,
    val caption: FlexString = null,
    val type: FlexString = null,
    @SerialName("show_as_column_in_tables") val showAsColumnInTables: FlexBoolean = null,
    @SerialName("input_required") val inputRequired: FlexBoolean = null,
    val config: FlexString = null,
    @SerialName("default_value") val defaultValue: FlexString = null,
    @SerialName("sort_number") val sortNumber: FlexInt = null,
) {
    fun toModel() = Userfield(
        id = id,
        entity = entity,
        name = name,
        caption = caption?.takeIf { it.isNotBlank() } ?: name,
        type = UserfieldType.of(type),
        showAsColumnInTables = showAsColumnInTables ?: false,
        inputRequired = inputRequired ?: false,
        options = config.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() },
        defaultValue = defaultValue,
        sortNumber = sortNumber,
    )
}

/** The definition as Grocy's userfield form sends it; options are its configuration lines. */
internal fun Userfield.toJson(): JsonObject = buildJsonObject {
    put("entity", entity)
    put("name", name)
    put("caption", caption)
    put("type", type.value)
    put("show_as_column_in_tables", flag(showAsColumnInTables))
    put("input_required", flag(inputRequired))
    put("config", options.joinToString("\n"))
    put("default_value", defaultValue.orEmpty())
    put("sort_number", sortNumber)
}

@Serializable
internal data class CreatedObjectDto(
    @SerialName("created_object_id") val createdObjectId: FlexInt = null,
)

@Serializable
internal data class ErrorDto(
    @SerialName("error_message") val errorMessage: FlexString = null,
)

/** The computed part of Grocy's `/stock/products/{id}`, the rest the app has stored already. */
@Serializable
internal data class ProductDetailsDto(
    @SerialName("last_purchased") val lastPurchased: FlexString = null,
    @SerialName("last_used") val lastUsed: FlexString = null,
    @SerialName("stock_amount") val stockAmount: FlexDouble = null,
    @SerialName("stock_value") val stockValue: FlexDouble = null,
    @SerialName("stock_amount_opened") val stockAmountOpened: FlexDouble = null,
    @SerialName("stock_amount_aggregated") val stockAmountAggregated: FlexDouble = null,
    @SerialName("stock_amount_opened_aggregated")
    val stockAmountOpenedAggregated: FlexDouble = null,
    @SerialName("is_aggregated_amount") val isAggregatedAmount: FlexBoolean = null,
    @SerialName("last_price") val lastPrice: FlexDouble = null,
    @SerialName("avg_price") val averagePrice: FlexDouble = null,
    @SerialName("qu_conversion_factor_price_to_stock") val priceToStockFactor: FlexDouble = null,
    @SerialName("average_shelf_life_days") val averageShelfLifeDays: FlexDouble = null,
    @SerialName("spoil_rate_percent") val spoilRatePercent: FlexDouble = null,
) {
    fun toModel(productId: Int, priceHistory: List<PriceHistoryEntry>) = ProductOverview(
        productId = productId,
        stockAmount = stockAmount ?: 0.0,
        stockAmountOpened = stockAmountOpened ?: 0.0,
        stockAmountAggregated = stockAmountAggregated ?: 0.0,
        stockAmountOpenedAggregated = stockAmountOpenedAggregated ?: 0.0,
        isAggregatedAmount = isAggregatedAmount ?: false,
        stockValue = stockValue ?: 0.0,
        lastPurchased = lastPurchased?.toIsoDate(),
        lastUsed = lastUsed?.toIsoDate(),
        lastPrice = lastPrice,
        averagePrice = averagePrice,
        priceToStockFactor = priceToStockFactor?.takeIf { it > 0 } ?: 1.0,
        averageShelfLifeDays = averageShelfLifeDays ?: ProductOverview.UNKNOWN_SHELF_LIFE,
        spoilRatePercent = spoilRatePercent ?: 0.0,
        priceHistory = priceHistory,
    )
}

/** One entry of `/stock/products/{id}/price-history`. */
@Serializable
internal data class PriceHistoryDto(
    val date: FlexString = null,
    val price: FlexDouble = null,
    @SerialName("shopping_location") val store: StoreDto? = null,
) {
    /** Null for entries without date or price, which the chart cannot place. */
    fun toModel(): PriceHistoryEntry? {
        val day = date?.toIsoDate() ?: return null
        return PriceHistoryEntry(date = day, price = price ?: return null, storeId = store?.id)
    }
}

/** The date part of Grocy's "YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS". */
internal fun String.toIsoDate(): String? = take(ISO_DATE_LENGTH).takeIf { ISO_DATE.matches(it) }

private const val ISO_DATE_LENGTH = 10
private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
