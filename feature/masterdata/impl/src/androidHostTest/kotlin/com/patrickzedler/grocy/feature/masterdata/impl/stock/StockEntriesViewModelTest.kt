package com.patrickzedler.grocy.feature.masterdata.impl.stock

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class StockEntriesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = MasterDataTestEnvironment(dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(productId: Int? = null): StockEntriesViewModel {
        environment.repository.refresh()
        return StockEntriesViewModel(
            productId = productId,
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

    @Test
    fun entriesAreDownloadedWhenTheScreenOpens() = runTest(dispatcher) {
        val viewModel = viewModel()

        val items = viewModel.state.value!!.items
        assertEquals(listOf(2, 1), items.map { it.entry.id })
        // October 20th lies before today, Milk's best before date is only overdue
        assertEquals(DueStatus.OVERDUE, items.first().dueStatus)
        assertTrue(viewModel.refresh.value.loadedOnce)
    }

    @Test
    fun productOfTheLinkIsFilteredAndChoosable() = runTest(dispatcher) {
        val viewModel = viewModel(productId = 1)

        assertEquals(1, viewModel.state.value!!.filter.productId)
        assertEquals(listOf("Milk"), viewModel.state.value!!.products.map { it.name })
    }

    @Test
    fun consumedEntryCanBeUndone() = runTest(dispatcher) {
        val viewModel = viewModel()
        val entry = viewModel.state.value!!.items.last().entry

        viewModel.events.test {
            viewModel.consume(entry, spoiled = true)
            advanceUntilIdle()

            val event = assertIs<StockEntryEvent.Consumed>(awaitItem())
            assertEquals("2 Pieces", event.amount)
            assertEquals("Milk", event.productName)
            assertTrue(event.spoiled)
            assertEquals(10, event.bookingId)

            viewModel.undo(10)
            advanceUntilIdle()
            assertEquals(StockEntryEvent.Undone, awaitItem())
        }
        assertEquals(
            1,
            environment.server.requestsTo("/api/stock/bookings/10/undo", HttpMethod.Post).size,
        )
    }

    @Test
    fun openingMentionsTheMoveToTheConsumeLocation() = runTest(dispatcher) {
        environment.server.respond(
            "/api/objects/products",
            body = """[{"id":1,"name":"Milk","active":1,"location_id":1,"qu_id_stock":1,""" +
                """"qu_id_purchase":2,"qu_id_consume":1,"qu_id_price":2,"move_on_open":1,""" +
                """"default_consume_location_id":2}]""",
        )
        val viewModel = viewModel()
        val entry = viewModel.state.value!!.items.last().entry

        viewModel.events.test {
            viewModel.open(entry)
            advanceUntilIdle()

            assertEquals("Pantry", assertIs<StockEntryEvent.Opened>(awaitItem()).movedTo)
        }
    }

    @Test
    fun rejectedBookingIsReported() = runTest(dispatcher) {
        environment.server.respond(
            "/api/stock/products/1/consume",
            status = HttpStatusCode.BadRequest,
            body = """{"error_message":"Amount to be consumed cannot be > current stock"}""",
            method = HttpMethod.Post,
        )
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.consume(viewModel.state.value!!.items.first().entry, spoiled = false)
            advanceUntilIdle()

            assertEquals(
                StockEntryEvent.Failed(
                    ChangeError.Failed("Amount to be consumed cannot be > current stock"),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun editedEntryIsBookedWithItsValues() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.edit(viewModel.state.value!!.items.last().entry)
        advanceUntilIdle()

        val dialog = assertNotNull(viewModel.dialog)
        assertEquals("2", dialog.amount.text.toString())
        dialog.amount.setTextAndPlaceCursorAtEnd("1.5")
        dialog.neverOverdue = true
        viewModel.save()
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        val body = environment.server.requestsTo("/api/stock/entry/1", HttpMethod.Put).single()
            .let { (it.body as TextContent).text }
        assertTrue("\"amount\":1.5" in body)
        assertTrue("\"best_before_date\":\"2999-12-31\"" in body)
    }

    @Test
    fun invalidEntryIsNotBooked() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.edit(viewModel.state.value!!.items.last().entry)
        advanceUntilIdle()

        viewModel.dialog!!.purchasedDate.setTextAndPlaceCursorAtEnd("yesterday")
        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.dialog!!.errors[StockEntryField.PURCHASED_DATE])
        assertTrue(environment.server.requestsTo("/api/stock/entry/1", HttpMethod.Put).isEmpty())
    }
}
