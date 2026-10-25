package com.patrickzedler.grocy.feature.masterdata.impl

import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class MasterDataViewModelTest {

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

    // Read eagerly like the app's, which reads it at start
    private fun TestScope.serverConfig() = environment.repository.serverConfig.stateIn(
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
        SharingStarted.Eagerly,
        null,
    )

    private fun TestScope.viewModel() =
        MasterDataViewModel(environment.repository, serverConfig()).also {
            backgroundScope.launch { it.state.collect {} }
            advanceUntilIdle()
        }

    @Test
    fun categoriesShowRightAwayFromTheConfigReadAtStart() = runTest(dispatcher) {
        val config = MutableStateFlow<ServerConfig?>(ServerConfig(chores = false))

        val viewModel = MasterDataViewModel(environment.repository, config)

        // Before the database answered anything
        val state = viewModel.state.value
        assertEquals(MasterDataCategory.entries - MasterDataCategory.CHORES, state?.categories)
        assertNull(state?.counts?.get(MasterDataCategory.PRODUCTS))
    }

    @Test
    fun allCategoriesWithTheServersDefaultFeatures() = runTest(dispatcher) {
        assertEquals(MasterDataCategory.entries, viewModel().state.value?.categories)
    }

    @Test
    fun categoriesOfDisabledFeaturesAreHiddenLikeInGrocysMenu() = runTest(dispatcher) {
        environment.server.respond(
            "/api/system/config",
            body = """{"FEATURE_FLAG_STOCK_PRICE_TRACKING":false,"FEATURE_FLAG_CHORES":false,""" +
                """"FEATURE_FLAG_BATTERIES":"0"}""",
        )

        val viewModel = viewModel()

        assertEquals(
            MasterDataCategory.entries - setOf(
                MasterDataCategory.STORES,
                MasterDataCategory.CHORES,
                MasterDataCategory.BATTERIES,
            ),
            viewModel.state.value?.categories,
        )
    }

    @Test
    fun productCountIncludesDisabledProducts() = runTest(dispatcher) {
        assertEquals(4, viewModel().state.value?.counts?.get(MasterDataCategory.PRODUCTS))
    }

    @Test
    fun everyCategoryShowsItsNumberOfObjects() = runTest(dispatcher) {
        val counts = viewModel().state.value?.counts

        assertEquals(2, counts?.get(MasterDataCategory.CHORES))
        assertEquals(1, counts?.get(MasterDataCategory.BATTERIES))
        assertEquals(1, counts?.get(MasterDataCategory.TASK_CATEGORIES))
        assertEquals(1, counts?.get(MasterDataCategory.USERENTITIES))
    }

    @Test
    fun noProductsAfterADownloadIsZero() = runTest(dispatcher) {
        environment.server.respond("/api/objects/products", body = "[]")

        assertEquals(0, viewModel().state.value?.counts?.get(MasterDataCategory.PRODUCTS))
    }

    @Test
    fun countIsUnknownWhileNothingWasDownloaded() = runTest(dispatcher) {
        environment.server.unreachable = true

        val viewModel = viewModel()

        // An empty database is no proof that the server has no products
        assertNull(viewModel.state.value?.counts?.get(MasterDataCategory.PRODUCTS))
        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun pullToRefreshDownloadsAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        environment.server.respond("/api/objects/products", body = "[]")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value?.counts?.get(MasterDataCategory.PRODUCTS))
    }
}
