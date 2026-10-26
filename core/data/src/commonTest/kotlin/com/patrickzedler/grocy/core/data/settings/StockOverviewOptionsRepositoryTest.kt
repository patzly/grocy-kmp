package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class StockOverviewOptionsRepositoryTest {

    private val stored = StockOverviewOptions(ascending = false)
    private val settingsStore = FakeSettingsStore(stockOverviewOptions = stored)

    private fun TestScope.repository() = StockOverviewOptionsRepository(
        settingsStore,
        // Unconfined, so the options are read right away like at app start
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun storedOptionsAreReadBeforeTheScreenAsksForThem() = runTest {
        assertEquals(stored, repository().options.value)
    }

    @Test
    fun updatesKeepTheOtherOptions() = runTest {
        val repository = repository()

        repository.update { it.copy(grouping = StockOverviewGrouping.PRODUCT_GROUP) }

        val expected = stored.copy(grouping = StockOverviewGrouping.PRODUCT_GROUP)
        assertEquals(expected, settingsStore.stockOverviewOptions.value)
        assertEquals(expected, repository.options.value)
    }
}
