package com.patrickzedler.grocy.feature.masterdata.impl.journal

import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.stock.StockTransactionType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class StockJournalViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(summary: Boolean = false): StockJournalViewModel {
        environment.repository.refresh()
        return StockJournalViewModel(
            productId = null,
            summary = summary,
            masterData = environment.repository,
            stock = environment.stockRepository,
            decimalSeparator = '.',
            language = "en",
            today = { epochDayOf("2026-10-25")!! },
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }
    }

    private fun journalQueries() = environment.server.requests
        .filter { it.url.encodedPath == "/api/objects/stock_log" }
        .map { it.url.parameters.getAll("query[]") }

    @Test
    fun journalShowsTheLastSixMonths() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(listOf(3, 2, 1), viewModel.state.value!!.items.map { it.booking.id })
        assertEquals(listOf(listOf("row_created_timestamp>=2026-04-25")), journalQueries())
    }

    @Test
    fun longerRangeDownloadsAgain() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setFilter { it.copy(months = JOURNAL_ALL_MONTHS) }
        advanceUntilIdle()

        assertNull(journalQueries().last())
    }

    @Test
    fun typeFilterKeepsItsBookings() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setFilter { it.copy(transactionType = StockTransactionType.PURCHASE) }
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.state.value!!.items.map { it.booking.id })
    }

    @Test
    fun undoingABookingDownloadsTheJournalAgain() = runTest(dispatcher) {
        environment.server.respond(
            "/api/stock/bookings/2/undo",
            status = HttpStatusCode.NoContent,
            method = HttpMethod.Post,
        )
        val viewModel = viewModel()
        val booking = viewModel.state.value!!.items.first { it.booking.id == 2 }.booking

        viewModel.events.test {
            viewModel.undo(booking)
            advanceUntilIdle()

            assertEquals(JournalEvent.Undone, awaitItem())
        }
        assertEquals(2, journalQueries().size)
    }

    @Test
    fun summaryDownloadsEverythingAndSumsWhatWasNotUndone() = runTest(dispatcher) {
        val viewModel = viewModel(summary = true)

        assertEquals(listOf(null), journalQueries())
        assertEquals(
            listOf("-1 Piece", "3 Pieces"),
            viewModel.state.value!!.summary.map { it.amount },
        )
    }
}
