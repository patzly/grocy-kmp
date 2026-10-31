package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class ShoppingModeSettingsRepositoryTest {

    private val stored = ShoppingModeSettings(autoSync = false, syncIntervalSeconds = 30)
    private val settingsStore = FakeSettingsStore(shoppingModeSettings = stored)

    private fun TestScope.repository() = ShoppingModeSettingsRepository(
        settingsStore,
        // Unconfined, so the settings are read right away like at app start
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun storedSettingsAreReadBeforeTheScreenAsksForThem() = runTest {
        assertEquals(stored, repository().settings.value)
    }

    @Test
    fun updatesKeepTheOtherSettings() = runTest {
        val repository = repository()

        repository.update { it.copy(keepScreenOn = false) }

        val expected = stored.copy(keepScreenOn = false)
        assertEquals(expected, settingsStore.shoppingModeSettings.value)
        assertEquals(expected, repository.settings.value)
    }
}
