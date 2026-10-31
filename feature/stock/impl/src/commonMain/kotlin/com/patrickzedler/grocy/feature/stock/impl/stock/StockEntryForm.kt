package com.patrickzedler.grocy.feature.stock.impl.stock

import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.forms.numberError
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal

/** The inputs of Grocy's "Edit stock entry" form. */
internal data class StockEntryValues(
    val amount: String,
    val purchasedDate: String,
    val dueDate: String,
    /** Grocy's "Never overdue", the due date 2999-12-31. */
    val neverOverdue: Boolean,
    val price: String,
    val storeId: Int?,
    val locationId: Int?,
    val note: String,
    val open: Boolean,
)

internal enum class StockEntryField { AMOUNT, PURCHASED_DATE, DUE_DATE, PRICE, LOCATION }

/** Grocy's rules of the visible fields: amount, dates, location required, price optional. */
internal fun StockEntryValues.validate(
    config: ServerConfig,
    decimals: Int,
    priceDecimals: Int,
): Map<StockEntryField, FieldError> = buildMap {
    numberError(amount, minimumAmount(decimals), decimals, required = true)
        ?.let { put(StockEntryField.AMOUNT, it) }
    dateError(purchasedDate)?.let { put(StockEntryField.PURCHASED_DATE, it) }
    if (config.stockBestBeforeDateTracking && !neverOverdue) {
        dateError(dueDate)?.let { put(StockEntryField.DUE_DATE, it) }
    }
    if (config.stockPriceTracking) {
        numberError(price, 0.0, priceDecimals, required = false)
            ?.let { put(StockEntryField.PRICE, it) }
    }
    if (config.stockLocationTracking && locationId == null) {
        put(StockEntryField.LOCATION, FieldError.Required)
    }
}

private fun dateError(text: String): FieldError? = when {
    text.isBlank() -> FieldError.Required
    epochDayOf(text.trim()) == null || text.trim().length != ISO_DATE_LENGTH ->
        FieldError.InvalidDate
    else -> null
}

/**
 * The entry to save from valid values; fields the server's features hide keep [original]'s
 * values, as Grocy's form sends them unchanged.
 */
internal fun StockEntryValues.toEntry(original: StockEntry, config: ServerConfig) =
    original.copy(
        amount = parseDecimal(amount) ?: original.amount,
        purchasedDate = purchasedDate.trim(),
        bestBeforeDate = when {
            !config.stockBestBeforeDateTracking -> original.bestBeforeDate
            neverOverdue -> StockEntry.NEVER_OVERDUE
            else -> dueDate.trim()
        },
        price = if (config.stockPriceTracking) parseDecimal(price) else original.price,
        storeId = if (config.stockPriceTracking) storeId else original.storeId,
        locationId = if (config.stockLocationTracking) locationId else original.locationId,
        note = note.trim().ifEmpty { null },
        open = if (config.stockProductOpenedTracking) open else original.open,
    )

internal fun StockEntry.toValues(decimals: Int, priceDecimals: Int, decimalSeparator: Char) =
    StockEntryValues(
        amount = formatDecimal(amount, decimals, decimalSeparator),
        purchasedDate = purchasedDate.orEmpty(),
        dueDate = bestBeforeDate.orEmpty().takeUnless { it == StockEntry.NEVER_OVERDUE }.orEmpty(),
        neverOverdue = bestBeforeDate == StockEntry.NEVER_OVERDUE,
        price = price?.let { formatDecimal(it, priceDecimals, decimalSeparator) }.orEmpty(),
        storeId = storeId,
        locationId = locationId,
        note = note.orEmpty(),
        open = open,
    )

private const val ISO_DATE_LENGTH = 10
