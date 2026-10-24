package com.patrickzedler.grocy.core.network.dto

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
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
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
    fun flag(value: Boolean) = JsonPrimitive(if (value) 1 else 0)
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
) {
    fun toModel() = ProductGroup(id, name, description, active ?: true)
}

@Serializable
internal data class LocationDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    @SerialName("is_freezer") val isFreezer: FlexBoolean = null,
    val active: FlexBoolean = null,
) {
    fun toModel() = Location(id, name, description, isFreezer ?: false, active ?: true)
}

@Serializable
internal data class QuantityUnitDto(
    val id: Int,
    val name: String,
    @SerialName("name_plural") val namePlural: FlexString = null,
    val description: FlexString = null,
    val active: FlexBoolean = null,
) {
    fun toModel() = QuantityUnit(id, name, namePlural, description, active ?: true)
}

@Serializable
internal data class StoreDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    val active: FlexBoolean = null,
) {
    fun toModel() = Store(id, name, description, active ?: true)
}

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
) {
    fun toModel() = ProductBarcode(id, productId, barcode, quId, amount, storeId, lastPrice, note)
}

@Serializable
internal data class StockDto(
    @SerialName("product_id") val productId: Int,
    val amount: FlexDouble = null,
    @SerialName("amount_aggregated") val amountAggregated: FlexDouble = null,
) {
    fun toModel() = StockAmount(
        productId = productId,
        amount = amount ?: 0.0,
        amountAggregated = amountAggregated ?: amount ?: 0.0,
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
private fun String.toIsoDate(): String? = take(ISO_DATE_LENGTH).takeIf { ISO_DATE.matches(it) }

private const val ISO_DATE_LENGTH = 10
private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
