package com.patrickzedler.grocy.feature.stock.impl.journal

import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import com.patrickzedler.grocy.core.model.stock.StockTransactionType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.isoDateOf

/** Grocy's date ranges of the journal in months, the last one for everything. */
internal val JournalRanges = listOf(1, 6, 12, 24, JOURNAL_ALL_MONTHS)

internal const val JOURNAL_DEFAULT_MONTHS = 6
internal const val JOURNAL_ALL_MONTHS = 9999

/**
 * The first day of the journal's range of [months] before [today] like Grocy's SQL
 * `DATE('now', '-6 months')`, null for everything.
 */
internal fun journalSince(today: Long, months: Int): String? {
    if (months >= JOURNAL_ALL_MONTHS) return null
    val (year, month, day) = isoDateOf(today).split('-').map { it.toInt() }
    val total = year * 12 + (month - 1) - months
    val targetYear = total / 12
    val targetMonth = total % 12 + 1
    // SQLite moves a day beyond the month's end into the next month
    val first = epochDayOf(
        "${targetYear.toString().padStart(4, '0')}-${targetMonth.toString().padStart(2, '0')}-01",
    ) ?: error("Invalid date")
    return isoDateOf(first + day - 1)
}

/** The filters of Grocy's journal pages. */
internal data class JournalFilter(
    val productId: Int? = null,
    val transactionType: StockTransactionType? = null,
    val locationId: Int? = null,
    val userId: Int? = null,
    val months: Int = JOURNAL_DEFAULT_MONTHS,
) {
    /** Filters besides the date range, which [JOURNAL_DEFAULT_MONTHS] does not count as one. */
    val isActive: Boolean
        get() = productId != null || transactionType != null || locationId != null ||
            userId != null || months != JOURNAL_DEFAULT_MONTHS
}

/** One booking of Grocy's journal table. */
internal data class JournalItem(
    val booking: StockLogEntry,
    val productName: String,
    /** Amount and stock unit, e.g. "-1 Piece". */
    val amount: String,
    val type: StockTransactionType?,
    val locationName: String?,
    val userName: String?,
)

/** What the master data names bookings with. */
internal class JournalNames(
    val products: List<Product>,
    val units: List<QuantityUnit>,
    val locations: List<Location>,
    val users: List<User>,
)

/**
 * The rows of Grocy's journal: bookings of existing products, filtered and searched by
 * [query] in product, location, user and note, newest first.
 */
internal fun buildJournalItems(
    bookings: List<StockLogEntry>,
    names: JournalNames,
    filter: JournalFilter,
    query: String,
    decimals: Int,
    decimalSeparator: Char,
    language: String,
): List<JournalItem> {
    val products = names.products.associateBy { it.id }
    val units = names.units.associateBy { it.id }
    val locations = names.locations.associateBy { it.id }
    val users = names.users.associateBy { it.id }
    val search = query.trim().lowercase()
    return bookings
        .filter { filter.productId == null || it.productId == filter.productId }
        .filter {
            filter.transactionType == null || it.transactionType == filter.transactionType.value
        }
        .filter { filter.locationId == null || it.locationId == filter.locationId }
        .filter { filter.userId == null || it.userId == filter.userId }
        .mapNotNull { booking ->
            val product = products[booking.productId] ?: return@mapNotNull null
            val unit = product.quIdStock?.let { units[it] }
            JournalItem(
                booking = booking,
                productName = product.name,
                amount = listOfNotNull(
                    formatDecimal(booking.amount, decimals, decimalSeparator),
                    unit?.nameFor(booking.amount, language),
                ).joinToString(" "),
                type = StockTransactionType.of(booking.transactionType),
                locationName = booking.locationId?.let { locations[it]?.name },
                userName = booking.userId?.let { users[it]?.displayName },
            )
        }
        .filter { item ->
            search.isEmpty() || listOfNotNull(
                item.productName,
                item.locationName,
                item.userName,
                item.booking.note,
            ).any { it.lowercase().contains(search) }
        }
        .sortedWith(
            compareByDescending<JournalItem> { it.booking.createdTimestamp }
                .thenByDescending { it.booking.id },
        )
}

/** One row of Grocy's journal summary: what a user booked of a product per type. */
internal data class JournalSummaryItem(
    val productId: Int,
    val productName: String,
    val type: StockTransactionType?,
    val userName: String?,
    val amount: String,
)

/**
 * Grocy's journal summary: the bookings that were not undone, summed per product, type and
 * user, filtered and searched like the journal; by product, type and user.
 */
internal fun buildJournalSummary(
    bookings: List<StockLogEntry>,
    names: JournalNames,
    filter: JournalFilter,
    query: String,
    decimals: Int,
    decimalSeparator: Char,
    language: String,
): List<JournalSummaryItem> {
    val products = names.products.associateBy { it.id }
    val units = names.units.associateBy { it.id }
    val users = names.users.associateBy { it.id }
    val search = query.trim().lowercase()
    return bookings
        .filter { !it.undone }
        .filter { filter.productId == null || it.productId == filter.productId }
        .filter {
            filter.transactionType == null || it.transactionType == filter.transactionType.value
        }
        .filter { filter.userId == null || it.userId == filter.userId }
        .groupBy { Triple(it.productId, it.transactionType, it.userId) }
        .mapNotNull { (key, group) ->
            val (productId, type, userId) = key
            val product = products[productId] ?: return@mapNotNull null
            val unit = product.quIdStock?.let { units[it] }
            val sum = group.sumOf { it.amount }
            JournalSummaryItem(
                productId = productId,
                productName = product.name,
                type = StockTransactionType.of(type),
                userName = userId?.let { users[it]?.displayName },
                amount = listOfNotNull(
                    formatDecimal(sum, decimals, decimalSeparator),
                    unit?.nameFor(sum, language),
                ).joinToString(" "),
            )
        }
        .filter { item ->
            search.isEmpty() ||
                listOfNotNull(item.productName, item.userName)
                    .any { it.lowercase().contains(search) }
        }
        .sortedWith(
            compareBy(
                { it.productName.lowercase() },
                { it.type?.ordinal ?: Int.MAX_VALUE },
                { it.userName?.lowercase().orEmpty() },
            ),
        )
}
