package com.patrickzedler.grocy.core.model.shoppinglist

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
