package com.patrickzedler.grocy.feature.settings.impl.general

import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.testing.FakeAppLanguageStore
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class GeneralSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsStore = FakeSettingsStore(
        appearance = AppearanceSettings(hue = 100f),
    )
    private val languageStore = FakeAppLanguageStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Like at app start, the stored appearance is read before the screen exists. */
    private fun TestScope.viewModel() = GeneralSettingsViewModel(
        AppearanceRepository(
            settingsStore,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ),
        languageStore,
    )

    @Test
    fun appearanceIsThereWithoutWaiting() = runTest {
        // No advance: a value that arrives later would let the controls jump
        assertEquals(AppearanceSettings(hue = 100f), viewModel().appearance.value)
    }

    @Test
    fun changesKeepTheOtherValues() = runTest {
        val viewModel = viewModel()

        viewModel.updateAppearance { it.copy(theme = AppTheme.DARK) }
        advanceUntilIdle()
        viewModel.updateAppearance { it.copy(contrast = AppContrast.HIGH) }
        advanceUntilIdle()

        assertEquals(
            AppearanceSettings(theme = AppTheme.DARK, hue = 100f, contrast = AppContrast.HIGH),
            settingsStore.appearance.value,
        )
    }

    @Test
    fun colorSourceIsStored() = runTest {
        val viewModel = viewModel()

        viewModel.updateAppearance { it.copy(color = AppColor.DYNAMIC) }
        advanceUntilIdle()

        assertEquals(AppColor.DYNAMIC, settingsStore.appearance.value.color)
    }

    @Test
    fun chosenLanguageGoesToTheLanguageStore() = runTest {
        val viewModel = viewModel()

        viewModel.selectLanguage("en")

        assertEquals("en", languageStore.language.value)
        assertEquals("en", viewModel.language.value)
    }

    @Test
    fun systemLanguageIsNull() = runTest {
        languageStore.setLanguage("en")
        val viewModel = viewModel()

        viewModel.selectLanguage(null)

        assertNull(viewModel.language.value)
    }
}
