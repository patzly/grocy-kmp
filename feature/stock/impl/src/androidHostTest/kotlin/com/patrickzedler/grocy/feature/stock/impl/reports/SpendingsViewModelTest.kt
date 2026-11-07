package com.patrickzedler.grocy.feature.stock.impl.reports

import com.patrickzedler.grocy.core.model.stock.ProductGroupFilter
import com.patrickzedler.grocy.core.model.stock.SpendingsGrouping
import com.patrickzedler.grocy.core.model.stock.SpendingsRangePreset
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.testTimeSource

class SpendingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)
    private val server = environment.server

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server.respond("/api/objects/stock_log", body = PRICE_LOG)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.viewModel(offline: Boolean = false): SpendingsViewModel {
        environment.repository.refresh()
        server.unreachable = offline
        return SpendingsViewModel(
            masterData = environment.repository,
            stock = environment.stockRepository,
            today = { epochDayOf("2026-11-07")!! },
            timeSource = testTimeSource,
        ).also { advanceUntilIdle() }
    }

    private fun requestedRanges() = server.requests
        .filter { it.url.encodedPath == "/api/objects/stock_log" }
        .map { request -> request.url.parameters.getAll("query[]").orEmpty() }
        .filter { query -> query.any { it.startsWith("purchased_date>=") } }

    @Test
    fun opensWithThisMonthsSpendingsByProduct() = runTest(dispatcher) {
        val state = viewModel().state.value!!

        assertIs<SpendingsLoad.Loaded>(state.load)
        assertEquals(listOf("Bread" to 3.0, "Milk" to 3.0), state.rows.map { it.name to it.total })
        assertEquals(
            listOf(listOf("purchased_date>=2026-11-01", "purchased_date<=2026-11-30")),
            requestedRanges(),
        )
    }

    @Test
    fun groupingNeedsNoNewRequest() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setGrouping(SpendingsGrouping.PRODUCT_GROUP)
        advanceUntilIdle()

        val rows = viewModel.state.value!!.rows
        assertEquals(listOf(null to 3.0, "Dairy" to 3.0), rows.map { it.name to it.total })
        assertEquals(1, requestedRanges().size)
    }

    @Test
    fun anotherRangeReadsItsPurchases() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setRange(SpendingsRangePreset.LAST_MONTH)
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(listOf("Milk" to 4.0), state.rows.map { it.name to it.total })
        assertEquals("2026-10-01", requestedRanges().last().first().substringAfter(">="))
        assertTrue(!state.isDefaultFilter)
    }

    @Test
    fun productGroupFilterOnlyAppliesByProduct() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setProductGroup(ProductGroupFilter.Ungrouped)
        advanceUntilIdle()
        assertEquals(listOf("Bread"), viewModel.state.value!!.rows.map { it.name })

        viewModel.setGrouping(SpendingsGrouping.STORE)
        advanceUntilIdle()

        assertEquals(6.0, viewModel.state.value!!.rows.sumOf { it.total })
    }

    @Test
    fun failedRequestOffersToTryAgain() = runTest(dispatcher) {
        val viewModel = viewModel(offline = true)

        assertIs<SpendingsLoad.Failed>(viewModel.state.value!!.load)
        server.unreachable = false
        viewModel.refresh()
        advanceUntilIdle()
        assertIs<SpendingsLoad.Loaded>(viewModel.state.value!!.load)
    }

    private companion object {
        // The fake answers every query alike, the repository keeps what it asked for
        const val PRICE_LOG = """[
            {"id":1,"product_id":1,"stock_id":"a","transaction_type":"purchase","amount":"2",
             "price":"1.5","purchased_date":"2026-11-02","shopping_location_id":"1","undone":0},
            {"id":2,"product_id":2,"stock_id":"b","transaction_type":"purchase","amount":"1",
             "price":"3","purchased_date":"2026-11-04","undone":0},
            {"id":3,"product_id":1,"stock_id":"c","transaction_type":"purchase","amount":"4",
             "price":"1","purchased_date":"2026-10-10","undone":0}
        ]"""
    }
}
