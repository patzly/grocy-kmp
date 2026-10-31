package com.patrickzedler.grocy.feature.masterdata.impl.booking

import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.stock.ProductGrocycode
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.productcard.BookingKind

/**
 * The products Grocy's page of [kind] offers: active ones with own stock to purchase or count,
 * the ones in stock (sub products included) to consume, those with own stock in stock to move.
 */
internal fun bookableProducts(
    kind: BookingKind,
    products: List<Product>,
    stock: List<StockAmount>,
): List<Product> {
    val inStock = stock.filter { it.amountAggregated > 0 }.mapTo(HashSet()) { it.productId }
    return products.filter { product ->
        product.active && when (kind) {
            BookingKind.PURCHASE, BookingKind.INVENTORY -> !product.noOwnStock
            BookingKind.CONSUME -> product.id in inStock
            BookingKind.TRANSFER -> !product.noOwnStock && product.id in inStock
        }
    }.sortedBy { it.name.lowercase() }
}

/** What a scanned or typed code stands for. */
internal sealed interface CodeMatch {
    /** A product of the page; [barcode] if the code is one, [stockId] for a stock entry's code. */
    data class Found(
        val product: Product,
        val barcode: ProductBarcode? = null,
        val stockId: String? = null,
    ) : CodeMatch

    /** A known product the page does not offer, e.g. one not in stock on the consume page. */
    data object NotOffered : CodeMatch

    /** Nothing known, Grocy's "Create or assign product" workflow. */
    data object Unknown : CodeMatch
}

/**
 * Resolves [code] like Grocy's product picker: a grocycode by its product id (and stock id), a
 * barcode by any product's barcodes; [offered] are the page's products, [all] every product.
 */
internal fun matchCode(
    code: String,
    offered: List<Product>,
    all: List<Product>,
    barcodes: List<ProductBarcode>,
): CodeMatch {
    val trimmed = code.trim()
    val grocycode = ProductGrocycode.parse(trimmed)
    val barcode = if (grocycode == null) barcodes.firstOrNull { it.barcode == trimmed } else null
    val productId = grocycode?.productId ?: barcode?.productId
        ?: return if (all.any { it.name.equals(trimmed, ignoreCase = true) }) {
            CodeMatch.NotOffered
        } else {
            CodeMatch.Unknown
        }
    val product = offered.firstOrNull { it.id == productId }
        ?: return if (all.any { it.id == productId }) CodeMatch.NotOffered else CodeMatch.Unknown
    return CodeMatch.Found(product, barcode, grocycode?.stockId)
}

/**
 * The price per stock unit Grocy books for a [price] typed for a unit with [factor] (that
 * unit's amount per stock unit); a [isTotalPrice] is for the whole [displayAmount], without
 * the [tareWeight] of the container.
 */
internal fun pricePerStockUnit(
    price: Double,
    factor: Double,
    isTotalPrice: Boolean,
    displayAmount: Double,
    tareWeight: Double = 0.0,
): Double {
    val perUnit = price * factor
    val amount = displayAmount - tareWeight
    return if (isTotalPrice && amount > 0) perUnit / amount else perUnit
}

/** The due date Grocy presets for [days] after [today]: none for 0, never overdue for -1. */
internal fun dueDateAfter(days: Int, today: Long): String? = when {
    days == NEVER_OVERDUE_DAYS -> NEVER_OVERDUE_DATE
    days == 0 -> null
    else -> isoDateOf(today + days)
}

/**
 * The due date field of Grocy's purchase page after choosing a product or a location: the
 * product's default only into an empty field, a freezer's default (with [freezing]) also over
 * that default, so a date typed or kept from another product stays.
 */
internal fun purchaseDueDate(
    current: String,
    product: Product,
    location: Location?,
    freezing: Boolean,
    today: Long,
): String {
    val default = dueDateAfter(product.defaultDueDays, today)
    val frozen = if (freezing && location?.isFreezer == true) {
        dueDateAfter(product.defaultDueDaysAfterFreezing, today)
    } else {
        null
    }
    return when {
        frozen != null && (current.isBlank() || current == default) -> frozen
        current.isBlank() -> default.orEmpty()
        else -> current
    }
}

/** What an inventory of [newAmount] changes, all in the stock unit. */
internal sealed interface InventoryChange {
    /** [amount] will be added; due date and location are needed then. */
    data class Added(val amount: Double) : InventoryChange

    data class Removed(val amount: Double) : InventoryChange

    /** Nothing changes, which Grocy refuses. */
    data object None : InventoryChange
}

/**
 * Grocy's "This means ... will be added to/removed from stock" for [newAmount] (the gross
 * weight with tare weight handling, [tareWeight] then the container's weight).
 */
internal fun inventoryChange(
    newAmount: Double,
    stockAmount: Double,
    tareWeight: Double = 0.0,
): InventoryChange {
    val change = newAmount - stockAmount - tareWeight
    return when {
        change > AMOUNT_EPSILON -> InventoryChange.Added(change)
        change < -AMOUNT_EPSILON -> InventoryChange.Removed(-change)
        else -> InventoryChange.None
    }
}

/**
 * The location Grocy's consume page preselects: the product's default consume location, else
 * its default location, as long as it has stock there, otherwise the first one with stock.
 */
internal fun consumeLocation(product: Product, amounts: List<ProductLocationAmount>): Int? {
    val preferred = product.defaultConsumeLocationId ?: product.locationId
    return amounts.firstOrNull { it.locationId == preferred }?.locationId
        ?: amounts.firstOrNull()?.locationId
}

/** The location Grocy's transfer page preselects as source, its default one if it has stock. */
internal fun transferSource(product: Product, amounts: List<ProductLocationAmount>): Int? =
    amounts.firstOrNull { it.locationId == product.locationId }?.locationId
        ?: amounts.lastOrNull()?.locationId

/** The default location of the product among [locationIds], for Grocy's "(Default location)". */
internal fun defaultLocationOf(product: Product, consume: Boolean): Int? =
    if (consume) product.defaultConsumeLocationId ?: product.locationId else product.locationId

/** The stock entries at [locationId] (all without location tracking), in Grocy's consume order. */
internal fun entriesAt(entries: List<StockEntry>, locationId: Int?): List<StockEntry> =
    if (locationId == null) entries else entries.filter { it.locationId == locationId }

/** Amount at [locationId] and how much of it is opened, in the stock unit. */
internal data class LocationStock(val amount: Double, val opened: Double) {
    val unopened: Double get() = amount - opened
}

internal fun locationStockOf(entries: List<StockEntry>, locationId: Int?): LocationStock {
    val here = entriesAt(entries, locationId)
    return LocationStock(here.sumOf { it.amount }, here.filter { it.open }.sumOf { it.amount })
}

/** Grocy's due date of entries that never become overdue, and its preset days for it. */
internal const val NEVER_OVERDUE_DATE = StockEntry.NEVER_OVERDUE
private const val NEVER_OVERDUE_DAYS = -1

// Amounts are compared with Grocy's smallest decimal step in mind
private const val AMOUNT_EPSILON = 1e-9
