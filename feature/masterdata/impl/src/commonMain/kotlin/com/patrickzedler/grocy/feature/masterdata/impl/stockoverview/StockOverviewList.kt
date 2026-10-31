package com.patrickzedler.grocy.feature.masterdata.impl.stockoverview

import androidx.compose.runtime.Immutable
import com.patrickzedler.grocy.core.forms.EntryPrice
import com.patrickzedler.grocy.core.forms.htmlToPlainText
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.stock.dueStatusOf
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.normalizedForSearch
import com.patrickzedler.grocy.feature.masterdata.impl.stock.priceToStockFactor
import com.patrickzedler.grocy.feature.masterdata.impl.stock.toStockFactor

/**
 * The status filter of Grocy's stock overview, which its colored messages above the table set
 * too, plus "not fresh" (due soon, overdue or expired) and "opened", which Grocy lacks.
 */
internal enum class StockStatus {
    NOT_FRESH,
    DUE_SOON,
    OVERDUE,
    EXPIRED,
    BELOW_MIN_STOCK,
    IN_STOCK,
    OPENED,
}

/** Grocy's filters of the stock overview; the defaults show every row. */
@Immutable
internal data class StockOverviewFilter(
    val status: StockStatus? = null,
    val locationId: Int? = null,
    val productGroupId: Int? = null,
) {
    val isActive: Boolean
        get() = this != StockOverviewFilter()
}

/** The sort order the list applies, after leaving out fields that are not shown. */
@Immutable
internal data class StockOverviewSort(
    val field: StockOverviewField = StockOverviewField.NAME,
    val ascending: Boolean = true,
)

/** A row of Grocy's stock overview with every column; the screen shows the chosen ones. */
@Immutable
internal data class StockOverviewItem(
    val productId: Int,
    val name: String,
    /** Own amount with the stock unit, e.g. "5 Glasses"; null for products without own stock. */
    val amount: String?,
    /** The opened part of [amount], null when nothing is opened. */
    val amountOpened: String?,
    /** Including sub products, e.g. "16 Packs"; only for parent products. */
    val amountAggregated: String?,
    val amountOpenedAggregated: String?,
    /** Grocy's "in stock": above 0 including sub products. */
    val isInStock: Boolean,
    val isBelowMinStock: Boolean,
    val onShoppingList: Boolean,
    /** Days since 1970-01-01 of the next due date, null when unknown or not tracked. */
    val dueDay: Long?,
    val neverOverdue: Boolean,
    val dueStatus: DueStatus?,
    val productGroupName: String?,
    /** What the stock cost, null without price tracking. */
    val value: Double?,
    /** Energy of one stock unit, e.g. "120 kcal"; null without. */
    val caloriesPerUnit: String?,
    /** Energy of the whole stock. */
    val calories: String?,
    /** The stock unit [caloriesPerUnit] refers to. */
    val stockUnitName: String,
    val lastPurchasedDay: Long?,
    val lastPrice: EntryPrice?,
    val averagePrice: EntryPrice?,
    /** With the stock unit, null when it is 0. */
    val minStockAmount: String?,
    val description: String?,
    val parentProductName: String?,
    val defaultLocationName: String?,
    val pictureFileName: String?,
    val defaultStoreName: String?,
    /** Grocy's quick consume amount in the consume unit, e.g. "1 Piece". */
    val quickConsume: String,
    val quickOpen: String,
    /** What the buttons of Grocy's row book, in the stock unit. */
    val quickConsumeAmount: Double,
    val quickOpenAmount: Double,
    /** What "Consume all" books (the tare weight with tare weight handling), in the stock unit. */
    val consumeAllAmount: Double,
    /** [consumeAllAmount] with the stock unit, e.g. "3 Packs". */
    val consumeAll: String,
    /** Where opening moves the product, Grocy's "Move on open"; null if it stays. */
    val openMovesTo: String?,
    val canConsume: Boolean,
    val canConsumeAll: Boolean,
    val canOpen: Boolean,
)

/** What names a group of rows; [None] collects the rows without a value. */
internal sealed interface StockOverviewGroupTitle {
    data class Text(val text: String) : StockOverviewGroupTitle

    data class Day(val epochDay: Long) : StockOverviewGroupTitle

    data object NeverOverdue : StockOverviewGroupTitle

    data object None : StockOverviewGroupTitle
}

/** Rows that share the value they are grouped by; one group without title when ungrouped. */
@Immutable
internal data class StockOverviewGroup(
    val title: StockOverviewGroupTitle?,
    val items: List<StockOverviewItem>,
)

/** Everything the stock overview is built from, as the database has it. */
internal data class StockOverviewData(
    val products: List<Product> = emptyList(),
    val productGroups: List<ProductGroup> = emptyList(),
    val locations: List<Location> = emptyList(),
    val quantityUnits: List<QuantityUnit> = emptyList(),
    val stores: List<Store> = emptyList(),
    val barcodes: List<ProductBarcode> = emptyList(),
    val stock: List<StockAmount> = emptyList(),
    val info: List<ProductStockInfo> = emptyList(),
    val stockLocations: List<StockLocationAmount> = emptyList(),
    val resolvedConversions: List<ResolvedQuantityUnitConversion> = emptyList(),
)

/** The rows as Grocy's page lists them, grouped, plus what its header and messages count. */
internal data class StockOverviewList(
    val groups: List<StockOverviewGroup>,
    /** Rows of each status, unfiltered, like Grocy's messages above the table. */
    val statusCounts: Map<StockStatus, Int>,
    /** Products in stock, Grocy's "%s Products". */
    val productCount: Int,
    /** What the whole stock cost, Grocy's "%s total value". */
    val totalValue: Double,
)

/** The fields the server's features allow, in Grocy's order; the name is always shown. */
internal fun availableFields(config: ServerConfig): List<StockOverviewField> =
    StockOverviewField.entries.filter { field ->
        when (field) {
            StockOverviewField.NAME -> false
            StockOverviewField.VALUE,
            StockOverviewField.LAST_PRICE,
            StockOverviewField.AVERAGE_PRICE,
            StockOverviewField.DEFAULT_STORE,
            -> config.stockPriceTracking
            StockOverviewField.NEXT_DUE_DATE -> config.stockBestBeforeDateTracking
            StockOverviewField.DEFAULT_LOCATION -> config.stockLocationTracking
            else -> true
        }
    }

/** The groupings of Grocy's "Table options" the server's features allow. */
internal fun availableGroupings(config: ServerConfig): List<StockOverviewGrouping> =
    StockOverviewGrouping.entries.filter { grouping ->
        when (grouping) {
            StockOverviewGrouping.NEXT_DUE_DATE -> config.stockBestBeforeDateTracking
            StockOverviewGrouping.DEFAULT_LOCATION -> config.stockLocationTracking
            StockOverviewGrouping.DEFAULT_STORE -> config.stockPriceTracking
            else -> true
        }
    }

/** The statuses the server's features allow, the due ones need due date tracking. */
internal fun availableStatuses(config: ServerConfig): List<StockStatus> =
    StockStatus.entries.filter { status ->
        when (status) {
            StockStatus.NOT_FRESH,
            StockStatus.DUE_SOON,
            StockStatus.OVERDUE,
            StockStatus.EXPIRED,
            -> config.stockBestBeforeDateTracking
            StockStatus.OPENED -> config.stockProductOpenedTracking
            else -> true
        }
    }

/** The fields the list can be sorted by: the name and every shown field but the picture. */
internal fun sortFields(shown: List<StockOverviewField>): List<StockOverviewField> =
    listOf(StockOverviewField.NAME) + shown.filter { it != StockOverviewField.PICTURE }

/**
 * Grocy's default order, by the next due date, or by name while the due date is not shown; the
 * order a hidden sort field falls back to.
 */
internal fun defaultSort(shown: List<StockOverviewField>): StockOverviewSort =
    if (StockOverviewField.NEXT_DUE_DATE in shown) {
        StockOverviewSort(StockOverviewField.NEXT_DUE_DATE)
    } else {
        StockOverviewSort(StockOverviewField.NAME)
    }

/** [field] while it can be sorted by, otherwise the [defaultSort]. */
internal fun effectiveSort(
    field: StockOverviewField,
    ascending: Boolean,
    shown: List<StockOverviewField>,
): StockOverviewSort =
    if (field in sortFields(shown)) StockOverviewSort(field, ascending) else defaultSort(shown)

/**
 * Grocy's stock overview: active products not hidden there that are in stock or below their
 * min. stock amount (every active one with the user setting to show all), filtered, searched
 * (by relevance then) or sorted, and grouped like its "Table options" group the table.
 */
internal fun buildStockOverview(
    data: StockOverviewData,
    filter: StockOverviewFilter,
    sort: StockOverviewSort,
    grouping: StockOverviewGrouping,
    query: String,
    config: ServerConfig,
    settings: UserSettings,
    today: Long,
    decimalSeparator: Char,
    language: String,
): StockOverviewList {
    val stockById = data.stock.associateBy { it.productId }
    val infoById = data.info.associateBy { it.productId }
    val productsById = data.products.associateBy { it.id }
    val context = RowContext(data, config, settings, today, decimalSeparator, language)

    val rows = data.products
        .filter { it.active && !it.hideOnStockOverview }
        .filter { product ->
            settings.stockOverviewShowAllOutOfStockProducts ||
                product.id in stockById ||
                infoById[product.id]?.amountMissing != null
        }
        .map { product ->
            Row(product, context.item(product, stockById[product.id], infoById[product.id]))
        }

    val statusCounts = availableStatuses(config).associateWith { status ->
        rows.count { it.item.hasStatus(status) }
    }
    val locationsByProduct = data.stockLocations
        .filter { it.amount > 0 }
        .groupBy({ it.productId }, { it.locationId })

    val filtered = rows.filter { row ->
        (filter.status == null || row.item.hasStatus(filter.status)) &&
            (filter.productGroupId == null ||
                row.product.productGroupId == filter.productGroupId) &&
            (filter.locationId == null ||
                filter.locationId in locationsByProduct[row.product.id].orEmpty())
    }
    val searched = search(filtered, query, data.barcodes)
    // The search orders by relevance, otherwise the chosen sort applies
    val ordered = if (query.isNotBlank()) {
        searched
    } else {
        searched.sortedWith(comparator(sort, stockById))
    }

    return StockOverviewList(
        groups = group(ordered, grouping, productsById),
        statusCounts = statusCounts,
        productCount = data.stock.count { stock ->
            productsById[stock.productId]?.hideOnStockOverview == false
        },
        totalValue = data.stock.sumOf { it.value },
    )
}

private class Row(val product: Product, val item: StockOverviewItem)

/** Builds the rows; what all of them need, looked up once. */
private class RowContext(
    private val data: StockOverviewData,
    private val config: ServerConfig,
    private val settings: UserSettings,
    private val today: Long,
    private val decimalSeparator: Char,
    private val language: String,
) {
    private val groups = data.productGroups.associateBy { it.id }
    private val locations = data.locations.associateBy { it.id }
    private val units = data.quantityUnits.associateBy { it.id }
    private val stores = data.stores.associateBy { it.id }
    private val products = data.products.associateBy { it.id }
    private val decimals = settings.stockDecimalPlacesAmounts

    fun item(product: Product, stock: StockAmount?, info: ProductStockInfo?): StockOverviewItem {
        val stockUnit = product.quIdStock?.let { units[it] }
        val amount = stock?.amount ?: 0.0
        val aggregated = stock?.amountAggregated ?: 0.0
        val openedAggregated = stock?.amountOpenedAggregated ?: 0.0
        val dueDate = stock?.bestBeforeDate?.takeIf { config.stockBestBeforeDateTracking }
        val neverOverdue = dueDate == StockEntry.NEVER_OVERDUE
        val dueDay = dueDate?.let(::epochDayOf)
        val priceFactor = priceToStockFactor(product, data.resolvedConversions)
        val priceUnit = (product.quIdPrice?.let { units[it] } ?: stockUnit)?.name.orEmpty()
        fun perPriceUnit(price: Double?) = price?.takeIf { config.stockPriceTracking }
            ?.let { EntryPrice(it * priceFactor, priceUnit) }
        val consumeFactor =
            toStockFactor(product, product.quIdConsume, data.resolvedConversions)
        val consumeUnit = product.quIdConsume?.let { units[it] } ?: stockUnit
        // Grocy's own amount; a parent without stock of its own consumes its sub products
        val consumeAll = when {
            product.enableTareWeightHandling -> product.tareWeight
            amount > 0 -> amount
            else -> aggregated
        }

        return StockOverviewItem(
            productId = product.id,
            name = product.name,
            amount = if (product.noOwnStock) null else amountOf(amount, stockUnit),
            amountOpened = stock?.amountOpened?.takeIf { it > 0 }?.let(::decimal),
            amountAggregated = if (stock?.isAggregatedAmount == true) {
                amountOf(aggregated, stockUnit)
            } else {
                null
            },
            amountOpenedAggregated = if (stock?.isAggregatedAmount == true) {
                openedAggregated.takeIf { it > 0 }?.let(::decimal)
            } else {
                null
            },
            isInStock = aggregated > 0,
            isBelowMinStock = info?.amountMissing != null,
            onShoppingList = info?.onShoppingList == true &&
                settings.showIconOnStockOverviewWhenOnShoppingList,
            dueDay = dueDay?.takeUnless { neverOverdue },
            neverOverdue = neverOverdue,
            // Like Grocy only products with stock of their own are due
            dueStatus = dueDay?.takeUnless { neverOverdue || amount <= 0 }?.let { day ->
                dueStatusOf(day, today, settings.stockDueSoonDays, product.dueType)
            },
            // Grocy only names active groups
            productGroupName = product.productGroupId?.let { groups[it] }
                ?.takeIf { it.active }?.name,
            value = (stock?.value ?: 0.0).takeIf { config.stockPriceTracking },
            caloriesPerUnit = product.calories?.takeIf { it > 0 }?.let(::energy),
            calories = product.calories?.takeIf { it > 0 && amount > 0 }
                ?.let { energy(it * amount) },
            stockUnitName = stockUnit?.name.orEmpty(),
            lastPurchasedDay = info?.lastPurchased?.let(::epochDayOf),
            lastPrice = perPriceUnit(info?.lastPrice),
            averagePrice = perPriceUnit(info?.averagePrice),
            minStockAmount = product.minStockAmount.takeIf { it != 0.0 }
                ?.let { amountOf(it, stockUnit) },
            description = product.description?.let(::htmlToPlainText)?.ifBlank { null },
            parentProductName = product.parentProductId?.let { products[it]?.name },
            defaultLocationName = product.locationId?.let { locations[it]?.name }
                ?.takeIf { config.stockLocationTracking },
            pictureFileName = product.pictureFileName?.takeIf { it.isNotBlank() },
            defaultStoreName = product.storeId?.let { stores[it]?.name }
                ?.takeIf { config.stockPriceTracking },
            quickConsume = amountOf(product.quickConsumeAmount / consumeFactor, consumeUnit),
            quickOpen = amountOf(product.quickOpenAmount / consumeFactor, consumeUnit),
            quickConsumeAmount = product.quickConsumeAmount,
            quickOpenAmount = product.quickOpenAmount,
            consumeAllAmount = consumeAll,
            consumeAll = amountOf(consumeAll, stockUnit),
            openMovesTo = product.defaultConsumeLocationId
                ?.takeIf { product.moveOnOpen && config.stockLocationTracking }
                ?.let { locations[it]?.name },
            // Grocy's buttons of the row: tare weight handling needs the consume page
            canConsume = aggregated >= product.quickConsumeAmount && aggregated > 0 &&
                !product.enableTareWeightHandling,
            canConsumeAll = aggregated > 0,
            canOpen = config.stockProductOpenedTracking &&
                aggregated >= product.quickOpenAmount && aggregated > 0 &&
                aggregated != openedAggregated &&
                !product.enableTareWeightHandling && !product.disableOpen,
        )
    }

    private fun decimal(value: Double) = formatDecimal(value, decimals, decimalSeparator)

    private fun amountOf(value: Double, unit: QuantityUnit?) =
        listOfNotNull(decimal(value), unit?.nameFor(value, language)).joinToString(" ")

    private fun energy(value: Double) =
        listOf(decimal(value), config.energyUnit).filter { it.isNotBlank() }.joinToString(" ")
}

/** Grocy's status column: due ones need stock of their own, the rest the amounts. */
internal fun StockOverviewItem.hasStatus(status: StockStatus): Boolean = when (status) {
    StockStatus.NOT_FRESH -> dueStatus != null
    StockStatus.DUE_SOON -> dueStatus == DueStatus.DUE_SOON
    StockStatus.OVERDUE -> dueStatus == DueStatus.OVERDUE
    StockStatus.EXPIRED -> dueStatus == DueStatus.EXPIRED
    StockStatus.BELOW_MIN_STOCK -> isBelowMinStock
    StockStatus.IN_STOCK -> isInStock
    StockStatus.OPENED -> amountOpened != null || amountOpenedAggregated != null
}

private fun search(rows: List<Row>, query: String, barcodes: List<ProductBarcode>): List<Row> {
    if (query.isBlank()) return rows
    val code = query.trim()
    val barcodeMatches = barcodes.filter { it.barcode == code }.map { it.productId }.toSet()
    val textMatches = FuzzySearch.filter(rows, query) { row ->
        listOfNotNull(
            row.item.name,
            row.item.productGroupName,
            row.item.defaultLocationName,
            row.item.defaultStoreName,
            row.item.parentProductName,
        )
    }
    return rows.filter { it.product.id in barcodeMatches } +
        textMatches.filter { it.product.id !in barcodeMatches }
}

private fun comparator(
    sort: StockOverviewSort,
    stockById: Map<Int, StockAmount>,
): Comparator<Row> {
    val byName = compareBy<Row> { it.item.name.normalizedForSearch() }
    fun directed(values: Comparator<Row>) = if (sort.ascending) values else values.reversed()
    // Empty values last in both directions, like a table sorted by text puts them together
    fun <T : Comparable<T>> emptyLast(selector: (Row) -> T?): Comparator<Row> =
        compareBy<Row> { selector(it) == null }
            .then(directed(compareBy { selector(it) }))
    fun text(selector: (Row) -> String?) =
        emptyLast { row -> selector(row)?.takeIf { it.isNotBlank() }?.normalizedForSearch() }

    val primary: Comparator<Row> = when (sort.field) {
        StockOverviewField.NAME -> directed(byName)
        StockOverviewField.PRODUCT_GROUP -> text { it.item.productGroupName }
        // Grocy sorts by the aggregated amount of products without own stock
        StockOverviewField.AMOUNT -> directed(
            compareBy { row ->
                val stock = stockById[row.product.id]
                if (row.product.noOwnStock) stock?.amountAggregated else stock?.amount
            },
        )
        StockOverviewField.VALUE -> directed(compareBy { stockById[it.product.id]?.value })
        // Grocy's placeholders sort unknown dates after the known ones, never overdue last
        StockOverviewField.NEXT_DUE_DATE -> directed(
            compareBy { row ->
                when {
                    row.item.neverOverdue -> epochDayOf(StockEntry.NEVER_OVERDUE)
                    else -> row.item.dueDay ?: epochDayOf(UNKNOWN_DUE_DATE)
                }
            },
        )
        StockOverviewField.CALORIES_PER_UNIT -> emptyLast { it.product.calories }
        StockOverviewField.CALORIES -> emptyLast { row ->
            row.product.calories?.let { it * (stockById[row.product.id]?.amount ?: 0.0) }
        }
        StockOverviewField.LAST_PURCHASED -> emptyLast { it.item.lastPurchasedDay }
        StockOverviewField.LAST_PRICE -> emptyLast { it.item.lastPrice?.price }
        StockOverviewField.MIN_STOCK_AMOUNT ->
            directed(compareBy { it.product.minStockAmount })
        StockOverviewField.DESCRIPTION -> text { it.item.description }
        StockOverviewField.PARENT_PRODUCT -> text { it.item.parentProductName }
        StockOverviewField.DEFAULT_LOCATION -> text { it.item.defaultLocationName }
        StockOverviewField.AVERAGE_PRICE -> emptyLast { it.item.averagePrice?.price }
        StockOverviewField.DEFAULT_STORE -> text { it.item.defaultStoreName }
        StockOverviewField.PICTURE -> byName
    }
    return primary.then(byName)
}

/**
 * The rows in groups of the value of [grouping], ordered by that value with the rows without
 * one last; the rows keep their order inside each group.
 */
private fun group(
    rows: List<Row>,
    grouping: StockOverviewGrouping,
    productsById: Map<Int, Product>,
): List<StockOverviewGroup> {
    if (grouping == StockOverviewGrouping.NONE) {
        return if (rows.isEmpty()) emptyList() else listOf(StockOverviewGroup(null, rows.items()))
    }
    val keyed = rows.groupBy { row -> groupKeyOf(row, grouping, productsById) }
    return keyed.entries
        .sortedWith(
            compareBy<Map.Entry<GroupKey, List<Row>>>(
                { it.key.order },
                { it.key.sortNumber },
                { it.key.sortText },
            ),
        )
        .map { (key, groupRows) -> StockOverviewGroup(key.title, groupRows.items()) }
}

private fun List<Row>.items() = map { it.item }

/** A group's title and where it goes: by its number or text, empty values last. */
private data class GroupKey(
    val title: StockOverviewGroupTitle,
    val sortNumber: Double? = null,
    val sortText: String? = null,
) {
    val order: Int
        get() = when (title) {
            StockOverviewGroupTitle.None -> 2
            StockOverviewGroupTitle.NeverOverdue -> 1
            else -> 0
        }
}

private fun groupKeyOf(
    row: Row,
    grouping: StockOverviewGrouping,
    productsById: Map<Int, Product>,
): GroupKey {
    val none = GroupKey(StockOverviewGroupTitle.None)
    fun text(value: String?) = value?.takeIf { it.isNotBlank() }
        ?.let { GroupKey(StockOverviewGroupTitle.Text(it), sortText = it.normalizedForSearch()) }
        ?: none
    fun day(value: Long?) = value
        ?.let { GroupKey(StockOverviewGroupTitle.Day(it), sortNumber = it.toDouble()) }
        ?: none
    val item = row.item
    return when (grouping) {
        StockOverviewGrouping.NONE -> none
        StockOverviewGrouping.PRODUCT_GROUP -> text(item.productGroupName)
        StockOverviewGrouping.NEXT_DUE_DATE -> if (item.neverOverdue) {
            GroupKey(StockOverviewGroupTitle.NeverOverdue)
        } else {
            day(item.dueDay)
        }
        StockOverviewGrouping.LAST_PURCHASED -> day(item.lastPurchasedDay)
        StockOverviewGrouping.MIN_STOCK_AMOUNT -> item.minStockAmount?.let { amount ->
            val value = productsById[item.productId]?.minStockAmount
            GroupKey(StockOverviewGroupTitle.Text(amount), sortNumber = value)
        } ?: none
        StockOverviewGrouping.PARENT_PRODUCT -> text(item.parentProductName)
        StockOverviewGrouping.DEFAULT_LOCATION -> text(item.defaultLocationName)
        StockOverviewGrouping.DEFAULT_STORE -> text(item.defaultStoreName)
    }
}

// Grocy's next due date of products without stock
private const val UNKNOWN_DUE_DATE = "2888-12-31"
