package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.stock.ConsumeBooking
import com.patrickzedler.grocy.core.model.stock.InventoryBooking
import com.patrickzedler.grocy.core.model.stock.OpenBooking
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.ProductStockDetails
import com.patrickzedler.grocy.core.model.stock.PurchaseBooking
import com.patrickzedler.grocy.core.model.stock.TransferBooking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** What the stock pages read of `/stock/products/{id}`. */
@Serializable
internal data class ProductStockDetailsDto(
    @SerialName("stock_amount") val stockAmount: FlexDouble = null,
    @SerialName("stock_amount_opened") val stockAmountOpened: FlexDouble = null,
    @SerialName("last_price") val lastPrice: FlexDouble = null,
    @SerialName("last_shopping_location_id") val lastStoreId: FlexInt = null,
    @SerialName("next_due_date") val nextDueDate: FlexString = null,
    @SerialName("has_childs") val hasChildren: FlexBoolean = null,
) {
    fun toModel(productId: Int) = ProductStockDetails(
        productId = productId,
        stockAmount = stockAmount ?: 0.0,
        stockAmountOpened = stockAmountOpened ?: 0.0,
        lastPrice = lastPrice,
        lastStoreId = lastStoreId,
        nextDueDate = nextDueDate?.toIsoDate(),
        hasSubProducts = hasChildren ?: false,
    )
}

/** A row of `/stock/products/{id}/locations`. */
@Serializable
internal data class ProductLocationAmountDto(
    @SerialName("location_id") val locationId: Int,
    @SerialName("location_name") val locationName: FlexString = null,
    val amount: FlexDouble = null,
    @SerialName("location_is_freezer") val isFreezer: FlexBoolean = null,
) {
    fun toModel() = ProductLocationAmount(
        locationId = locationId,
        locationName = locationName.orEmpty(),
        amount = amount ?: 0.0,
        isFreezer = isFreezer ?: false,
    )
}

/** A recipe as Grocy's consume page offers it, `/objects/recipes`. */
@Serializable
internal data class RecipeDto(
    val id: Int,
    val name: FlexString = null,
    val type: FlexString = null,
)

/** The body of `POST /stock/products/{id}/add`, like Grocy's purchase page sends it. */
internal fun PurchaseBooking.toJson(): JsonObject = buildJsonObject {
    put("amount", amount)
    // Grocy's page sends 0 for an empty price or without price tracking
    put("price", price ?: 0.0)
    bestBeforeDate?.let { put("best_before_date", it) }
    purchasedDate?.let { put("purchased_date", it) }
    storeId?.let { put("shopping_location_id", it) }
    locationId?.let { put("location_id", it) }
    labelType?.let { put("stock_label_type", it) }
    put("note", note)
}

/** The body of `POST /stock/products/{id}/consume`. */
internal fun ConsumeBooking.toJson(): JsonObject = buildJsonObject {
    put("amount", amount)
    put("exact_amount", exactAmount)
    put("spoiled", spoiled)
    put("allow_subproduct_substitution", true)
    stockEntryId?.let { put("stock_entry_id", it) }
    locationId?.let { put("location_id", it) }
    recipeId?.let { put("recipe_id", it) }
}

/** The body of `POST /stock/products/{id}/open`. */
internal fun OpenBooking.toJson(): JsonObject = buildJsonObject {
    put("amount", amount)
    put("allow_subproduct_substitution", true)
    stockEntryId?.let { put("stock_entry_id", it) }
    locationId?.let { put("location_id", it) }
}

/** The body of `POST /stock/products/{id}/transfer`. */
internal fun TransferBooking.toJson(): JsonObject = buildJsonObject {
    put("amount", amount)
    put("location_id_from", fromLocationId)
    put("location_id_to", toLocationId)
    stockEntryId?.let { put("stock_entry_id", it) }
}

/** The body of `POST /stock/products/{id}/inventory`. */
internal fun InventoryBooking.toJson(): JsonObject = buildJsonObject {
    put("new_amount", newAmount)
    bestBeforeDate?.let { put("best_before_date", it) }
    purchasedDate?.let { put("purchased_date", it) }
    // Without a price the server takes the product's last price, like Grocy's empty field
    price?.let { put("price", it) }
    storeId?.let { put("shopping_location_id", it) }
    locationId?.let { put("location_id", it) }
    labelType?.let { put("stock_label_type", it) }
    put("note", note)
}
