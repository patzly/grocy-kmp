package com.patrickzedler.grocy.core.model.shoppinglist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShoppingListSummaryTest {

    private fun item(id: Int, listId: Int, done: Boolean) =
        ShoppingListItem(id, listId, productId = null, note = "x", 1.0, null, done)

    @Test
    fun countsUndoneAndAllItemsPerListByName() {
        val summary = shoppingListSummaryOf(
            lists = listOf(ShoppingList(1, "Shopping list"), ShoppingList(2, "hardware store")),
            items = listOf(item(1, 1, false), item(2, 1, true), item(3, 2, false)),
        )

        assertEquals(
            listOf(
                ShoppingListCount(2, "hardware store", undoneCount = 1, itemCount = 1),
                ShoppingListCount(1, "Shopping list", undoneCount = 1, itemCount = 2),
            ),
            summary.lists,
        )
        assertEquals(2, summary.undoneCount)
        assertEquals("Shopping list", summary.defaultList?.name)
    }

    @Test
    fun withoutListsThereIsNoDefaultList() {
        val summary = shoppingListSummaryOf(emptyList(), emptyList())

        assertEquals(0, summary.undoneCount)
        assertNull(summary.defaultList)
    }
}
