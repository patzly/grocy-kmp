package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * The items of Grocy's shopping lists as last downloaded, plus the user's check offs the server
 * has not taken yet, which keep the list working offline.
 */
@Dao
interface ShoppingListDao {

    @Query("SELECT * FROM shopping_list_items ORDER BY id")
    fun observeItems(): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    fun observeItem(id: Int): Flow<ShoppingListItemEntity?>

    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingListItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItem(id: Int)

    @Query("UPDATE shopping_list_items SET done = :done WHERE id = :id")
    suspend fun setItemDone(id: Int, done: Boolean)

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    fun observeList(id: Int): Flow<ShoppingListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: ShoppingListEntity)

    /** Removes the list and its items, like Grocy's trigger does on the server. */
    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteList(id: Int)

    @Query("DELETE FROM shopping_list_items WHERE shopping_list_id = :listId")
    suspend fun deleteItemsOfList(listId: Int)

    @Query("SELECT * FROM pending_shopping_list_done")
    suspend fun pendingDone(): List<PendingShoppingListDoneEntity>

    @Query("SELECT * FROM pending_shopping_list_done")
    fun observePendingDone(): Flow<List<PendingShoppingListDoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingDone(pending: PendingShoppingListDoneEntity)

    /**
     * Forgets a check off the server took; only if the user did not change it again since, that
     * newer one still has to be sent.
     */
    @Query("DELETE FROM pending_shopping_list_done WHERE item_id = :itemId AND done = :done")
    suspend fun deletePendingDone(itemId: Int, done: Boolean)

    @Query("DELETE FROM pending_shopping_list_done WHERE item_id = :itemId")
    suspend fun deletePendingDoneOf(itemId: Int)
}
