package com.patrickzedler.grocy.feature.masterdata.impl.overview

import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import io.ktor.util.encodeBase64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ProductOverviewViewModelTest {

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

    /** The list downloaded the products before, the overview opens from there. */
    private suspend fun TestScope.viewModel(productId: Int = 1): ProductOverviewViewModel {
        environment.repository.refresh()
        return openedViewModel(productId)
    }

    private fun TestScope.openedViewModel(productId: Int = 1) =
        ProductOverviewViewModel(environment.repository, decimalSeparator = '.').also {
            backgroundScope.launch { it.state.collect {} }
            it.show(productId)
            advanceUntilIdle()
        }

    @Test
    fun overviewIsDownloadedWithItsPriceHistory() = runTest(dispatcher) {
        val viewModel = viewModel()

        val state = viewModel.state.value
        assertEquals("Milk", state?.name)
        assertEquals("Fridge", state?.locationName)
        val details = state?.details
        assertEquals(AmountText("3 Pieces", opened = "1"), details?.stockAmount)
        assertEquals(UnitPrice(6.0, "Pack", 1.0, "Piece"), details?.lastPrice)
        assertEquals(2, details?.priceHistory?.series?.size)
        assertFalse(viewModel.refresh.value.isRefreshing)
        assertTrue(viewModel.refresh.value.loadedOnce)
    }

    @Test
    fun withoutPriceTrackingNoPriceHistoryIsRequested() = runTest(dispatcher) {
        environment.server.respond(
            "/api/system/config",
            body = """{"FEATURE_FLAG_STOCK_PRICE_TRACKING":"0"}""",
        )

        val viewModel = viewModel()

        assertFalse(viewModel.state.value!!.showPrices)
        assertTrue(viewModel.state.value!!.details!!.priceHistory.isEmpty)
        assertFalse(
            "/api/stock/products/1/price-history" in environment.server.requestedPaths,
        )
    }

    @Test
    fun storedOverviewShowsOfflineWithANote() = runTest(dispatcher) {
        viewModel()
        environment.server.unreachable = true

        val viewModel = openedViewModel()

        assertEquals("3 Pieces", viewModel.state.value?.details?.stockAmount?.amount)
        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun offlineWithoutStoredOverviewShowsTheProductOnly() = runTest(dispatcher) {
        environment.repository.refresh()
        environment.server.unreachable = true

        val viewModel = openedViewModel()

        assertEquals("Milk", viewModel.state.value?.name)
        assertNull(viewModel.state.value?.details)
        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun retryAfterAnErrorDownloadsAgain() = runTest(dispatcher) {
        environment.repository.refresh()
        environment.server.unreachable = true
        val viewModel = openedViewModel()
        environment.server.unreachable = false

        viewModel.refresh()
        advanceUntilIdle()

        assertNull(viewModel.refresh.value.error)
        assertEquals("3 Pieces", viewModel.state.value?.details?.stockAmount?.amount)
    }

    @Test
    fun closedOverviewShowsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.hide()
        advanceUntilIdle()

        assertNull(viewModel.productId.value)
        assertNull(viewModel.state.value)
        assertEquals(RefreshState(), viewModel.refresh.value)
    }

    @Test
    fun everyOpeningDownloadsAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.hide()
        environment.server.respond(
            "/api/stock/products/1",
            body = """{"stock_amount":"5"}""",
        )

        viewModel.show(1)
        advanceUntilIdle()

        assertEquals("5 Pieces", viewModel.state.value?.details?.stockAmount?.amount)
    }

    @Test
    fun closingCancelsTheDownload() = runTest(dispatcher) {
        environment.repository.refresh()
        val viewModel = ProductOverviewViewModel(environment.repository)
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.show(1)
        viewModel.hide()
        advanceUntilIdle()

        assertNull(environment.repository.productOverview(1).first())
        assertFalse(viewModel.refresh.value.isRefreshing)
    }

    @Test
    fun pictureFallsBackToTheListPictureOffline() = runTest(dispatcher) {
        val path = "/api/files/productpictures/${"x1bread.jpg".encodeBase64()}"
        environment.server.respond(path, body = "jpeg")
        val viewModel = viewModel(productId = 2)
        environment.repository.productThumbnail("x1bread.jpg")
        environment.server.unreachable = true

        assertEquals("jpeg", viewModel.picture("x1bread.jpg")?.decodeToString())
        assertNull(viewModel.picture("unknown.jpg"))
    }
}
