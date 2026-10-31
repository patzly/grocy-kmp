package com.patrickzedler.grocy.core.data.shoppinglist

import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListCount
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.MasterDataJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

class ShoppingListRepositoryTest {

    // Unconfined everywhere, so check offs sent in the background are done right away
    private val dispatcher = UnconfinedTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "k") }

    private val networkAvailable = MutableSharedFlow<Unit>()

    private fun TestScope.repository() = ShoppingListRepository(
        api = server.api,
        database = database,
        scope = backgroundScope,
        connection = connection,
        networkAvailable = networkAvailable,
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun doneRequests(itemId: Int) =
        server.requestsTo("/api/objects/shopping_list/$itemId", HttpMethod.Put)
            .map { (it.body as TextContent).text }

    @Test
    fun refreshStoresListsWithNotesAndItems() = runTest(dispatcher) {
        val repository = repository()

        repository.refresh()

        assertEquals(
            listOf(
                ShoppingList(2, "Hardware store"),
                ShoppingList(1, "Shopping list", "<p>Bring bags</p>"),
            ),
            repository.lists.first(),
        )
        assertEquals(
            listOf(
                ShoppingListItem(5, 1, 1, "Low fat", 6.0, 2, done = false),
                ShoppingListItem(6, 1, null, "Batteries", 2.0, null, done = true),
            ),
            repository.items.first(),
        )
    }

    @Test
    fun summaryCountsTheUndoneItemsPerList() = runTest(dispatcher) {
        val repository = repository()

        repository.refresh()

        assertEquals(
            listOf(
                ShoppingListCount(2, "Hardware store", undoneCount = 0, itemCount = 0),
                ShoppingListCount(1, "Shopping list", undoneCount = 1, itemCount = 2),
            ),
            repository.summary.first().lists,
        )
    }

    @Test
    fun checkingOffOnlineSendsOnlyDoneAndEmptiesTheQueue() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()

        repository.setDone(5, done = true)

        assertEquals(listOf("""{"done":1}"""), doneRequests(5))
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun checkingOffOfflineIsStoredAndQueued() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true

        repository.setDone(5, done = true)

        assertTrue(repository.items.first().first { it.id == 5 }.done)
        assertEquals(mapOf(5 to ShoppingListItemSync.FAILED), repository.syncStates.first())
    }

    @Test
    fun queuedCheckOffSurvivesADownloadThatCannotSendIt() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(5, done = true)

        assertFailsWith<GrocyApiException.Unreachable> { repository.refresh() }

        assertTrue(repository.items.first().first { it.id == 5 }.done)
    }

    @Test
    fun queuedCheckOffsAreReplayedOnceOnlineWithTheUsersLastChoice() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(5, done = true)
        repository.setDone(6, done = false)
        repository.setDone(5, done = false)
        server.unreachable = false
        // The attempts while offline never reached the server
        server.requests.clear()

        repository.refresh()

        assertEquals(listOf("""{"done":0}"""), doneRequests(5))
        assertEquals(listOf("""{"done":0}"""), doneRequests(6))
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun replayNeverRecreatesAnItemSomeoneElseRemoved() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(5, done = true)
        server.unreachable = false
        // Someone removed Milk in the meantime, Grocy refuses the change
        server.respond(
            path = "/api/objects/shopping_list/5",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"Object not found"}""",
            method = HttpMethod.Put,
        )
        server.respond(
            "/api/objects/shopping_list",
            body = """[{"id":6,"note":"Batteries","amount":2,"done":1}]""",
        )

        repository.refresh()

        assertEquals(listOf(6), repository.items.first().map { it.id })
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun replayKeepsWhatOthersChangedOnTheItem() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(5, done = true)
        server.unreachable = false
        // Someone else raised the amount meanwhile; the server keeps it, only done is sent
        server.respond(
            "/api/objects/shopping_list",
            body = """[{"id":5,"product_id":1,"shopping_list_id":1,"amount":"12",""" +
                """"qu_id":2,"done":1}]""",
        )

        repository.refresh()

        val milk = repository.items.first().single()
        assertEquals(12.0, milk.amount)
        assertTrue(milk.done)
    }

    @Test
    fun serverErrorKeepsTheCheckOffQueued() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.respond(
            path = "/api/objects/shopping_list/5",
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Put,
        )

        repository.setDone(5, done = true)

        assertEquals(mapOf(5 to ShoppingListItemSync.FAILED), repository.syncStates.first())
    }

    @Test
    fun backgroundSendingAtAppStartReplaysTheQueue() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(6, done = false)
        server.unreachable = false
        server.requests.clear()

        repository.sendPendingInBackground()

        assertEquals(listOf("""{"done":0}"""), doneRequests(6))
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun queuedCheckOffIsSendingUntilItsFirstAttemptFails() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(5, done = true)
        // The app starts again, nothing was tried yet in this process
        val restarted = repository()

        assertEquals(mapOf(5 to ShoppingListItemSync.SENDING), restarted.syncStates.first())
    }

    @Test
    fun failedCheckOffIsSentOnceTheDeviceIsOnlineAgain() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.setDone(5, done = true)
        server.unreachable = false
        server.requests.clear()

        networkAvailable.emit(Unit)

        assertEquals(listOf("""{"done":1}"""), doneRequests(5))
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun failedCheckOffIsRetriedByItselfEverLessOften() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.respond(
            path = "/api/objects/shopping_list/5",
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Put,
        )
        repository.setDone(5, done = true)

        advanceTimeBy(ShoppingListRepository.FIRST_RETRY_DELAY + 1.milliseconds)
        assertEquals(2, doneRequests(5).size)
        advanceTimeBy(ShoppingListRepository.FIRST_RETRY_DELAY + 1.milliseconds)
        assertEquals(2, doneRequests(5).size)
        server.respond(
            path = "/api/objects/shopping_list/5",
            status = HttpStatusCode.NoContent,
            method = HttpMethod.Put,
        )
        advanceTimeBy(ShoppingListRepository.FIRST_RETRY_DELAY)

        assertEquals(3, doneRequests(5).size)
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun deletedItemIsGoneLocallyWithItsQueuedCheckOff() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.respond(
            path = "/api/objects/shopping_list/6",
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Put,
        )
        repository.setDone(6, done = false)

        repository.deleteItem(6)

        assertEquals(1, server.requestsTo("/api/objects/shopping_list/6", HttpMethod.Delete).size)
        assertEquals(listOf(5), repository.items.first().map { it.id })
        assertEquals(emptyMap(), repository.syncStates.first())
    }

    @Test
    fun deletingOfflineFailsAndKeepsTheItem() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> { repository.deleteItem(5) }

        assertEquals(2, repository.items.first().size)
    }

    @Test
    fun savedItemIsStoredAsTheServerSavedIt() = runTest(dispatcher) {
        val repository = repository()
        server.respond(
            "/api/objects/shopping_list/${MasterDataJson.CREATED_ID}",
            body = """{"id":${MasterDataJson.CREATED_ID},"shopping_list_id":2,""" +
                """"note":"Nails","amount":"1","done":0}""",
        )

        val id = repository.saveItem(
            ShoppingListItem(0, 2, null, "Nails", 1.0, null, false),
            userfields = mapOf("urgent" to "1"),
        )

        assertEquals(MasterDataJson.CREATED_ID, id)
        assertEquals(
            1,
            server.requestsTo(
                "/api/userfields/shopping_list/${MasterDataJson.CREATED_ID}",
                HttpMethod.Put,
            ).size,
        )
        assertEquals("Nails", repository.item(id).first()?.note)
    }

    @Test
    fun addedProductGetsItsUserfieldsAndTheItemsAreDownloaded() = runTest(dispatcher) {
        val repository = repository()

        repository.addProduct(1, 1, 6.0, 2, note = null, userfields = mapOf("urgent" to "1"))

        assertEquals(1, server.requestsTo("/api/userfields/shopping_list/5", HttpMethod.Put).size)
        assertEquals(2, repository.items.first().size)
    }

    @Test
    fun addedProductWithoutChangedUserfieldsIsNotWrittenAgain() = runTest(dispatcher) {
        repository().addProduct(1, 1, 6.0, 2, note = null, userfields = emptyMap())

        assertTrue(server.requestsTo("/api/userfields/shopping_list/5", HttpMethod.Put).isEmpty())
    }

    @Test
    fun overdueAndExpiredProductsAreAddedInGrocysOrder() = runTest(dispatcher) {
        repository().addOverdueAndExpiredProducts(listId = 2)

        assertEquals(
            listOf(
                "/api/stock/shoppinglist/add-overdue-products",
                "/api/stock/shoppinglist/add-expired-products",
                "/api/objects/shopping_list",
            ),
            server.requestedPaths,
        )
    }

    @Test
    fun failedDownloadAfterAChangeNeverReportsTheChangeAsFailed() = runTest(dispatcher) {
        val repository = repository()
        server.respond("/api/objects/shopping_list", HttpStatusCode.InternalServerError)

        repository.clear(listId = 1, doneOnly = true)

        assertEquals(1, server.requestsTo("/api/stock/shoppinglist/clear", HttpMethod.Post).size)
    }

    @Test
    fun deletedListTakesItsItemsAlong() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()

        repository.deleteList(1)

        assertEquals(listOf(2), repository.lists.first().map { it.id })
        assertTrue(repository.items.first().isEmpty())
    }

    private fun notesRequests(listId: Int) =
        server.requestsTo("/api/objects/shopping_lists/$listId", HttpMethod.Put)
            .map { (it.body as TextContent).text }

    @Test
    fun notesOnlineAreSentAndStoredAsTheServerCleanedThemUp() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.respond(
            "/api/objects/shopping_lists",
            body = """[{"id":2,"name":"Hardware store","description":"<p>Nails</p>"}]""",
        )

        repository.saveNotes(2, "<p>Nails")

        assertEquals(listOf("""{"description":"<p>Nails"}"""), notesRequests(2))
        assertEquals("<p>Nails</p>", repository.list(2).first()?.description)
        assertEquals(emptyMap(), repository.notesSyncStates.first())
    }

    @Test
    fun notesOfflineAreStoredAndQueued() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true

        repository.saveNotes(2, "<p>Nails</p>")

        assertEquals("<p>Nails</p>", repository.list(2).first()?.description)
        assertEquals(mapOf(2 to ShoppingListItemSync.FAILED), repository.notesSyncStates.first())
    }

    @Test
    fun queuedNotesAreSentOnceTheDeviceIsOnlineAgain() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.saveNotes(2, "<p>Nails</p>")
        repository.saveNotes(2, "<p>Screws</p>")
        server.unreachable = false
        server.requests.clear()

        networkAvailable.emit(Unit)

        assertEquals(listOf("""{"description":"<p>Screws</p>"}"""), notesRequests(2))
        assertEquals(emptyMap(), repository.notesSyncStates.first())
    }

    @Test
    fun replayDropsTheNotesOfAListSomeoneElseRemoved() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.unreachable = true
        repository.saveNotes(2, "<p>Nails</p>")
        server.unreachable = false
        server.respond(
            path = "/api/objects/shopping_lists/2",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"Object not found"}""",
            method = HttpMethod.Put,
        )
        server.respond(
            "/api/objects/shopping_lists",
            body = """[{"id":1,"name":"Shopping list"}]""",
        )

        repository.refresh()

        assertEquals(listOf(1), repository.lists.first().map { it.id })
        assertEquals(emptyMap(), repository.notesSyncStates.first())
    }

    @Test
    fun queuedNotesStayAboveARenameOfTheList() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.respond(
            path = "/api/objects/shopping_lists/2",
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Put,
        )
        repository.saveNotes(2, "<p>Nails</p>")
        server.respond(
            path = "/api/objects/shopping_lists/2",
            status = HttpStatusCode.NoContent,
            method = HttpMethod.Put,
        )
        server.respond(
            "/api/objects/shopping_lists/2",
            body = """{"id":2,"name":"DIY store"}""",
        )

        repository.saveList(ShoppingList(2, "DIY store"), userfields = emptyMap())

        assertEquals(
            ShoppingList(2, "DIY store", "<p>Nails</p>"),
            repository.list(2).first(),
        )
    }

    @Test
    fun deletedListTakesItsQueuedNotesAlong() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.respond(
            path = "/api/objects/shopping_lists/2",
            status = HttpStatusCode.InternalServerError,
            method = HttpMethod.Put,
        )
        repository.saveNotes(2, "<p>Nails</p>")

        repository.deleteList(2)

        assertEquals(emptyMap(), repository.notesSyncStates.first())
    }

    @Test
    fun quickRefreshDownloadsOnlyTheItems() = runTest(dispatcher) {
        val repository = repository()
        repository.refresh()
        server.requests.clear()

        repository.refreshItemsOnly()

        assertEquals(listOf("/api/objects/shopping_list"), server.requestedPaths)
    }

    @Test
    fun printingAsksTheServersThermalPrinter() = runTest(dispatcher) {
        repository().print(listId = 2, printHeader = true)

        val url = server.requests.single().url
        assertEquals("2", url.parameters["list"])
        assertEquals("true", url.parameters["printHeader"])
        assertFalse(server.unreachable)
    }
}
