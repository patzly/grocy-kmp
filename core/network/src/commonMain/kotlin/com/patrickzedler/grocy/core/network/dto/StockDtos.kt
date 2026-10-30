package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.masterdata.QuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.stock.ProductPurchase
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class QuantityUnitConversionDto(
    val id: Int,
    @SerialName("from_qu_id") val fromQuId: Int,
    @SerialName("to_qu_id") val toQuId: Int,
    val factor: FlexDouble = null,
    @SerialName("product_id") val productId: FlexInt = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = QuantityUnitConversion(
        id = id,
        fromQuId = fromQuId,
        toQuId = toQuId,
        factor = factor ?: 1.0,
        productId = productId,
        userfields = userfields.orEmpty(),
    )
}

/** The conversion as Grocy's form sends it; default conversions have no product. */
internal fun QuantityUnitConversion.toJson(): JsonObject = buildJsonObject {
    put("from_qu_id", fromQuId)
    put("to_qu_id", toQuId)
    put("factor", factor)
    productId?.let { put("product_id", it) }
}

@Serializable
internal data class ResolvedQuantityUnitConversionDto(
    @SerialName("product_id") val productId: FlexInt = null,
    @SerialName("from_qu_id") val fromQuId: Int,
    @SerialName("to_qu_id") val toQuId: Int,
    val factor: FlexDouble = null,
) {
    fun toModel() = ResolvedQuantityUnitConversion(productId, fromQuId, toQuId, factor ?: 1.0)
}

/** A row of Grocy's `stock`, with the userfields of its `stock_id`. */
@Serializable
internal data class StockEntryDto(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val amount: FlexDouble = null,
    @SerialName("best_before_date") val bestBeforeDate: FlexString = null,
    @SerialName("purchased_date") val purchasedDate: FlexString = null,
    @SerialName("stock_id") val stockId: FlexString = null,
    val price: FlexDouble = null,
    val open: FlexBoolean = null,
    @SerialName("opened_date") val openedDate: FlexString = null,
    @SerialName("row_created_timestamp") val createdTimestamp: FlexString = null,
    @SerialName("location_id") val locationId: FlexInt = null,
    @SerialName("shopping_location_id") val storeId: FlexInt = null,
    val note: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = StockEntry(
        id = id,
        productId = productId,
        amount = amount ?: 0.0,
        bestBeforeDate = bestBeforeDate?.takeIf { it.isNotBlank() },
        purchasedDate = purchasedDate?.takeIf { it.isNotBlank() },
        stockId = stockId.orEmpty(),
        price = price,
        open = open ?: false,
        openedDate = openedDate?.takeIf { it.isNotBlank() },
        locationId = locationId,
        storeId = storeId,
        note = note?.takeIf { it.isNotBlank() },
        createdTimestamp = createdTimestamp,
        userfields = userfields.orEmpty(),
    )
}

/** A booking of Grocy's `stock_log`. */
@Serializable
internal data class StockLogDto(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val amount: FlexDouble = null,
    @SerialName("transaction_type") val transactionType: FlexString = null,
    @SerialName("row_created_timestamp") val createdTimestamp: FlexString = null,
    val spoiled: FlexBoolean = null,
    val undone: FlexBoolean = null,
    @SerialName("undone_timestamp") val undoneTimestamp: FlexString = null,
    @SerialName("location_id") val locationId: FlexInt = null,
    @SerialName("user_id") val userId: FlexInt = null,
    @SerialName("correlation_id") val correlationId: FlexString = null,
    @SerialName("stock_id") val stockId: FlexString = null,
    /** The action the booking belongs to; undoing it undoes all of its bookings. */
    @SerialName("transaction_id") val transactionId: FlexString = null,
    val note: FlexString = null,
) {
    fun toModel() = StockLogEntry(
        id = id,
        productId = productId,
        amount = amount ?: 0.0,
        transactionType = transactionType.orEmpty(),
        createdTimestamp = createdTimestamp.orEmpty(),
        spoiled = spoiled ?: false,
        undone = undone ?: false,
        undoneTimestamp = undoneTimestamp?.takeIf { it.isNotBlank() },
        locationId = locationId,
        userId = userId,
        correlationId = correlationId?.takeIf { it.isNotBlank() },
        stockId = stockId?.takeIf { it.isNotBlank() },
        note = note?.takeIf { it.isNotBlank() },
    )
}

/** The part of Grocy's `/stock/volatile` its stock overview takes from there. */
@Serializable
internal data class VolatileStockDto(
    @SerialName("missing_products") val missingProducts: List<MissingProductDto> = emptyList(),
)

/** A product below its min. stock amount. */
@Serializable
internal data class MissingProductDto(
    val id: Int,
    @SerialName("amount_missing") val amountMissing: FlexDouble = null,
)

/** A row of Grocy's `products_last_purchased`. */
@Serializable
internal data class LastPurchaseDto(
    @SerialName("product_id") val productId: Int,
    @SerialName("purchased_date") val purchasedDate: FlexString = null,
    val price: FlexDouble = null,
) {
    fun toModel() = ProductPurchase(
        productId = productId,
        date = purchasedDate?.takeIf { it.isNotBlank() },
        // Grocy stores 0 when the purchase had no price
        price = price?.takeIf { it > 0.0 },
    )
}

/** A row of Grocy's `products_average_price`. */
@Serializable
internal data class AveragePriceDto(
    @SerialName("product_id") val productId: Int,
    val price: FlexDouble = null,
)

/** A row of Grocy's `stock_current_locations`. */
@Serializable
internal data class StockLocationDto(
    @SerialName("product_id") val productId: Int,
    @SerialName("location_id") val locationId: Int,
    val amount: FlexDouble = null,
) {
    fun toModel() = StockLocationAmount(productId, locationId, amount ?: 0.0)
}
