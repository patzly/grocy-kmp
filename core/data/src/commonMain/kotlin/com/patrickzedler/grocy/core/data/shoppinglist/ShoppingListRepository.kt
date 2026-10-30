package com.patrickzedler.grocy.core.data.shoppinglist

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.PendingShoppingListDoneEntity
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListSummary
import com.patrickzedler.grocy.core.model.shoppinglist.shoppingListSummaryOf
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Grocy's shopping lists and their items, offline first. Checking items off works offline: the
 * user's choice is stored at once and queued, and sent once the server can be reached (the next
 * check off, refresh, app start, the device going online or a retry while sending failed). Only
 * the item's `done` is sent and only while the item still exists, so what others changed in the
 * meantime stays. Every other change is online only.
 *
 * @param scope where queued check offs are sent, the app's scope, so leaving a screen never
 *   stops them.
 * @param connection the logged in server, null after a logout.
 * @param networkAvailable emits whenever the device gets a network, which sends the queue.
 * @param firstRetryDelay how long a check off that failed waits for the next attempt, doubled
 *   up to [MAX_RETRY_DELAY] while it keeps failing; null never retries by itself.
 */
class ShoppingListRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val scope: CoroutineScope,
    private val connection: suspend () -> ServerConnection?,
    networkAvailable: Flow<Unit> = emptyFlow(),
    firstRetryDelay: Duration? = FIRST_RETRY_DELAY,
) {

    private val dao = database.shoppingListDao()
    private val masterDataDao = database.masterDataDao()

    // Sending queued check offs and storing downloads one at a time, so a download never
    // overwrites a check off that is being sent
    private val mutex = Mutex()

    /** Every list, by name like Grocy's list selector. */
    val lists: Flow<List<ShoppingList>> =
        masterDataDao.observeShoppingLists().map { list -> list.map { it.toModel() } }

    /** Every item of every list, checked off as the user last chose here. */
    val items: Flow<List<ShoppingListItem>> =
        dao.observeItems().map { list -> list.map { it.toModel() } }

    // Items whose queued check off could not be sent the last time it was tried
    private val failedItemIds = MutableStateFlow(emptySet<Int>())

    /** The items whose check off the server has not taken yet, by id. */
    val syncStates: Flow<Map<Int, ShoppingListItemSync>> =
        combine(dao.observePendingDone(), failedItemIds) { pending, failed ->
            pending.associate { entry ->
                val state = if (entry.itemId in failed) {
                    ShoppingListItemSync.FAILED
                } else {
                    ShoppingListItemSync.SENDING
                }
                entry.itemId to state
            }
        }

    /** How many items each list has and how many are not done, e.g. for the start screen. */
    val summary: Flow<ShoppingListSummary> = combine(lists, items, ::shoppingListSummaryOf)

    private val mutableSelectedListId = MutableStateFlow<Int?>(null)

    /**
     * The list the shopping list shows, chosen there or just created (Grocy switches to a new
     * list); null until one was chosen in this process.
     */
    val selectedListId: StateFlow<Int?> = mutableSelectedListId.asStateFlow()

    init {
        scope.launch { networkAvailable.collect { sendPendingDoneInBackground() } }
        // Also where the network stays but the server was down, ever less often
        if (firstRetryDelay != null) {
            scope.launch { retryWhileFailing(firstRetryDelay) }
        }
    }

    private suspend fun retryWhileFailing(firstDelay: Duration) {
        failedItemIds.map { it.isNotEmpty() }.distinctUntilChanged().collectLatest { failing ->
            var wait = firstDelay
            while (failing) {
                delay(wait)
                sendPendingDoneInBackground()
                wait = (wait * 2).coerceAtMost(MAX_RETRY_DELAY)
            }
        }
    }

    fun selectList(listId: Int) {
        mutableSelectedListId.value = listId
    }

    fun item(id: Int): Flow<ShoppingListItem?> = dao.observeItem(id).map { it?.toModel() }

    fun list(id: Int): Flow<ShoppingList?> = dao.observeList(id).map { it?.toModel() }

    /**
     * Sends the queued check offs, then downloads the lists and items and replaces the stored
     * ones; check offs still queued stay as the user chose them.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline; the stored
     *   lists stay as they were.
     */
    suspend fun refresh(): Unit = mutex.withLock {
        val connection = requireConnection()
        sendPendingDone(connection)
        val (lists, items) = coroutineScope {
            val lists = async { api.shoppingLists(connection) }
            val items = async { api.shoppingListItems(connection) }
            lists.await() to items.await()
        }
        database.withWriteTransaction {
            masterDataDao.deleteShoppingLists()
            masterDataDao.insertShoppingLists(lists.map { it.toEntity() })
            storeItems(items)
        }
    }

    /**
     * Checks the item off ([done]) or on again, offline too: stored at once, sent in the
     * background, queued until the server can be reached.
     */
    suspend fun setDone(itemId: Int, done: Boolean) {
        database.withWriteTransaction {
            dao.setItemDone(itemId, done)
            dao.upsertPendingDone(PendingShoppingListDoneEntity(itemId, done))
        }
        // A new choice is being sent, whatever happened to the last one
        failedItemIds.update { it - itemId }
        sendPendingDoneInBackground()
    }

    /** Sends the queued check offs in the background, e.g. at app start; offline they wait. */
    fun sendPendingDoneInBackground() {
        scope.launch {
            ignoringServerErrors {
                val sent = mutex.withLock { sendPendingDone(requireConnection()) }
                // What others changed meanwhile shows too
                if (sent) refreshItems()
            }
        }
    }

    /**
     * Replays the user's own check offs against the server's current state: each item gets the
     * done state the user chose; an item that is gone (Grocy refuses it) drops its check off.
     * Returns whether anything was sent.
     */
    private suspend fun sendPendingDone(connection: ServerConnection): Boolean {
        val pending = dao.pendingDone()
        for ((index, entry) in pending.withIndex()) {
            try {
                api.setShoppingListItemDone(connection, entry.itemId, entry.done)
            } catch (e: GrocyApiException) {
                // Rejected for good, e.g. removed by someone else; the server may recover
                if (e !is GrocyApiException.HttpError || e.statusCode >= HTTP_SERVER_ERROR) {
                    failedItemIds.update { failed ->
                        failed + pending.drop(index).map { it.itemId }
                    }
                    throw e
                }
            }
            dao.deletePendingDone(entry.itemId, entry.done)
            failedItemIds.update { it - entry.itemId }
        }
        return pending.isNotEmpty()
    }

    /** Downloads the items again, e.g. after a change on the server. */
    private suspend fun refreshItems(): Unit = mutex.withLock {
        val items = api.shoppingListItems(requireConnection())
        database.withWriteTransaction { storeItems(items) }
    }

    /** Replaces the stored items by [items] with the check offs still to send on top. */
    private suspend fun storeItems(items: List<ShoppingListItem>) {
        val pending = dao.pendingDone().associate { it.itemId to it.done }
        dao.deleteItems()
        dao.insertItems(
            items.map { item -> item.copy(done = pending[item.id] ?: item.done).toEntity() },
        )
    }

    /**
     * Creates the item (id [MasterDataRepository.NEW_OBJECT_ID]) or saves it like Grocy's item
     * form, then its changed [userfields]; stores what the server saved and returns its id.
     * Online only, like every following change.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun saveItem(item: ShoppingListItem, userfields: Map<String, String?>): Int {
        val connection = requireConnection()
        val id = api.saveShoppingListItem(connection, item)
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, MasterDataRepository.SHOPPING_LIST, id, userfields)
        }
        val saved = api.shoppingListItem(connection, id)
        mutex.withLock {
            val pending = dao.pendingDone().firstOrNull { it.itemId == id }
            dao.upsertItem(saved.copy(done = pending?.done ?: saved.done).toEntity())
        }
        return id
    }

    /**
     * Removes the item, like Grocy's delete button and like its purchase does with an item
     * added to the stock from the list.
     */
    suspend fun deleteItem(itemId: Int) {
        api.deleteObject(requireConnection(), MasterDataRepository.SHOPPING_LIST, itemId)
        database.withWriteTransaction {
            dao.deleteItem(itemId)
            dao.deletePendingDoneOf(itemId)
        }
        failedItemIds.update { it - itemId }
    }

    /**
     * Puts [amount] (in the stock unit) of the product on list [listId], shown in unit [quId],
     * like Grocy's "Add to shopping list": an item of the product on that list gets the amount
     * added. Then saves the item's changed [userfields], which Grocy itself forgets.
     */
    suspend fun addProduct(
        productId: Int,
        listId: Int,
        amount: Double,
        quId: Int?,
        note: String?,
        userfields: Map<String, String?>,
    ) {
        val connection = requireConnection()
        val itemId = api.addProductToShoppingList(connection, productId, listId, amount, quId, note)
        if (itemId != null && userfields.isNotEmpty()) {
            api.setUserfields(connection, MasterDataRepository.SHOPPING_LIST, itemId, userfields)
        }
        afterChange()
    }

    /** Grocy's "Clear list", or "Clear done items" with [doneOnly]. */
    suspend fun clear(listId: Int, doneOnly: Boolean) {
        api.clearShoppingList(requireConnection(), listId, doneOnly)
        afterChange()
    }

    /** Grocy's "Add products that are below defined min. stock amount". */
    suspend fun addMissingProducts(listId: Int) {
        api.addMissingProductsToShoppingList(requireConnection(), listId)
        afterChange()
    }

    /** Grocy's "Add overdue/expired products": both of its calls in its order. */
    suspend fun addOverdueAndExpiredProducts(listId: Int) {
        val connection = requireConnection()
        api.addOverdueProductsToShoppingList(connection, listId)
        api.addExpiredProductsToShoppingList(connection, listId)
        afterChange()
    }

    /**
     * Creates the list (id [MasterDataRepository.NEW_OBJECT_ID]) or renames it, then saves its
     * changed [userfields]; returns its id.
     */
    suspend fun saveList(list: ShoppingList, userfields: Map<String, String?>): Int {
        val connection = requireConnection()
        val id = api.saveShoppingList(connection, list)
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, SHOPPING_LISTS, id, userfields)
        }
        dao.upsertList(api.shoppingList(connection, id).toEntity())
        return id
    }

    /** Deletes the list with its items, like Grocy's trigger does on the server. */
    suspend fun deleteList(listId: Int) {
        api.deleteObject(requireConnection(), SHOPPING_LISTS, listId)
        database.withWriteTransaction {
            dao.deleteList(listId)
            dao.deleteItemsOfList(listId)
        }
    }

    /** Saves the list's "Notes" as HTML. */
    suspend fun saveNotes(listId: Int, html: String) {
        val connection = requireConnection()
        api.saveShoppingListDescription(connection, listId, html)
        dao.upsertList(api.shoppingList(connection, listId).toEntity())
    }

    /** Prints the list on the server's thermal printer. */
    suspend fun print(listId: Int, printHeader: Boolean) {
        api.printShoppingList(requireConnection(), listId, printHeader)
    }

    /**
     * Downloads what a change on the server changed. The change is done once the server took
     * it, so a failed download must not report it as failed, which would invite repeating it.
     */
    private suspend fun afterChange() {
        ignoringServerErrors { refreshItems() }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    companion object {
        /** Grocy's entity of the lists, e.g. of their userfields. */
        const val SHOPPING_LISTS = "shopping_lists"

        private const val HTTP_SERVER_ERROR = 500

        // Retries of check offs that failed while the device kept its network
        val FIRST_RETRY_DELAY = 15.seconds
        val MAX_RETRY_DELAY = 5.minutes
    }
}

/** Runs [block]; a server that cannot be reached or refuses leaves things for later. */
private suspend fun ignoringServerErrors(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: GrocyApiException) {
        // The next refresh or check off tries again
    }
}
