package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ShoppingListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val settingsStore = FakeSettingsStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private var options: ShoppingListOptionsRepository? = null

    private fun TestScope.viewModel(listId: Int? = null): ShoppingListViewModel {
        val options = ShoppingListOptionsRepository(
            settingsStore,
            // Unconfined, so the options are read right away like at app start
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )
        this@ShoppingListViewModelTest.options = options
        return ShoppingListViewModel(
            listId = listId,
            masterData = environment.repository,
            stock = environment.stockRepository,
            shoppingLists = environment.shoppingListRepository,
            optionsRepository = options,
            computeDispatcher = dispatcher,
            decimalSeparator = '.',
            language = "en",
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    private val ShoppingListViewModel.rows
        get() = state.value?.groups?.flatMap { it.rows }.orEmpty()

    private fun doneRequests(itemId: Int) = environment.server
        .requestsTo("/api/objects/shopping_list/$itemId", HttpMethod.Put)
        .map { (it.body as TextContent).text }

    @Test
    fun listIsDownloadedAndShownByProductGroup() = runTest(dispatcher) {
        val viewModel = viewModel()

        val state = viewModel.state.value!!
        assertEquals(1, state.list?.id)
        assertEquals(
            listOf(ShoppingListGroupTitle.Text("Dairy"), ShoppingListGroupTitle.Ungrouped),
            state.groups.map { it.title },
        )
        // Milk's 6 pieces in packs of 6; only its sub product is below its min. stock amount
        val milk = viewModel.rows.first { it.id == 5 }
        assertEquals("1 Pack", milk.amount)
        assertFalse(milk.isBelowMinStock)
        assertEquals("Bring bags", state.notes)
        assertEquals(
            listOf(
                ShoppingListChoice(2, "Hardware store", 0),
                ShoppingListChoice(1, "Shopping list", 2),
            ),
            state.lists,
        )
        // Milk's last price of 1 per piece
        assertEquals(6.0, state.totalValue)
    }

    @Test
    fun anotherListIsShownWhenChosen() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.selectList(2)
        advanceUntilIdle()

        assertEquals("Hardware store", viewModel.state.value?.list?.name)
        assertTrue(viewModel.state.value!!.isEmpty)
    }

    @Test
    fun checkingOffWorksOfflineAndIsSentOnceOnline() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        viewModel.toggleDone(viewModel.rows.first { it.id == 5 })
        advanceUntilIdle()

        assertTrue(viewModel.rows.first { it.id == 5 }.done)
        assertEquals(
            mapOf(5 to ShoppingListItemSync.FAILED),
            viewModel.state.value?.syncStates,
        )

        environment.server.unreachable = false
        environment.server.requests.clear()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf("""{"done":1}"""), doneRequests(5))
        assertEquals(emptyMap(), viewModel.state.value?.syncStates)
    }

    @Test
    fun offlineRefreshKeepsTheStoredList() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
        assertEquals(2, viewModel.rows.size)
    }

    @Test
    fun deletingOfflineReportsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.delete(viewModel.rows.first())
            advanceUntilIdle()

            val event = assertIs<ShoppingListEvent.Failed>(awaitItem())
            assertEquals(ChangeError.Offline, event.error)
        }
        assertEquals(2, viewModel.rows.size)
    }

    @Test
    fun clearingAsksFirstNamingTheList() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.requestClear()
        val dialog = assertIs<ShoppingListDialog.ClearList>(viewModel.dialog)
        assertEquals("Shopping list", dialog.name)
        viewModel.clear(dialog.listId)
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        val clear = environment.server
            .requestsTo("/api/stock/shoppinglist/clear", HttpMethod.Post).single()
        assertEquals("""{"list_id":1,"done_only":false}""", (clear.body as TextContent).text)
    }

    @Test
    fun clearingDoneItemsAsksNothing() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.clearDone()
        advanceUntilIdle()

        val clear = environment.server
            .requestsTo("/api/stock/shoppinglist/clear", HttpMethod.Post).single()
        assertEquals("""{"list_id":1,"done_only":true}""", (clear.body as TextContent).text)
    }

    @Test
    fun theFirstListCannotBeDeleted() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.requestDeleteList()

        assertNull(viewModel.dialog)
    }

    @Test
    fun deletedListShowsTheFirstOneAgain() = runTest(dispatcher) {
        val viewModel = viewModel(listId = 2)

        viewModel.requestDeleteList()
        val dialog = assertIs<ShoppingListDialog.DeleteList>(viewModel.dialog)
        viewModel.deleteList(dialog.listId)
        advanceUntilIdle()

        assertEquals(
            1,
            environment.server.requestsTo("/api/objects/shopping_lists/2", HttpMethod.Delete).size,
        )
        assertEquals(1, viewModel.state.value?.list?.id)
    }

    @Test
    fun statusFilterShowsOnlyDoneItems() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setStatus(ShoppingListStatus.DONE)
        advanceUntilIdle()

        assertEquals(listOf(6), viewModel.rows.map { it.id })
        viewModel.clearFilter()
        advanceUntilIdle()
        assertEquals(2, viewModel.rows.size)
    }

    @Test
    fun tableOptionsAreKept() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setGrouping(ShoppingListGrouping.NONE)
        viewModel.setFieldShown(ShoppingListField.BARCODES, shown = true)
        advanceUntilIdle()

        val options = settingsStore.shoppingListOptions.value
        assertEquals(ShoppingListGrouping.NONE, options.grouping)
        assertTrue(ShoppingListField.BARCODES in options.fields)
        assertNull(viewModel.state.value!!.groups.single().title)
    }

    @Test
    fun notesOfflineCloseTheirDialogAndAreSentOnceOnline() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        viewModel.editNotes()
        viewModel.notes!!.text.setTextAndPlaceCursorAtEnd("Bring bags\nand coins")
        viewModel.saveNotes()
        advanceUntilIdle()

        assertNull(viewModel.notes)
        assertEquals("Bring bags\nand coins", viewModel.state.value?.notes)
        assertEquals(ShoppingListItemSync.FAILED, viewModel.state.value?.notesSync)
        environment.server.unreachable = false
        viewModel.refresh()
        advanceUntilIdle()
        assertNull(viewModel.state.value?.notesSync)
    }

    @Test
    fun listSelectorOpensAndClosesWithTheChoice() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.chooseList()
        assertEquals(ShoppingListDialog.ChooseList, viewModel.dialog)
        viewModel.selectList(2)
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        assertEquals(2, viewModel.state.value?.list?.id)
    }

    @Test
    fun statusFilterIsTheOneTheShoppingModeShares() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setStatus(ShoppingListStatus.DONE)
        advanceUntilIdle()

        assertEquals(ShoppingListStatus.DONE, options?.status?.value)
        assertEquals(listOf(6), viewModel.rows.map { it.id })
    }

    @Test
    fun notesAreSavedAsHtml() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.editNotes()
        assertEquals("Bring bags", viewModel.notes?.text?.text.toString())
        viewModel.saveNotes(clear = true)
        advanceUntilIdle()

        assertNull(viewModel.notes)
        val put = environment.server
            .requestsTo("/api/objects/shopping_lists/1", HttpMethod.Put).single()
        assertEquals("""{"description":""}""", (put.body as TextContent).text)
    }

    @Test
    fun failedPrintStaysInItsDialog() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.respond(
            "/api/print/shoppinglist/thermal",
            HttpStatusCode.BadRequest,
            """{"error_message":"No printer"}""",
        )

        viewModel.openPrint()
        viewModel.printThermal()
        advanceUntilIdle()

        assertEquals(ChangeError.Failed("No printer"), viewModel.print?.error)
    }

    @Test
    fun addingAllToStockOpensOnePurchaseAfterTheOther() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/shopping_list",
            body = """[{"id":5,"product_id":1,"shopping_list_id":1,"amount":6,"qu_id":2},""" +
                """{"id":7,"product_id":2,"shopping_list_id":1,"amount":1,"qu_id":1}]""",
        )
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.addAllToStock()
            advanceUntilIdle()
            // Dairy first, then the ungrouped Bread
            val first = assertIs<ShoppingListEvent.OpenPurchase>(awaitItem())
            assertEquals(ShoppingListPurchase(5, 1, 6.0, 2), first.purchase)
            assertEquals(PurchaseProgress(position = 1, count = 2), first.progress)

            // Booked: the purchase removed the item, the next one opens
            environment.shoppingListRepository.deleteItem(5)
            viewModel.onShown()
            advanceUntilIdle()
            val second = assertIs<ShoppingListEvent.OpenPurchase>(awaitItem()).purchase
            assertEquals(ShoppingListPurchase(7, 2, 1.0, 1), second)

            // Left without booking: Grocy's skip or stop
            viewModel.onShown()
            advanceUntilIdle()
            val skipped = assertIs<ShoppingListDialog.StockItemSkipped>(viewModel.dialog)
            assertEquals(2, skipped.position)
            assertEquals("Bread", skipped.name)
            viewModel.skipStockItem()
            advanceUntilIdle()
            expectNoEvents()
        }
        assertNull(viewModel.dialog)
    }

    @Test
    fun shownAgainWithoutWorkflowDoesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onShown()
        advanceUntilIdle()

        assertNull(viewModel.dialog)
    }
}
