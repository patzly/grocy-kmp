package com.patrickzedler.grocy.feature.masterdata.impl.booking

import com.patrickzedler.grocy.core.forms.minimumAmount
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.stock.ProductStockDetails
import com.patrickzedler.grocy.core.model.text.decimalPlaces
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.parseDecimal
import kotlin.math.abs

/** The fields of Grocy's stock pages that can be wrong. */
internal enum class BookingField {
    PRODUCT,
    AMOUNT,
    UNIT,
    PURCHASED_DATE,
    DUE_DATE,
    PRICE,
    LOCATION,
    TO_LOCATION,
    STOCK_ENTRY,
}

/** Why a field of a stock page is wrong, with Grocy's messages. */
internal sealed interface BookingError {
    data object Required : BookingError

    /**
     * Grocy's number picker message: at least [min], at most [max] if given, not [notEqual] if
     * given, with up to [decimals] decimal places; all in the chosen unit.
     */
    data class InvalidAmount(
        val min: Double,
        val max: Double?,
        val notEqual: Double?,
        val decimals: Int,
    ) : BookingError

    /** "There are no units available at this location". */
    data object NoStockAtLocation : BookingError

    data object InvalidDate : BookingError

    /** A price below 0 or with more than [decimals] decimal places. */
    data class InvalidPrice(val decimals: Int) : BookingError
}

/** The allowed amounts of a page's amount field, in the chosen unit. */
internal data class AmountLimits(val min: Double, val max: Double?, val notEqual: Double? = null)

/**
 * Grocy's limits of the amount: purchases from its smallest step (with tare weight handling
 * more than the container plus the stock, the field is the gross weight), consumption and
 * transfer up to the stock at the location ([available], or the chosen entry's), sub products
 * consume beyond it; an inventory from 0 but not the current stock.
 */
internal fun amountLimits(
    kind: BookingKind,
    product: Product,
    details: ProductStockDetails?,
    factor: Double,
    decimals: Int,
    available: Double?,
    exactAmount: Boolean = false,
): AmountLimits {
    val step = minimumAmount(decimals)
    val stock = details?.stockAmount ?: 0.0
    val tare = product.tareWeight.takeIf { product.enableTareWeightHandling }
    return when (kind) {
        BookingKind.PURCHASE ->
            AmountLimits(min = tare?.let { it + stock + step } ?: step, max = null)

        BookingKind.CONSUME -> when {
            tare != null && !exactAmount -> AmountLimits(min = tare, max = stock + tare)
            details?.hasSubProducts == true -> AmountLimits(min = step, max = null)
            else -> AmountLimits(min = step, max = available?.let { it * factor })
        }

        BookingKind.TRANSFER -> AmountLimits(min = step, max = available?.let { it * factor })

        BookingKind.INVENTORY -> AmountLimits(
            min = tare ?: 0.0,
            max = null,
            notEqual = (stock + (tare ?: 0.0)) * factor,
        )
    }
}

/** What the fields of a stock page say, for [validateBooking]. */
internal data class BookingInput(
    val kind: BookingKind,
    val productId: Int?,
    val amount: String,
    val quId: Int?,
    val decimals: Int,
    val limits: AmountLimits?,
    /** The location has no stock (consume, transfer). */
    val noStockAtLocation: Boolean = false,
    val purchasedDate: String? = null,
    /** Null where the page has no due date or does not need one. */
    val dueDate: String? = null,
    val price: String = "",
    val priceDecimals: Int = 2,
    val locationRequired: Boolean = false,
    val locationId: Int? = null,
    val toLocationRequired: Boolean = false,
    val toLocationId: Int? = null,
    val stockEntryRequired: Boolean = false,
    val stockId: String? = null,
)

/** The wrong fields of [input], like Grocy's form validation; empty when it can be booked. */
internal fun validateBooking(input: BookingInput): Map<BookingField, BookingError> = buildMap {
    if (input.productId == null) put(BookingField.PRODUCT, BookingError.Required)
    amountError(input)?.let { put(BookingField.AMOUNT, it) }
    if (input.productId != null && input.quId == null) {
        put(BookingField.UNIT, BookingError.Required)
    }
    input.purchasedDate?.let { if (!isDate(it)) put(BookingField.PURCHASED_DATE, dateError(it)) }
    input.dueDate?.let { if (!isDate(it)) put(BookingField.DUE_DATE, dateError(it)) }
    if (input.price.isNotBlank()) {
        val price = parseDecimal(input.price)
        if (price == null || price < 0 || decimalPlaces(input.price) > input.priceDecimals) {
            put(BookingField.PRICE, BookingError.InvalidPrice(input.priceDecimals))
        }
    }
    if (input.locationRequired && input.locationId == null) {
        put(BookingField.LOCATION, BookingError.Required)
    }
    if (input.toLocationRequired && input.toLocationId == null) {
        put(BookingField.TO_LOCATION, BookingError.Required)
    }
    if (input.stockEntryRequired && input.stockId == null) {
        put(BookingField.STOCK_ENTRY, BookingError.Required)
    }
}

private fun amountError(input: BookingInput): BookingError? {
    val limits = input.limits ?: return null
    if (input.noStockAtLocation) return BookingError.NoStockAtLocation
    val amount = parseDecimal(input.amount)
    val invalid = amount == null ||
        amount < limits.min - EPSILON ||
        limits.max?.let { amount > it + EPSILON } == true ||
        limits.notEqual?.let { abs(amount - it) < EPSILON } == true ||
        decimalPlaces(input.amount) > input.decimals
    return if (invalid) {
        BookingError.InvalidAmount(limits.min, limits.max, limits.notEqual, input.decimals)
    } else {
        null
    }
}

private fun dateError(text: String) =
    if (text.isBlank()) BookingError.Required else BookingError.InvalidDate

private fun isDate(text: String): Boolean =
    text.length == ISO_DATE_LENGTH && epochDayOf(text) != null

private const val ISO_DATE_LENGTH = 10
private const val EPSILON = 1e-9
