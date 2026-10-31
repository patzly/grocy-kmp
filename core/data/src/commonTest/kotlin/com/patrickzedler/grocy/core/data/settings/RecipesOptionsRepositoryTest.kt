package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.recipes.RecipesView
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class RecipesOptionsRepositoryTest {

    private val stored = RecipesOptions(ascending = false)
    private val settingsStore = FakeSettingsStore(recipesOptions = stored)

    private fun TestScope.repository() = RecipesOptionsRepository(
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

        repository.update { it.copy(view = RecipesView.GALLERY) }

        val expected = stored.copy(view = RecipesView.GALLERY)
        assertEquals(expected, settingsStore.recipesOptions.value)
        assertEquals(expected, repository.options.value)
    }
}
