package com.patrickzedler.grocy.core.model.stock

import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.text.addMonths
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.firstDayOfMonth
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.model.text.lastDayOfMonth
import com.patrickzedler.grocy.core.model.text.yearOfDay

/** The fields of a booking of Grocy's `stock_log` its price history needs. */
data class PriceLogRow(
    val id: Int,
    val productId: Int,
    val stockId: String?,
    val transactionType: String,
    /** In the stock unit, negative for what left the stock. */
    val amount: Double,
    /** Per stock unit. */
    val price: Double?,
    /** "YYYY-MM-DD". */
    val purchasedDate: String?,
    val storeId: Int?,
    val undone: Boolean,
)

/** A purchase with its price, a row of Grocy's `products_price_history`. */
data class PricedPurchase(
    val productId: Int,
    /** Per stock unit. */
    val price: Double,
    val amount: Double,
    val purchasedDate: String,
    val storeId: Int?,
    val transactionType: String,
)

private val OriginTypes = setOf(
    StockTransactionType.PURCHASE.value,
    StockTransactionType.INVENTORY_CORRECTION.value,
    StockTransactionType.SELF_PRODUCTION.value,
)

/**
 * Grocy's `products_price_history` of [rows]: what came into stock with a price, an edited entry
 * with its newest edit's price and date and the amount it then had at the start (the edit's
 * amount plus what was consumed before). [rows] must hold every booking of an entry whose
 * newest edit counts and every newest edit, so a dated query alone is not enough.
 */
fun pricedPurchasesOf(rows: Collection<PriceLogRow>): List<PricedPurchase> {
    val byStock = rows.filter { it.stockId != null }.groupBy { it.stockId!! }
    // Grocy's stock_edited_entries: entries that came into stock and were edited since
    val newestEdits = byStock.mapNotNull { (stockId, bookings) ->
        val newestEdit = bookings
            .filter { it.transactionType == StockTransactionType.STOCK_EDIT_NEW.value }
            .maxByOrNull { it.id }
        val hasOrigin = bookings.any { it.transactionType in OriginTypes && it.amount > 0 }
        if (newestEdit != null && hasOrigin) stockId to newestEdit else null
    }.toMap()

    return rows.mapNotNull { row ->
        if (row.undone) return@mapNotNull null
        val amount = when {
            row.transactionType in OriginTypes && row.stockId !in newestEdits -> row.amount
            row.transactionType == StockTransactionType.STOCK_EDIT_NEW.value &&
                newestEdits[row.stockId]?.id == row.id -> editedOriginAmount(row, byStock)
            else -> return@mapNotNull null
        }
        val price = row.price ?: 0.0
        val date = row.purchasedDate
        if (price <= 0.0 || amount <= 0.0 || date == null) return@mapNotNull null
        PricedPurchase(row.productId, price, amount, date, row.storeId, row.transactionType)
    }
}

/** The edit's amount and what was consumed or corrected away before it. */
private fun editedOriginAmount(edit: PriceLogRow, byStock: Map<String, List<PriceLogRow>>) =
    edit.amount - byStock[edit.stockId].orEmpty()
        .filter {
            it.id < edit.id && it.amount < 0 && !it.undone &&
                (
                    it.transactionType == StockTransactionType.CONSUME.value ||
                        it.transactionType == StockTransactionType.INVENTORY_CORRECTION.value
                    )
        }
        .sumOf { it.amount }

/** What Grocy's "Spendings" groups by. */
enum class SpendingsGrouping { PRODUCT, PRODUCT_GROUP, STORE }

/** Grocy's product group filter of the spendings by product. */
sealed interface ProductGroupFilter {
    data object All : ProductGroupFilter

    data object Ungrouped : ProductGroupFilter

    data class Group(val id: Int) : ProductGroupFilter
}

/**
 * One row of Grocy's spendings: a product, product group or store with what its purchases cost.
 * [name] is null for the ungrouped products or purchases without a store; [productGroup] is the
 * product's group when grouped by product.
 */
data class SpendingsRow(
    val id: Int?,
    val name: String?,
    val total: Double,
    val productGroup: String? = null,
)

/**
 * Grocy's spendings of [purchases] from [from] to [to] ("YYYY-MM-DD", both included) grouped by
 * [grouping], ordered by name like its table; self-production costs nothing. Products the app
 * does not know are left out like Grocy's join leaves out deleted ones.
 */
fun spendingsOf(
    purchases: List<PricedPurchase>,
    from: String,
    to: String,
    grouping: SpendingsGrouping,
    groupFilter: ProductGroupFilter,
    products: List<Product>,
    productGroups: List<ProductGroup>,
    stores: List<Store>,
): List<SpendingsRow> {
    val productsById = products.associateBy { it.id }
    val groupsById = productGroups.associateBy { it.id }
    val counted = purchases.filter {
        it.transactionType != StockTransactionType.SELF_PRODUCTION.value &&
            it.purchasedDate in from..to && it.productId in productsById
    }
    val rows = when (grouping) {
        SpendingsGrouping.PRODUCT -> counted
            .filter { groupFilter.matches(productsById.getValue(it.productId).productGroupId) }
            .groupBy { it.productId }
            .map { (id, list) ->
                val product = productsById.getValue(id)
                SpendingsRow(
                    id = id,
                    name = product.name,
                    total = list.total(),
                    productGroup = product.productGroupId?.let { groupsById[it]?.name },
                )
            }
        // A deleted group or store counts as none, like Grocy's left join
        SpendingsGrouping.PRODUCT_GROUP -> counted
            .groupBy { purchase ->
                productsById.getValue(purchase.productId).productGroupId
                    ?.takeIf { it in groupsById }
            }
            .map { (id, list) -> SpendingsRow(id, id?.let { groupsById[it]?.name }, list.total()) }
        SpendingsGrouping.STORE -> {
            val storesById = stores.associateBy { it.id }
            counted.groupBy { purchase -> purchase.storeId?.takeIf { it in storesById } }
                .map { (id, list) ->
                    SpendingsRow(id, id?.let { storesById[it]?.name }, list.total())
                }
        }
    }
    // SQLite sorts NULL first and NOCASE by the name
    return rows.sortedWith(
        compareBy<SpendingsRow> { it.name != null }.thenBy { it.name?.lowercase() },
    )
}

private fun ProductGroupFilter.matches(groupId: Int?): Boolean = when (this) {
    ProductGroupFilter.All -> true
    ProductGroupFilter.Ungrouped -> groupId == null
    is ProductGroupFilter.Group -> groupId == id
}

private fun List<PricedPurchase>.total() = sumOf { it.amount * it.price }

/** The date ranges of Grocy's spendings picker; [CUSTOM] is any other. */
enum class SpendingsRangePreset {
    TODAY, YESTERDAY, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_MONTH, THIS_YEAR,
    LAST_YEAR, CUSTOM,
}

/** A date range of the spendings, as days since 1970-01-01, both included. */
data class SpendingsRange(val preset: SpendingsRangePreset, val start: Long, val end: Long) {
    val from: String get() = isoDateOf(start)
    val to: String get() = isoDateOf(end)

    companion object {
        /** Grocy's default: this month. */
        fun default(today: Long) = of(SpendingsRangePreset.THIS_MONTH, today)

        /** [preset]'s days around [today], like Grocy's picker (moment.js) computes them. */
        fun of(preset: SpendingsRangePreset, today: Long): SpendingsRange {
            val (start, end) = when (preset) {
                SpendingsRangePreset.TODAY -> today to today
                SpendingsRangePreset.YESTERDAY -> today - 1 to today - 1
                SpendingsRangePreset.LAST_7_DAYS -> today - 6 to today
                SpendingsRangePreset.LAST_14_DAYS -> today - 13 to today
                SpendingsRangePreset.LAST_30_DAYS -> today - 29 to today
                SpendingsRangePreset.THIS_MONTH, SpendingsRangePreset.CUSTOM ->
                    firstDayOfMonth(today) to lastDayOfMonth(today)
                SpendingsRangePreset.LAST_MONTH -> {
                    val lastMonth = addMonths(firstDayOfMonth(today), -1)
                    lastMonth to lastDayOfMonth(lastMonth)
                }
                SpendingsRangePreset.THIS_YEAR -> yearRange(yearOfDay(today))
                SpendingsRangePreset.LAST_YEAR -> yearRange(yearOfDay(today) - 1)
            }
            return SpendingsRange(preset, start, end)
        }

        /** A range the user picked, [start] and [end] in either order. */
        fun custom(start: Long, end: Long) =
            SpendingsRange(SpendingsRangePreset.CUSTOM, minOf(start, end), maxOf(start, end))

        private fun yearRange(year: Int): Pair<Long, Long> {
            val first = epochDayOf("${year.toString().padStart(4, '0')}-01-01")!!
            return first to epochDayOf("${year.toString().padStart(4, '0')}-12-31")!!
        }
    }
}

/** A product of Grocy's "Location Content Sheet": its amount at the location, opened included. */
data class LocationContentItem(
    val product: Product,
    val amount: Double,
    val amountOpened: Double,
)

/** A location of the sheet with what is there. */
data class LocationContent(val location: Location, val items: List<LocationContentItem>)

/**
 * Grocy's "Location Content Sheet" of the stock [entries]: per location, ordered by name, the
 * active products there with their amounts; [includeOutOfStock] adds the products without
 * stock at their default location. Locations with nothing are left out.
 */
fun locationContentOf(
    entries: List<StockEntry>,
    products: List<Product>,
    locations: List<Location>,
    includeOutOfStock: Boolean,
): List<LocationContent> {
    val active = products.filter { it.active }
    val inStock = entries.filter { it.locationId != null }
        .groupBy { it.productId to it.locationId!! }
    val items = active.flatMap { product ->
        val stocked = inStock.filterKeys { it.first == product.id }.map { (key, list) ->
            key.second to LocationContentItem(
                product = product,
                amount = list.sumOf { it.amount },
                amountOpened = list.filter { it.open }.sumOf { it.amount },
            )
        }
        val default = product.locationId
        if (stocked.isEmpty() && includeOutOfStock && default != null) {
            listOf(default to LocationContentItem(product, 0.0, 0.0))
        } else {
            stocked
        }
    }.groupBy({ it.first }, { it.second })
    return locations.sortedBy { it.name.lowercase() }.mapNotNull { location ->
        items[location.id]?.let { list ->
            // Grocy orders them by name without NOCASE
            LocationContent(location, list.sortedBy { it.product.name })
        }
    }
}
