package com.patrickzedler.grocy.core.database

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ShoppingListDaoTest {

    private val database = createInMemoryGrocyDatabase()
    private val dao = database.shoppingListDao()

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun itemAndListKeepEveryPropertyThroughTheDatabase() = runTest {
        val item = ShoppingListItem(
            id = 5,
            listId = 2,
            productId = 1,
            note = "Low fat",
            amount = 6.0,
            quId = 2,
            done = true,
            userfields = mapOf("shop" to "A"),
        )
        val list = ShoppingList(2, "Hardware store", "<p>Cash</p>", mapOf("color" to null))

        dao.insertItems(listOf(item.toEntity()))
        dao.upsertList(list.toEntity())

        assertEquals(item, dao.observeItems().first().single().toModel())
        assertEquals(list, dao.observeList(2).first()?.toModel())
    }

    @Test
    fun pendingCheckOffIsOnlyForgottenWhileUnchanged() = runTest {
        dao.upsertPendingDone(PendingShoppingListDoneEntity(itemId = 5, done = false))

        // The user checked it off again after the first was sent
        dao.deletePendingDone(itemId = 5, done = true)
        assertEquals(1, dao.observePendingDoneCount().first())

        dao.deletePendingDone(itemId = 5, done = false)
        assertEquals(emptyList(), dao.pendingDone())
    }

    @Test
    fun itemsOfOneListAreDeletedTogether() = runTest {
        dao.insertItems(
            listOf(
                ShoppingListItem(1, 1, null, "a", 1.0, null, false).toEntity(),
                ShoppingListItem(2, 2, null, "b", 1.0, null, false).toEntity(),
            ),
        )

        dao.deleteItemsOfList(2)

        assertEquals(listOf(1), dao.observeItems().first().map { it.id })
    }
}
