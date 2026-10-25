package com.patrickzedler.grocy.core.data.stock

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What only the server knows about stock, offline first: its entries and the stock journal as
 * last downloaded. Bookings (consume, open, edit, undo) are online only and never calculated
 * locally; the entries and amounts are downloaded again after each.
 *
 * @param connection the logged in server, null after a logout.
 */
class StockRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
) {

    private val stockDao = database.stockDao()
    private val masterDataDao = database.masterDataDao()

    /** Every product's stock entries as last downloaded. */
    val stockEntries: Flow<List<StockEntry>> =
        stockDao.observeStockEntries().map { list -> list.map { it.toModel() } }

    /** The stored bookings since [since] ("YYYY-MM-DD", "" for all), newest first. */
    fun stockLog(since: String): Flow<List<StockLogEntry>> =
        stockDao.observeStockLog(since).map { list -> list.map { it.toModel() } }

    /**
     * Downloads every stock entry and the stock amounts of the products, which change together.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refreshStockEntries() {
        val connection = requireConnection()
        val (entries, amounts) = coroutineScope {
            val entries = async { api.stockEntries(connection) }
            val amounts = async { api.stock(connection) }
            entries.await() to amounts.await()
        }
        database.withWriteTransaction {
            stockDao.deleteStockEntries()
            stockDao.insertStockEntries(entries.map { it.toEntity() })
            masterDataDao.deleteStock()
            masterDataDao.insertStock(amounts.map { it.toEntity() })
        }
    }

    /**
     * Downloads the bookings since [since] ("YYYY-MM-DD", all without) and replaces the stored
     * ones of that time.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refreshStockLog(since: String?) {
        val bookings = api.stockLog(requireConnection(), since)
        database.withWriteTransaction {
            stockDao.deleteStockLog(since.orEmpty())
            stockDao.insertStockLog(bookings.map { it.toEntity() })
        }
    }

    /**
     * Consumes the whole [entry], as spoiled if [spoiled]; returns the booking to undo it.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun consume(entry: StockEntry, spoiled: Boolean): Int? =
        book { api.consumeStockEntry(it, entry, spoiled) }

    /** Marks the whole [entry] as opened; returns the booking to undo it. */
    suspend fun open(entry: StockEntry): Int? = book { api.openStockEntry(it, entry) }

    /**
     * Saves [entry] like Grocy's "Edit stock entry" with its changed [userfields]; returns the
     * booking to undo it.
     */
    suspend fun edit(entry: StockEntry, userfields: Map<String, String?>): Int? = book {
        val booking = api.editStockEntry(it, entry)
        if (userfields.isNotEmpty()) {
            api.setUserfields(it, MasterDataRepository.STOCK, entry.stockId, userfields)
        }
        booking
    }

    /** Undoes the booking [bookingId] with the others of its action. */
    suspend fun undo(bookingId: Int) {
        book { api.undoBooking(it, bookingId) }
    }

    /**
     * Puts [amount] (in the stock unit) of the product on shopping list [listId], shown in unit
     * [quId], like Grocy's "Add to shopping list", then saves the item's changed [userfields].
     */
    suspend fun addToShoppingList(
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
    }

    /** Prints the product's grocycode on the server's label printer. */
    suspend fun printProductLabel(productId: Int) {
        api.printProductLabel(requireConnection(), productId)
    }

    /** Prints the stock entry's label on the server's label printer. */
    suspend fun printStockEntryLabel(entryId: Int) {
        api.printStockEntryLabel(requireConnection(), entryId)
    }

    /** Runs a booking, then downloads the entries and amounts it changed. */
    private suspend fun <T> book(block: suspend (ServerConnection) -> T): T {
        val result = block(requireConnection())
        refreshStockEntries()
        return result
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()
}
