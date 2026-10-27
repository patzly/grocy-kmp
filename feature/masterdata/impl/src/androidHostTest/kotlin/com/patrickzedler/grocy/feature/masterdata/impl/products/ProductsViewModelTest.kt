package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

class ProductsViewModelTest {

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

    private fun TestScope.viewModel() =
        ProductsViewModel(
            environment.repository,
            dispatcher,
            decimalSeparator = '.',
            language = "en",
        ).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }

    @Test
    fun productsAreDownloadedAndShownWithoutDisabledOnes() = runTest(dispatcher) {
        val viewModel = viewModel()

        val names = viewModel.state.value?.items?.map { it.name }
        assertEquals(listOf("Bread", "Milk", "Yoghurt"), names)
        assertFalse(viewModel.refresh.value.isRefreshing)
        assertNull(viewModel.refresh.value.error)
    }

    @Test
    fun listPicturesAreStoredInTheBackground() = runTest(dispatcher) {
        val path = "/api/files/productpictures/eDFicmVhZC5qcGc="
        environment.server.respond(path, body = "jpeg")
        val viewModel = viewModel()
        environment.server.unreachable = true

        val thumbnail = viewModel.thumbnail("x1bread.jpg")

        assertEquals("jpeg", thumbnail?.decodeToString())
    }

    @Test
    fun pictureThatIsNotStoredIsNullOffline() = runTest(dispatcher) {
        environment.server.unreachable = true
        val viewModel = viewModel()

        assertNull(viewModel.thumbnail("x1bread.jpg"))
    }

    @Test
    fun offlineShowsTheSavedProductsWithANote() = runTest(dispatcher) {
        viewModel()
        environment.server.unreachable = true

        val viewModel = viewModel()

        assertEquals(3, viewModel.state.value?.items?.size)
        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun firstDownloadFailingLeavesNoProducts() = runTest(dispatcher) {
        environment.server.unreachable = true

        val viewModel = viewModel()

        assertEquals(false, viewModel.state.value?.hasProducts)
        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun filtersApplyToTheList() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setFilter { it.copy(stockStatus = StockStatusFilter.IN_STOCK) }
        advanceUntilIdle()

        assertEquals(listOf("Milk"), viewModel.state.value?.items?.map { it.name })
    }

    @Test
    fun searchFindsProductsAfterAShortPause() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.showSearch()

        viewModel.searchState.setTextAndPlaceCursorAtEnd("yogurt")
        // In the app every frame does this, the search follows text changes through snapshots
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("Yoghurt"), viewModel.state.value?.items?.map { it.name })
    }

    @Test
    fun clearFilterResetsFiltersAndSearch() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setFilter { it.copy(showDisabled = true, productGroupId = 1) }
        viewModel.showSearch()
        viewModel.searchState.setTextAndPlaceCursorAtEnd("milk")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        viewModel.clearFilter()
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(ProductFilter(), viewModel.state.value?.filter)
        assertFalse(viewModel.isSearchVisible)
        assertTrue(viewModel.searchState.text.isEmpty())
        assertEquals(3, viewModel.state.value?.items?.size)
    }

    @Test
    fun onlyActiveGroupsAreFilterChoices() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(listOf("Dairy"), viewModel.state.value?.productGroups?.map { it.name })
    }

    @Test
    fun sortChangesTheOrder() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setSort(ProductSort(ProductSortField.NAME, ascending = false))
        advanceUntilIdle()

        assertEquals(
            listOf("Yoghurt", "Milk", "Bread"),
            viewModel.state.value?.items?.map { it.name },
        )
    }
}
