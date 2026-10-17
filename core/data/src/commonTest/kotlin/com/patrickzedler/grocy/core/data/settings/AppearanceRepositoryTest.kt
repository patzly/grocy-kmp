package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class AppearanceRepositoryTest {

    private val settingsStore = FakeSettingsStore(appearance = AppearanceSettings(hue = 42f))

    private fun TestScope.repository() = AppearanceRepository(
        settingsStore,
        // Unconfined, so the settings are read right away like at app start
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun storedAppearanceIsReadBeforeAnyScreenAsksForIt() = runTest {
        assertEquals(AppearanceSettings(hue = 42f), repository().appearance.value)
    }

    @Test
    fun updatesKeepTheOtherValues() = runTest {
        val repository = repository()

        repository.update { it.copy(theme = AppTheme.DARK) }
        repository.update { it.copy(contrast = AppContrast.HIGH) }

        val expected =
            AppearanceSettings(theme = AppTheme.DARK, hue = 42f, contrast = AppContrast.HIGH)
        assertEquals(expected, settingsStore.appearance.value)
        assertEquals(expected, repository.appearance.value)
    }
}
