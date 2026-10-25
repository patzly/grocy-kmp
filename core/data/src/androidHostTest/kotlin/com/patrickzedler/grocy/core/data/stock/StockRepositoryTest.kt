package com.patrickzedler.grocy.core.data.stock

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
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
    fun journalRefreshReplacesTheBookingsOfItsTime() = runTest {
        repository.refreshStockLog(since = "2026-04-25")
        server.respond("/api/objects/stock_log", body = "[]")

        repository.refreshStockLog(since = "2026-10-21")

        // The purchase of October 1st stays, the later consumptions are gone
        assertEquals(listOf(1), repository.stockLog(since = "").first().map { it.id })
    }

    @Test
    fun shoppingListItemGetsItsUserfieldsAfterTheProductWasAdded() = runTest {
        repository.addToShoppingList(
            productId = 1,
            listId = 1,
            amount = 6.0,
            quId = 2,
            note = null,
            userfields = mapOf("urgent" to "1"),
        )

        assertEquals(1, server.requestsTo("/api/userfields/shopping_list/5", HttpMethod.Put).size)
    }

    @Test
    fun shoppingListItemWithoutChangedUserfieldsIsNotWrittenAgain() = runTest {
        repository.addToShoppingList(1, 1, 6.0, 2, note = null, userfields = emptyMap())

        assertTrue(server.requestsTo("/api/userfields/shopping_list/5", HttpMethod.Put).isEmpty())
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
