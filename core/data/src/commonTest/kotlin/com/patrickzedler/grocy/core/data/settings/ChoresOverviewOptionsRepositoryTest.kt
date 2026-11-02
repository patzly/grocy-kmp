package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewSortField
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class ChoresOverviewOptionsRepositoryTest {

    private val stored = ChoresOverviewOptions(grouping = ChoresOverviewGrouping.ASSIGNED_TO)
    private val settingsStore = FakeSettingsStore(choresOverviewOptions = stored)

    private fun TestScope.repository() = ChoresOverviewOptionsRepository(
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

        repository.update { it.copy(sortField = ChoresOverviewSortField.NAME) }

        val expected = stored.copy(sortField = ChoresOverviewSortField.NAME)
        assertEquals(expected, settingsStore.choresOverviewOptions.value)
        assertEquals(expected, repository.options.value)
    }
}
