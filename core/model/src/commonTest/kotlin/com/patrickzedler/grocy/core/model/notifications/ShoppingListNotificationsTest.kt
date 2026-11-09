package com.patrickzedler.grocy.core.model.notifications

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShoppingListNotificationsTest {

    private fun item(id: Int, done: Boolean = false) =
        ShoppingListItem(id, listId = 1, productId = null, note = "Item $id", 1.0, null, done)

    @Test
    fun aNewItemCountsAsAdded() {
        val changes = shoppingListChangesOf(listOf(item(1)), listOf(item(1), item(2)), emptySet())

        assertEquals(listOf(item(2)), changes.added)
        assertTrue(changes.checkedOff.isEmpty())
    }

    @Test
    fun anItemThatBecameDoneCountsAsCheckedOff() {
        val changes = shoppingListChangesOf(listOf(item(1)), listOf(item(1, done = true)), emptySet())

        assertEquals(listOf(item(1, done = true)), changes.checkedOff)
    }

    @Test
    fun aCheckOffStillToBeSentIsTheUsersOwn() {
        val changes = shoppingListChangesOf(listOf(item(1)), listOf(item(1, done = true)), setOf(1))

        assertTrue(changes.isEmpty)
    }

    @Test
    fun anItemAddedAlreadyDoneTellsNothing() {
        assertTrue(shoppingListChangesOf(emptyList(), listOf(item(1, done = true)), emptySet()).isEmpty)
    }

    @Test
    fun nothingNewTellsNothing() {
        assertTrue(shoppingListChangesOf(listOf(item(1)), listOf(item(1)), emptySet()).isEmpty)
    }

    @Test
    fun namesGrowWithoutRepeating() {
        val names = ShoppingListChangeNames(added = listOf("Milk")) +
            ShoppingListChangeNames(added = listOf("Milk", "Eggs"), checkedOff = listOf("Bread"))

        assertEquals(listOf("Milk", "Eggs"), names.added)
        assertEquals(listOf("Bread"), names.checkedOff)
    }

    @Test
    fun theStoredSettingsStayReadableAndAreOffByDefault() {
        val added = ShoppingListNotificationSettings(setOf(ShoppingListNotificationType.ADDED))

        assertEquals(added, ShoppingListNotificationSettings.of("ADDED"))
        assertEquals("ADDED", added.stored())
        assertFalse(ShoppingListNotificationSettings.of(null).isEnabled)
    }

    @Test
    fun theyOnlyApplyWhereTheServerHasShoppingLists() {
        val added = ShoppingListNotificationSettings(setOf(ShoppingListNotificationType.ADDED))

        assertTrue(added.appliesTo(ServerConfig()))
        assertFalse(added.appliesTo(ServerConfig(shoppingList = false)))
    }
}
