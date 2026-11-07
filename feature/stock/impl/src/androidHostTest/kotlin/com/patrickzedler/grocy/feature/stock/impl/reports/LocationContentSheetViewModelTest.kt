package com.patrickzedler.grocy.feature.stock.impl.reports

import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class LocationContentSheetViewModelTest {

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

    private suspend fun TestScope.viewModel(): LocationContentSheetViewModel {
        environment.repository.refresh()
        return LocationContentSheetViewModel(
            masterData = environment.repository,
            stock = environment.stockRepository,
            language = "en",
            decimalSeparator = '.',
        ).also { advanceUntilIdle() }
    }

    @Test
    fun opensWithTheDownloadedEntriesPerLocation() = runTest(dispatcher) {
        val state = viewModel().state.value!!

        // Milk's two entries in the fridge, one of them opened
        assertEquals(
            listOf(
                LocationSheetGroup(
                    locationId = 1,
                    name = "Fridge",
                    items = listOf(LocationSheetItem(1, "Milk", "3 Pieces", opened = "1")),
                ),
            ),
            state.groups,
        )
    }

    @Test
    fun outOfStockProductsShowAtTheirDefaultLocation() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setOnlyInStock(false)
        advanceUntilIdle()

        val groups = viewModel.state.value!!.groups
        assertEquals(listOf("Fridge", "Pantry"), groups.map { it.name })
        assertEquals(listOf("Milk", "Yoghurt"), groups[0].items.map { it.name })
        assertEquals(LocationSheetItem(2, "Bread", "0 Pieces", null), groups[1].items.single())
    }
}
