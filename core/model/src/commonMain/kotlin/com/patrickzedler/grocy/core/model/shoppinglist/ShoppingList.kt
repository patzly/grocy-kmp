package com.patrickzedler.grocy.core.model.shoppinglist

import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.formatDecimal
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * One of Grocy's shopping lists; Grocy's first list has the id [DEFAULT_ID] and cannot be
 * deleted. [description] is the list's "Notes" as HTML, edited on the shopping list page.
 */
data class ShoppingList(
    val id: Int,
    val name: String,
    val description: String? = null,
    val userfields: Map<String, String?> = emptyMap(),
) {
    companion object {
        const val DEFAULT_ID = 1
    }
}

/**
 * An item of a shopping list: a product, a note or both. Like Grocy, [amount] is always in the
 * product's stock unit; [quId] only chooses the unit it is shown in. Items without product
 * have a plain number as amount.
 */
data class ShoppingListItem(
    val id: Int,
    val listId: Int,
    val productId: Int?,
    val note: String?,
    val amount: Double,
    val quId: Int?,
    val done: Boolean,
    val userfields: Map<String, String?> = emptyMap(),
)

/** Where the user's own check off of an item stands while the server has not taken it yet. */
enum class ShoppingListItemSync {
    /** Being sent, or about to be. */
    SENDING,

    /** The last attempt failed (offline, the server down); it is sent again by itself. */
    FAILED,
}

/** How many items one list has, for summaries such as the start screen's. */
data class ShoppingListCount(
    val listId: Int,
    val name: String,
    /** Items not done yet. */
    val undoneCount: Int,
    val itemCount: Int,
)

/** The lists by name with their items, e.g. for the start screen. */
data class ShoppingListSummary(val lists: List<ShoppingListCount>) {
    /** Items not done yet on every list. */
    val undoneCount: Int
        get() = lists.sumOf { it.undoneCount }

    /** Grocy's first list, where everything goes without multiple lists. */
    val defaultList: ShoppingListCount?
        get() = lists.firstOrNull { it.listId == ShoppingList.DEFAULT_ID } ?: lists.firstOrNull()
}

/** The lists by name (case-insensitive like Grocy) with their counted [items]. */
fun shoppingListSummaryOf(
    lists: List<ShoppingList>,
    items: List<ShoppingListItem>,
): ShoppingListSummary {
    val byList = items.groupBy { it.listId }
    return ShoppingListSummary(
        lists.sortedBy { it.name.lowercase() }.map { list ->
            val listItems = byList[list.id].orEmpty()
            ShoppingListCount(
                listId = list.id,
                name = list.name,
                undoneCount = listItems.count { !it.done },
                itemCount = listItems.size,
            )
        },
    )
}

/** The amount of shopping list items as Grocy shows it: in the item's unit, by the user's settings. */
class ShoppingItemAmounts(
    products: List<Product>,
    quantityUnits: List<QuantityUnit>,
    conversions: List<ResolvedQuantityUnitConversion>,
    private val settings: UserSettings,
    private val language: String,
    private val decimalSeparator: Char,
) {
    private val products = products.associateBy { it.id }
    private val units = quantityUnits.associateBy { it.id }
    private val factors = conversions.associate {
        Triple<Int?, Int?, Int?>(it.productId, it.fromQuId, it.toQuId) to it.factor
    }

    /** Null for items without a product, whose amount is a plain number nobody chose. */
    fun of(item: ShoppingListItem): String? {
        val productId = item.productId ?: return null
        val decimals = settings.stockDecimalPlacesAmounts
        val product = products[productId]
        // The amount is stored in the stock unit, the item's unit only shows it
        val factor = product?.takeIf { item.quId != it.quIdStock }
            ?.let { factors[Triple(it.id, it.quIdStock, item.quId)] }
            ?: 1.0
        val scale = 10.0.pow(decimals)
        val exact = (item.amount * factor * scale).roundToLong() / scale
        val shown = if (settings.shoppingListRoundUp) ceil(exact) else exact
        return listOfNotNull(
            formatDecimal(shown, decimals, decimalSeparator),
            item.quId?.let { units[it] }?.nameFor(shown, language),
        ).joinToString(" ")
    }
}
