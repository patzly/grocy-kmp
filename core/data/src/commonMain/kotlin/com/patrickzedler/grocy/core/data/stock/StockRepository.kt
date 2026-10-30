package com.patrickzedler.grocy.core.data.stock

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.stock.BookingResult
import com.patrickzedler.grocy.core.model.stock.ConsumeBooking
import com.patrickzedler.grocy.core.model.stock.InventoryBooking
import com.patrickzedler.grocy.core.model.stock.OpenBooking
import com.patrickzedler.grocy.core.model.stock.ProductLocationAmount
import com.patrickzedler.grocy.core.model.stock.ProductPurchase
import com.patrickzedler.grocy.core.model.stock.ProductStockDetails
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.PurchaseBooking
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockLogEntry
import com.patrickzedler.grocy.core.model.stock.TransferBooking
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
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

    /** What the stock overview shows of the products besides their amounts. */
    val productStockInfo: Flow<List<ProductStockInfo>> =
        stockDao.observeProductStockInfo().map { list -> list.map { it.toModel() } }

    /** Every product's amount per location. */
    val stockLocations: Flow<List<StockLocationAmount>> =
        stockDao.observeStockLocations().map { list -> list.map { it.toModel() } }

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
     * Downloads what Grocy's stock overview shows besides the master data: the stock amounts,
     * the products below their min. stock amount, last purchases, average prices, the amounts
     * per location and the products on shopping lists, each as the server computed it.
     *
     * @param withShoppingLists false when the server has no shopping lists.
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refreshOverview(withShoppingLists: Boolean) {
        val connection = requireConnection()
        val download = coroutineScope {
            val amounts = async { api.stock(connection) }
            val missing = async { api.missingProducts(connection) }
            val purchases = async { api.lastPurchases(connection) }
            val averagePrices = async { api.averagePrices(connection) }
            val locations = async { api.stockLocations(connection) }
            val onShoppingLists = async {
                if (withShoppingLists) api.productsOnShoppingLists(connection) else emptySet()
            }
            OverviewDownload(
                amounts = amounts.await(),
                info = productStockInfoOf(
                    missing = missing.await(),
                    purchases = purchases.await(),
                    averagePrices = averagePrices.await(),
                    onShoppingLists = onShoppingLists.await(),
                ),
                locations = locations.await(),
            )
        }
        database.withWriteTransaction {
            masterDataDao.deleteStock()
            masterDataDao.insertStock(download.amounts.map { it.toEntity() })
            stockDao.deleteProductStockInfo()
            stockDao.insertProductStockInfo(download.info.map { it.toEntity() })
            stockDao.deleteStockLocations()
            stockDao.insertStockLocations(download.locations.map { it.toEntity() })
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
     * Consumes [amount] (in the stock unit) of a product like the buttons of Grocy's stock
     * overview, as spoiled if [spoiled]; returns the transaction to undo it. The overview is
     * downloaded again afterwards ([withShoppingLists] like [refreshOverview]).
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun consumeProduct(
        productId: Int,
        amount: Double,
        spoiled: Boolean,
        withShoppingLists: Boolean,
    ): String? = bookOnOverview(withShoppingLists) {
        api.consumeProduct(it, productId, amount, spoiled)
    }

    /** Marks [amount] (in the stock unit) of a product as opened; returns its transaction. */
    suspend fun openProduct(productId: Int, amount: Double, withShoppingLists: Boolean): String? =
        bookOnOverview(withShoppingLists) { api.openProduct(it, productId, amount) }

    /** Undoes every booking of the transaction [transactionId]. */
    suspend fun undoTransaction(transactionId: String, withShoppingLists: Boolean) {
        bookOnOverview(withShoppingLists) { api.undoTransaction(it, transactionId) }
    }

    /**
     * What Grocy's stock pages read of a product from the server, online only (the amounts
     * change with every booking of anyone).
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun productStockDetails(productId: Int): ProductStockDetails =
        api.productStockDetails(requireConnection(), productId)

    /** The product's stock per location, see [GrocyApi.productLocationAmounts]. */
    suspend fun productLocationAmounts(
        productId: Int,
        includeSubProducts: Boolean,
    ): List<ProductLocationAmount> =
        api.productLocationAmounts(requireConnection(), productId, includeSubProducts)

    /** The product's stock entries in Grocy's consume order. */
    suspend fun productStockEntries(productId: Int, includeSubProducts: Boolean): List<StockEntry> =
        api.productStockEntries(requireConnection(), productId, includeSubProducts)

    /** Grocy's normal recipes, id to name, for the statistics of consumed products. */
    suspend fun recipes(): Map<Int, String> = api.recipes(requireConnection())

    /**
     * Books Grocy's "Purchase" of the product, then the [userfields] of the new stock entries
     * (Grocy moves values saved for the transaction to them).
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun purchase(
        productId: Int,
        booking: PurchaseBooking,
        userfields: Map<String, String?>,
    ): BookingResult = book {
        val result = api.purchaseProduct(it, productId, booking)
        saveTransactionUserfields(it, result, userfields)
        result
    }

    /** Books Grocy's "Consume"; see [purchase]. */
    suspend fun consume(productId: Int, booking: ConsumeBooking): BookingResult =
        book { api.consumeProduct(it, productId, booking) }

    /** Books Grocy's "Mark as opened" of the consume page; see [purchase]. */
    suspend fun open(productId: Int, booking: OpenBooking): BookingResult =
        book { api.openProduct(it, productId, booking) }

    /** Books Grocy's "Transfer"; see [purchase]. */
    suspend fun transfer(productId: Int, booking: TransferBooking): BookingResult =
        book { api.transferProduct(it, productId, booking) }

    /** Books Grocy's "Inventory" with the [userfields] of what it adds; see [purchase]. */
    suspend fun inventory(
        productId: Int,
        booking: InventoryBooking,
        userfields: Map<String, String?>,
    ): BookingResult = book {
        val result = api.inventoryProduct(it, productId, booking)
        saveTransactionUserfields(it, result, userfields)
        result
    }

    /**
     * Saves the userfields of the stock entries a booking created, under its transaction like
     * Grocy's pages. The booking is done by then, so a failure here does not undo it.
     */
    private suspend fun saveTransactionUserfields(
        connection: ServerConnection,
        result: BookingResult,
        userfields: Map<String, String?>,
    ) {
        val transactionId = result.transactionId ?: return
        if (userfields.isEmpty()) return
        try {
            api.setUserfields(connection, MasterDataRepository.STOCK, transactionId, userfields)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The stock entries keep their default values, like after a failed save in Grocy
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
        afterBooking { refreshStockEntries() }
        return result
    }

    /** Runs a booking, then downloads the stock overview it changed. */
    private suspend fun <T> bookOnOverview(
        withShoppingLists: Boolean,
        block: suspend (ServerConnection) -> T,
    ): T {
        val result = block(requireConnection())
        afterBooking { refreshOverview(withShoppingLists) }
        return result
    }

    /**
     * Downloads what a booking changed. The booking is done once the server took it, so a failed
     * download must not report it as failed, which would invite booking it twice.
     */
    private suspend fun afterBooking(download: suspend () -> Unit) {
        try {
            download()
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next refresh downloads it
        }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    private class OverviewDownload(
        val amounts: List<StockAmount>,
        val info: List<ProductStockInfo>,
        val locations: List<StockLocationAmount>,
    )
}

/** One [ProductStockInfo] per product any of the downloads names. */
internal fun productStockInfoOf(
    missing: Map<Int, Double>,
    purchases: List<ProductPurchase>,
    averagePrices: Map<Int, Double>,
    onShoppingLists: Set<Int>,
): List<ProductStockInfo> {
    val purchasesById = purchases.associateBy { it.productId }
    val ids = missing.keys + purchasesById.keys + averagePrices.keys + onShoppingLists
    return ids.sorted().map { id ->
        ProductStockInfo(
            productId = id,
            amountMissing = missing[id],
            onShoppingList = id in onShoppingLists,
            lastPurchased = purchasesById[id]?.date,
            lastPrice = purchasesById[id]?.price,
            averagePrice = averagePrices[id],
        )
    }
}
