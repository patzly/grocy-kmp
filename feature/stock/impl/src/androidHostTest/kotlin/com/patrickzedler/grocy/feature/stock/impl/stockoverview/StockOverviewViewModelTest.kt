package com.patrickzedler.grocy.feature.stock.impl.stockoverview

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.settings.StockOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class StockOverviewViewModelTest {

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

    private fun TestScope.viewModel(): StockOverviewViewModel {
        val options = StockOverviewOptionsRepository(
            settingsStore,
            // Unconfined, so the options are read right away like at app start
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )
        return StockOverviewViewModel(
            masterData = environment.repository,
            stock = environment.stockRepository,
            optionsRepository = options,
            computeDispatcher = dispatcher,
            decimalSeparator = '.',
            language = "en",
            today = { epochDayOf("2026-10-25")!! },
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    private val StockOverviewViewModel.names
        get() = state.value?.groups?.flatMap { group -> group.items.map { it.name } }

    @Test
    fun stockIsDownloadedAndShownByNextDueDate() = runTest(dispatcher) {
        val viewModel = viewModel()

        // Milk is in stock, Yoghurt below its min. stock amount
        assertEquals(listOf("Milk", "Yoghurt"), viewModel.names)
        val state = viewModel.state.value!!
        assertEquals(1, state.productCount)
        assertEquals(3.0, state.totalValue)
        assertEquals(1, state.statusCounts[StockStatus.BELOW_MIN_STOCK])
        assertEquals(
            listOf(StockOverviewField.AMOUNT, StockOverviewField.NEXT_DUE_DATE),
            state.fields,
        )
        assertTrue(state.isDefaultSort)
    }

    @Test
    fun offlineShowsTheSavedStockWithANote() = runTest(dispatcher) {
        viewModel()
        environment.server.unreachable = true

        val viewModel = viewModel()

        assertEquals(listOf("Milk", "Yoghurt"), viewModel.names)
        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun tableOptionsAreKeptForTheNextStart() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setGrouping(StockOverviewGrouping.PRODUCT_GROUP)
        viewModel.setFieldShown(StockOverviewField.VALUE, shown = true)
        viewModel.setSort(StockOverviewField.VALUE, ascending = false)
        advanceUntilIdle()

        val expected = StockOverviewOptions(
            fields = StockOverviewOptions.DEFAULT_FIELDS + StockOverviewField.VALUE,
            grouping = StockOverviewGrouping.PRODUCT_GROUP,
            sortField = StockOverviewField.VALUE,
            ascending = false,
        )
        assertEquals(expected, settingsStore.stockOverviewOptions.value)
        val state = viewModel.state.value!!
        assertEquals(StockOverviewGrouping.PRODUCT_GROUP, state.grouping)
        assertEquals(StockOverviewSort(StockOverviewField.VALUE, ascending = false), state.sort)
        assertFalse(state.isDefaultSort)
    }

    @Test
    fun hidingTheSortedFieldSortsByGrocysDefaultAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setFieldShown(StockOverviewField.VALUE, shown = true)
        viewModel.setSort(StockOverviewField.VALUE, ascending = false)
        advanceUntilIdle()

        viewModel.setFieldShown(StockOverviewField.VALUE, shown = false)
        advanceUntilIdle()

        val stored = settingsStore.stockOverviewOptions.value
        assertEquals(StockOverviewField.NEXT_DUE_DATE, stored.sortField)
        assertTrue(stored.ascending)
        assertTrue(viewModel.state.value!!.isDefaultSort)
    }

    @Test
    fun hidingTheDueDateWhileSortedByItSortsByName() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setFieldShown(StockOverviewField.NEXT_DUE_DATE, shown = false)
        advanceUntilIdle()

        assertEquals(StockOverviewField.NAME, settingsStore.stockOverviewOptions.value.sortField)
        assertEquals(StockOverviewSort(StockOverviewField.NAME), viewModel.state.value!!.sort)
    }

    @Test
    fun hidingAnotherFieldKeepsTheSort() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setFieldShown(StockOverviewField.AMOUNT, shown = false)
        advanceUntilIdle()

        assertEquals(
            StockOverviewField.NEXT_DUE_DATE,
            settingsStore.stockOverviewOptions.value.sortField,
        )
    }

    @Test
    fun filterAndSearchNarrowTheListAndClearFilterResetsBoth() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setFilter { it.copy(status = StockStatus.IN_STOCK) }
        advanceUntilIdle()
        assertEquals(listOf("Milk"), viewModel.names)

        viewModel.showSearch()
        viewModel.searchState.setTextAndPlaceCursorAtEnd("yog")
        Snapshot.sendApplyNotifications()
        viewModel.clearFilter()
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("Milk", "Yoghurt"), viewModel.names)
        assertFalse(viewModel.isSearchVisible)
        assertEquals("", viewModel.searchState.text.toString())
    }

    private fun StockOverviewViewModel.item(name: String) =
        state.value!!.groups.flatMap { it.items }.single { it.name == name }

    private fun bookingBody(action: String) = environment.server
        .requestsTo("/api/stock/products/1/$action", HttpMethod.Post)
        .single().body.let { Json.parseToJsonElement((it as TextContent).text).jsonObject }

    @Test
    fun consumingBooksTheQuickConsumeAmountAndOffersUndo() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.consume(viewModel.item("Milk"))
            advanceUntilIdle()

            assertEquals(
                StockOverviewEvent.Consumed("1 Piece", "Milk", spoiled = false, "t10"),
                awaitItem(),
            )
        }
        val body = bookingBody("consume")
        assertEquals(1.0, body.getValue("amount").jsonPrimitive.double)
        assertFalse(body.getValue("spoiled").jsonPrimitive.boolean)
    }

    @Test
    fun consumingAllBooksTheWholeStockAsSpoiledOnlyWhenAsked() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.consumeAll(viewModel.item("Milk"))
            advanceUntilIdle()

            assertEquals("3 Pieces", (awaitItem() as StockOverviewEvent.Consumed).amount)
        }
        val body = bookingBody("consume")
        assertEquals(3.0, body.getValue("amount").jsonPrimitive.double)
        assertFalse(body.getValue("spoiled").jsonPrimitive.boolean)
    }

    @Test
    fun openingBooksTheQuickOpenAmount() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.open(viewModel.item("Milk"))
            advanceUntilIdle()

            assertEquals(
                StockOverviewEvent.Opened("1 Piece", "Milk", movedTo = null, "t10"),
                awaitItem(),
            )
        }
        assertEquals(1.0, bookingBody("open").getValue("amount").jsonPrimitive.double)
    }

    @Test
    fun undoUndoesTheWholeTransaction() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.undo("t10")
            advanceUntilIdle()

            assertEquals(StockOverviewEvent.Undone, awaitItem())
        }
        assertEquals(
            1,
            environment.server.requestsTo("/api/stock/transactions/t10/undo", HttpMethod.Post).size,
        )
    }

    @Test
    fun bookingOfflineFailsWithoutChangingAnything() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.consumeSpoiled(viewModel.item("Milk"))
            advanceUntilIdle()

            assertEquals(StockOverviewEvent.Failed(ChangeError.Offline), awaitItem())
        }
        assertEquals(listOf("Milk", "Yoghurt"), viewModel.names)
    }

    @Test
    fun searchFindsProductsByName() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.searchState.setTextAndPlaceCursorAtEnd("yog")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("Yoghurt"), viewModel.names)
    }
}
