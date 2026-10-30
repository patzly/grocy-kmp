package com.patrickzedler.grocy.core.data.stock

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class StockRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "k") }
    private val repository = StockRepository(server.api, database, connection)
    private val masterData = MasterDataRepository(server.api, database, connection)

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshStoresTheEntriesAndTheStockAmounts() = runTest {
        repository.refreshStockEntries()

        val entries = repository.stockEntries.first()
        assertEquals(listOf(1, 2), entries.map { it.id })
        assertEquals("Half full", entries[1].note)
        assertEquals(3.0, masterData.stock.first().single().amount)
    }

    @Test
    fun overviewRefreshStoresWhatTheServerComputedPerProduct() = runTest {
        repository.refreshOverview(withShoppingLists = true)

        val milk = masterData.stock.first().single()
        assertEquals(1.0, milk.amountOpened)
        assertEquals("2026-10-20", milk.bestBeforeDate)
        assertEquals(
            listOf(
                ProductStockInfo(1, null, true, "2026-10-01", 1.0, 0.9),
                ProductStockInfo(4, amountMissing = 1.0),
            ),
            repository.productStockInfo.first(),
        )
        assertEquals(listOf(StockLocationAmount(1, 1, 3.0)), repository.stockLocations.first())
    }

    @Test
    fun overviewRefreshWithoutShoppingListsNeverAsksForThem() = runTest {
        repository.refreshOverview(withShoppingLists = false)

        assertTrue("/api/objects/shopping_list" !in server.requestedPaths)
        assertEquals(false, repository.productStockInfo.first().first().onShoppingList)
    }

    @Test
    fun overviewStaysOfflineWhenItsRefreshFails() = runTest {
        repository.refreshOverview(withShoppingLists = true)
        server.respond("/api/objects/products_average_price", HttpStatusCode.InternalServerError)

        assertFailsWith<GrocyApiException.HttpError> {
            repository.refreshOverview(withShoppingLists = true)
        }

        assertEquals(2, repository.productStockInfo.first().size)
    }

    @Test
    fun entriesStayOfflineWhenTheRefreshFails() = runTest {
        repository.refreshStockEntries()
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> { repository.refreshStockEntries() }

        assertEquals(2, repository.stockEntries.first().size)
    }

    @Test
    fun consumingDownloadsTheChangedStockAndReturnsTheBooking() = runTest {
        repository.refreshStockEntries()
        val entry = repository.stockEntries.first().first()
        server.respond("/api/objects/stock", body = "[]")

        val booking = repository.consume(entry, spoiled = false)

        assertEquals(10, booking)
        assertTrue(repository.stockEntries.first().isEmpty())
    }

    @Test
    fun editedEntrySavesTheUserfieldsOfItsStockId() = runTest {
        repository.refreshStockEntries()
        val entry = repository.stockEntries.first().first()

        repository.edit(entry.copy(amount = 1.0), mapOf("batch" to "8"))

        val saved = server.requestsTo("/api/userfields/stock/6f1a2b", HttpMethod.Put)
        assertEquals("""{"batch":"8"}""", (saved.single().body as TextContent).text)
    }

    @Test
    fun undoingDownloadsTheStockAgain() = runTest {
        repository.undo(10)

        assertEquals(2, repository.stockEntries.first().size)
    }

    @Test
    fun quickConsumeDownloadsTheOverviewAndReturnsTheTransaction() = runTest {
        repository.refreshOverview(withShoppingLists = true)
        server.respond("/api/stock", body = "[]")

        val transaction = repository.consumeProduct(1, 1.0, false, withShoppingLists = true)

        assertEquals("t10", transaction)
        assertTrue(masterData.stock.first().isEmpty())
    }

    @Test
    fun quickOpenAndItsUndoDownloadTheOverview() = runTest {
        val transaction = repository.openProduct(1, 1.0, withShoppingLists = false)
        server.respond("/api/stock", body = "[]")

        repository.undoTransaction(requireNotNull(transaction), withShoppingLists = false)

        assertEquals(1, server.requestsTo("/api/stock/transactions/t10/undo", HttpMethod.Post).size)
        assertTrue(masterData.stock.first().isEmpty())
    }

    @Test
    fun bookingIsDoneEvenWhenItsDownloadFails() = runTest {
        server.respond("/api/stock/volatile", HttpStatusCode.InternalServerError)

        val transaction = repository.consumeProduct(1, 1.0, true, withShoppingLists = true)

        // Reporting it as failed would invite booking it twice
        assertEquals("t10", transaction)
    }

    @Test
    fun journalRefreshReplacesTheBookingsOfItsTime() = runTest {
        repository.refreshStockLog(since = "2026-04-25")
        server.respond("/api/objects/stock_log", body = "[]")

        repository.refreshStockLog(since = "2026-10-21")

        // The purchase of October 1st stays, the later consumptions are gone
        assertEquals(listOf(1), repository.stockLog(since = "").first().map { it.id })
    }

    @Test
    fun rejectedBookingFails() = runTest {
        repository.refreshStockEntries()
        val entry = repository.stockEntries.first().first()
        server.respond(
            path = "/api/stock/products/1/open",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"Not enough in stock"}""",
            method = HttpMethod.Post,
        )

        val error = assertFailsWith<GrocyApiException.HttpError> { repository.open(entry) }

        assertEquals("Not enough in stock", error.serverMessage)
    }
}
