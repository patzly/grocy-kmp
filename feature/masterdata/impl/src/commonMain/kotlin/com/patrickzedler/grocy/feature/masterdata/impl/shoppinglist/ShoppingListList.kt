package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.runtime.Immutable
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.text.FuzzySearch
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.normalizedForSearch
import com.patrickzedler.grocy.feature.masterdata.impl.stock.EntryPrice
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Grocy's status filter of the shopping list: its products below their min. stock amount, only
 * done or only undone items.
 */
internal enum class ShoppingListStatus { BELOW_MIN_STOCK, DONE, UNDONE }

/** What a group of rows is named by; the rows without a value come last. */
internal sealed interface ShoppingListGroupTitle {
    data class Text(val text: String) : ShoppingListGroupTitle

    /** Items without product group, Grocy's "Ungrouped". */
    data object Ungrouped : ShoppingListGroupTitle

    /** Items without the other value they are grouped by, the default store. */
    data object NotSet : ShoppingListGroupTitle
}

/** What adding an item to the stock books: its amount in the stock unit, shown in [quId]. */
@Immutable
internal data class ShoppingListPurchase(
    val itemId: Int,
    val productId: Int,
    val amount: Double,
    val quId: Int?,
)

/** A row of Grocy's shopping list table with every column; the screen shows the chosen ones. */
@Immutable
internal data class ShoppingListRow(
    val id: Int,
    val productId: Int?,
    /** The product's name, or the note of an item without product. */
    val title: String,
    /** The note below the product's name, null for items without product. */
    val note: String?,
    /** In the item's unit, e.g. "2 Packs"; the plain number for items without product. */
    val amount: String,
    val done: Boolean,
    val isBelowMinStock: Boolean,
    val productGroupName: String?,
    /** The last price per unit of the amount, null without price tracking or purchase. */
    val lastPriceUnit: EntryPrice?,
    /** What the amount cost last time. */
    val lastPriceTotal: Double?,
    val defaultStoreName: String?,
    val barcodes: List<String>,
    /** Caption and value of the userfields Grocy shows as columns, the item's and product's. */
    val userfields: List<Pair<String, String>>,
    /** Null for items without product, which cannot be added to the stock. */
    val purchase: ShoppingListPurchase?,
)

@Immutable
internal data class ShoppingListGroup(
    val title: ShoppingListGroupTitle?,
    val rows: List<ShoppingListRow>,
)

/** Everything the shopping list is built from, as the database has it. */
internal data class ShoppingListData(
    val items: List<ShoppingListItem> = emptyList(),
    val products: List<Product> = emptyList(),
    val productGroups: List<ProductGroup> = emptyList(),
    val quantityUnits: List<QuantityUnit> = emptyList(),
    val stores: List<Store> = emptyList(),
    val barcodes: List<ProductBarcode> = emptyList(),
    val resolvedConversions: List<ResolvedQuantityUnitConversion> = emptyList(),
    val info: List<ProductStockInfo> = emptyList(),
    val userfields: List<Userfield> = emptyList(),
)

/** The rows of one list as Grocy's page shows them, grouped, plus what it counts. */
internal data class ShoppingListContent(
    val groups: List<ShoppingListGroup>,
    /** Items of each status on the list, unfiltered, the status filter's numbers. */
    val statusCounts: Map<ShoppingListStatus, Int>,
    val itemCount: Int,
    /** What the list's items cost last time, Grocy's "%s total value". */
    val totalValue: Double,
)

/** The fields the server's features allow, in Grocy's order. */
internal fun availableShoppingListFields(config: ServerConfig): List<ShoppingListField> =
    ShoppingListField.entries.filter { field ->
        when (field) {
            ShoppingListField.LAST_PRICE_UNIT,
            ShoppingListField.LAST_PRICE_TOTAL,
            ShoppingListField.DEFAULT_STORE,
            -> config.stockPriceTracking
            else -> true
        }
    }

/** The groupings of Grocy's "Table options" the server's features allow. */
internal fun availableShoppingListGroupings(config: ServerConfig): List<ShoppingListGrouping> =
    ShoppingListGrouping.entries.filter {
        it != ShoppingListGrouping.DEFAULT_STORE || config.stockPriceTracking
    }

/** The statuses of the filter; below min. stock amount needs the stock. */
internal fun availableShoppingListStatuses(config: ServerConfig): List<ShoppingListStatus> =
    ShoppingListStatus.entries.filter { it != ShoppingListStatus.BELOW_MIN_STOCK || config.stock }

/**
 * Grocy's shopping list [listId]: every item, done ones too, by product name (or note), in
 * groups of [grouping]; filtered by [status] and searched in every column, like Grocy's table.
 */
internal fun buildShoppingList(
    data: ShoppingListData,
    listId: Int,
    status: ShoppingListStatus?,
    query: String,
    grouping: ShoppingListGrouping,
    config: ServerConfig,
    settings: UserSettings,
    decimalSeparator: Char,
    language: String,
): ShoppingListContent {
    val context = RowContext(data, config, settings, decimalSeparator, language)
    val items = data.items.filter { it.listId == listId }
    val rows = items.map(context::row)
        .sortedWith(compareBy({ it.title.normalizedForSearch() }, { it.id }))
    val statusCounts = availableShoppingListStatuses(config).associateWith { candidate ->
        rows.count { it.hasStatus(candidate) }
    }
    val filtered = rows.filter { status == null || it.hasStatus(status) }
    return ShoppingListContent(
        groups = group(search(filtered, query), grouping),
        statusCounts = statusCounts,
        itemCount = items.size,
        // The list's own items only, unlike Grocy, whose request sums every list
        totalValue = items.sumOf { item ->
            item.productId?.let { context.lastPrice(it) }?.let { it * item.amount } ?: 0.0
        },
    )
}

internal fun ShoppingListRow.hasStatus(status: ShoppingListStatus): Boolean = when (status) {
    ShoppingListStatus.BELOW_MIN_STOCK -> isBelowMinStock
    ShoppingListStatus.DONE -> done
    ShoppingListStatus.UNDONE -> !done
}

/** Builds the rows; what all of them need, looked up once. */
private class RowContext(
    data: ShoppingListData,
    private val config: ServerConfig,
    private val settings: UserSettings,
    private val decimalSeparator: Char,
    private val language: String,
) {
    private val products = data.products.associateBy { it.id }
    private val groups = data.productGroups.associateBy { it.id }
    private val units = data.quantityUnits.associateBy { it.id }
    private val stores = data.stores.associateBy { it.id }
    private val info = data.info.associateBy { it.productId }
    private val barcodes = data.barcodes.groupBy({ it.productId }, { it.barcode })
    private val resolved = data.resolvedConversions
    private val quantityUnits = data.quantityUnits
    private val itemFields = data.userfields.filter {
        it.entity == MasterDataRepository.SHOPPING_LIST && it.showAsColumnInTables
    }
    private val productFields = data.userfields.filter {
        it.entity == MasterDataRepository.PRODUCTS && it.showAsColumnInTables
    }

    /** The last price per stock unit of the product, null without price tracking. */
    fun lastPrice(productId: Int): Double? =
        info[productId]?.lastPrice?.takeIf { config.stockPriceTracking }

    fun row(item: ShoppingListItem): ShoppingListRow {
        val product = item.productId?.let { products[it] }
        if (product == null) {
            return ShoppingListRow(
                id = item.id,
                productId = null,
                title = item.note.orEmpty(),
                note = null,
                amount = decimal(item.amount),
                done = item.done,
                isBelowMinStock = false,
                productGroupName = null,
                lastPriceUnit = null,
                lastPriceTotal = null,
                defaultStoreName = null,
                barcodes = emptyList(),
                userfields = userfields(itemFields, item.userfields),
                purchase = null,
            )
        }
        // The amount is in the stock unit, the item's unit only shows it
        val factor = itemUnits(product, resolved, quantityUnits)
            .firstOrNull { it.unit.id == item.quId }?.factor ?: 1.0
        // Rounded like it is shown, so the unit's name fits the number (1.0000001 is one)
        val exact = roundTo(item.amount * factor, settings.stockDecimalPlacesAmounts)
        val shown = if (settings.shoppingListRoundUp) ceil(exact) else exact
        val unit = item.quId?.let { units[it] }
        val price = lastPrice(product.id)
        return ShoppingListRow(
            id = item.id,
            productId = product.id,
            title = product.name,
            note = item.note,
            amount = listOfNotNull(decimal(shown), unit?.nameFor(shown, language))
                .joinToString(" "),
            done = item.done,
            isBelowMinStock = config.stock && info[product.id]?.amountMissing != null,
            productGroupName = product.productGroupId?.let { groups[it]?.name },
            lastPriceUnit = price?.let { EntryPrice(it / factor, unit?.name.orEmpty()) },
            // Per stock unit times the stored amount, unlike Grocy's rounded up quirk
            lastPriceTotal = price?.let { it * item.amount },
            defaultStoreName = product.storeId?.let { stores[it]?.name }
                ?.takeIf { config.stockPriceTracking },
            barcodes = barcodes[product.id].orEmpty(),
            userfields = userfields(itemFields, item.userfields) +
                userfields(productFields, product.userfields),
            purchase = ShoppingListPurchase(item.id, product.id, item.amount, item.quId)
                .takeIf { config.stock },
        )
    }

    private fun decimal(value: Double) =
        formatDecimal(value, settings.stockDecimalPlacesAmounts, decimalSeparator)
}

private fun roundTo(value: Double, decimals: Int): Double {
    val scale = 10.0.pow(decimals)
    return (value * scale).roundToLong() / scale
}

/** The shown userfields with a value, as caption and text. */
private fun userfields(
    fields: List<Userfield>,
    values: Map<String, String?>,
): List<Pair<String, String>> = fields.mapNotNull { field ->
    userfieldText(field, values[field.name])?.let { field.caption to it }
}

/** A userfield's value as text like Grocy's table cell; null when it shows nothing. */
internal fun userfieldText(field: Userfield, value: String?): String? {
    if (value.isNullOrBlank()) return null
    return when (field.type) {
        // Grocy shows a check mark, the chip names the field
        UserfieldType.CHECKBOX -> field.caption.takeIf { value == "1" }
        UserfieldType.LINK_WITH_TITLE -> LINK_TITLE.find(value)?.groupValues?.get(1)
            ?.takeIf { it.isNotBlank() } ?: value
        UserfieldType.PRESET_CHECKLIST -> value.split(",").joinToString(", ")
        // Files and pictures need their own view
        UserfieldType.FILE, UserfieldType.IMAGE -> null
        else -> value
    }
}

private val LINK_TITLE = Regex(""""title"\s*:\s*"([^"]*)"""")

private fun search(rows: List<ShoppingListRow>, query: String): List<ShoppingListRow> {
    if (query.isBlank()) return rows
    val code = query.trim()
    val matches = FuzzySearch.filter(rows, query) { row ->
        listOfNotNull(row.title, row.note, row.productGroupName, row.defaultStoreName) +
            row.userfields.map { it.second }
    }.map { it.id }.toSet()
    // Grocy's table keeps its order while searching
    return rows.filter { it.id in matches || code in it.barcodes }
}

/** The rows in groups of [grouping] by name, the rows without a value last. */
private fun group(
    rows: List<ShoppingListRow>,
    grouping: ShoppingListGrouping,
): List<ShoppingListGroup> {
    if (rows.isEmpty()) return emptyList()
    if (grouping == ShoppingListGrouping.NONE) return listOf(ShoppingListGroup(null, rows))
    return rows.groupBy { row ->
        val value = when (grouping) {
            ShoppingListGrouping.PRODUCT_GROUP -> row.productGroupName
            ShoppingListGrouping.DEFAULT_STORE -> row.defaultStoreName
            ShoppingListGrouping.NONE -> null
        }
        when {
            !value.isNullOrBlank() -> ShoppingListGroupTitle.Text(value)
            grouping == ShoppingListGrouping.PRODUCT_GROUP -> ShoppingListGroupTitle.Ungrouped
            else -> ShoppingListGroupTitle.NotSet
        }
    }
        .map { (title, groupRows) -> ShoppingListGroup(title, groupRows) }
        // Unlike Grocy, which sorts "Ungrouped" among the names, the rest comes last
        .sortedWith(
            compareBy<ShoppingListGroup> { it.title !is ShoppingListGroupTitle.Text }
                .thenBy { (it.title as? ShoppingListGroupTitle.Text)?.text?.normalizedForSearch() },
        )
}
