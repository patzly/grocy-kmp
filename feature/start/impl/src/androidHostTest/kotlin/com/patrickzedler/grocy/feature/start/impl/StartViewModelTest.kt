package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class StartViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "key") }
    private val repository = MasterDataRepository(server.api, database, connection)
    private val stock = StockRepository(server.api, database, connection)
    private val config = MutableStateFlow<ServerConfig?>(ServerConfig())

    private fun TestScope.viewModel() = StartViewModel(
        repository = repository,
        stock = stock,
        serverConfig = config,
        today = { epochDayOf("2026-10-25")!! },
    ).also { backgroundScope.launch { it.state.collect {} } }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun openingDownloadsTheServersDataAndTheStockOverview() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(repository.products.first().isNotEmpty())
        assertTrue(stock.productStockInfo.first().isNotEmpty())
        assertTrue(viewModel.refresh.value.loadedOnce)
    }

    @Test
    fun stockSectionCountsLikeGrocysStockOverview() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val summary = viewModel.state.value!!.stock!!
        assertEquals(1, summary.productCount)
        assertEquals(3.0, summary.totalValue)
        // Milk's best before date lies five days back, Yoghurt is below its min. stock amount
        assertEquals(1, summary.overdueCount)
        assertEquals(1, summary.belowMinStockCount)
        assertEquals(5, viewModel.state.value!!.dueSoonDays)
    }

    @Test
    fun masterDataSectionCountsTheAvailableCategoriesWithObjects() = runTest(dispatcher) {
        config.value = ServerConfig(stockPriceTracking = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        val counts = viewModel.state.value!!.masterDataCounts.toMap()
        assertEquals(4, counts[MasterDataCount.PRODUCTS])
        // Stores belong to price tracking
        assertNull(counts[MasterDataCount.STORES])
        // In the order of Grocy's menu
        assertEquals(MasterDataCount.PRODUCTS, counts.keys.first())
    }

    @Test
    fun stockSectionTellsNothingWithoutTheStockFeature() = runTest(dispatcher) {
        config.value = ServerConfig(stock = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.stock)
        assertTrue("/api/stock/volatile" !in server.requestedPaths)
    }

    @Test
    fun offlineRefreshReportsIt() = runTest(dispatcher) {
        server.unreachable = true
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }
}
