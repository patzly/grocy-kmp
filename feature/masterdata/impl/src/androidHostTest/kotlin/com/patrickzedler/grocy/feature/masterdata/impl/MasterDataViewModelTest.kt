package com.patrickzedler.grocy.feature.masterdata.impl

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

    @Test
    fun allCategoriesBeforeTheServerConfigurationIsKnown() = runTest(dispatcher) {
        val viewModel = MasterDataViewModel(environment.repository)
        backgroundScope.launch { viewModel.categories.collect {} }
        advanceUntilIdle()

        assertEquals(MasterDataCategory.entries, viewModel.categories.value)
    }

    @Test
    fun categoriesOfDisabledFeaturesAreHiddenLikeInGrocysMenu() = runTest(dispatcher) {
        environment.server.respond(
            "/api/system/config",
            body = """{"FEATURE_FLAG_STOCK_PRICE_TRACKING":false,"FEATURE_FLAG_CHORES":false,""" +
                """"FEATURE_FLAG_BATTERIES":"0"}""",
        )
        environment.repository.refresh()
        val viewModel = MasterDataViewModel(environment.repository)
        backgroundScope.launch { viewModel.categories.collect {} }
        advanceUntilIdle()

        assertEquals(
            MasterDataCategory.entries - setOf(
                MasterDataCategory.STORES,
                MasterDataCategory.CHORES,
                MasterDataCategory.BATTERIES,
            ),
            viewModel.categories.value,
        )
    }
}
