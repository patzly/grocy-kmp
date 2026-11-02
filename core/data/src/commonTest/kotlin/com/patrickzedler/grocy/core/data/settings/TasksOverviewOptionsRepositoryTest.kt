package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewSortField
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class TasksOverviewOptionsRepositoryTest {

    private val stored = TasksOverviewOptions(grouping = TasksOverviewGrouping.CATEGORY)
    private val settingsStore = FakeSettingsStore(tasksOverviewOptions = stored)

    private fun TestScope.repository() = TasksOverviewOptionsRepository(
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

        repository.update { it.copy(sortField = TasksOverviewSortField.NAME) }

        val expected = stored.copy(sortField = TasksOverviewSortField.NAME)
        assertEquals(expected, settingsStore.tasksOverviewOptions.value)
        assertEquals(expected, repository.options.value)
    }
}
