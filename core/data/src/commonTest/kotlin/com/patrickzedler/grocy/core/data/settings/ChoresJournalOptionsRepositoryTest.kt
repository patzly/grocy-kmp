package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalSortField
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class ChoresJournalOptionsRepositoryTest {

    private val stored = ChoresJournalOptions(grouping = ChoresJournalGrouping.CHORE)
    private val settingsStore = FakeSettingsStore(choresJournalOptions = stored)

    private fun TestScope.repository() = ChoresJournalOptionsRepository(
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

        repository.update { it.copy(sortField = ChoresJournalSortField.CHORE) }

        val expected = stored.copy(sortField = ChoresJournalSortField.CHORE)
        assertEquals(expected, settingsStore.choresJournalOptions.value)
        assertEquals(expected, repository.options.value)
    }
}
