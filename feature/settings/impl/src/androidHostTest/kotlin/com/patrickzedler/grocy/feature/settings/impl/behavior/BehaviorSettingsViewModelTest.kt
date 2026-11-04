package com.patrickzedler.grocy.feature.settings.impl.behavior

import com.patrickzedler.grocy.core.data.settings.BehaviorSettingsRepository
import com.patrickzedler.grocy.core.model.DateInputOrder
import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class BehaviorSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = BehaviorSettingsViewModel(
        BehaviorSettingsRepository(
            store,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ),
    )

    @Test
    fun databasesAreTurnedOnAndOffOneByOne() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setSource(ProductFactsSource.FOOD, true)
        advanceUntilIdle()
        viewModel.setSource(ProductFactsSource.BEAUTY, true)
        advanceUntilIdle()
        viewModel.setSource(ProductFactsSource.FOOD, false)
        advanceUntilIdle()

        assertEquals(
            setOf(ProductFactsSource.BEAUTY),
            store.behaviorSettings.value.productFactsSources,
        )
    }

    @Test
    fun theOtherSettingsAreSavedAtOnce() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setDateKeyboard(true)
        advanceUntilIdle()
        viewModel.setDateInputOrder(DateInputOrder.YEAR_FIRST)
        advanceUntilIdle()
        viewModel.setBarcodeUsesAmount(false)
        advanceUntilIdle()

        val settings = store.behaviorSettings.value
        assertEquals(true, settings.dateKeyboard)
        assertEquals(DateInputOrder.YEAR_FIRST, settings.dateInputOrder)
        assertEquals(false, settings.barcodeUsesAmount)
    }
}
