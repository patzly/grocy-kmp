package com.patrickzedler.grocy.feature.settings.impl.shoppingmode

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

class ShoppingModeSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsStore = FakeSettingsStore(
        shoppingModeSettings = ShoppingModeSettings(syncIntervalSeconds = 20),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = ShoppingModeSettingsViewModel(
        ShoppingModeSettingsRepository(
            settingsStore,
            // Unconfined, so the settings are read right away like at app start
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ),
    ).also { advanceUntilIdle() }

    @Test
    fun storedIntervalIsShownRightAway() = runTest(dispatcher) {
        assertEquals("20", viewModel().syncInterval.text.toString())
    }

    @Test
    fun typedIntervalIsSavedOnceTypingPauses() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.syncInterval.setTextAndPlaceCursorAtEnd("45")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(45, settingsStore.shoppingModeSettings.value.syncIntervalSeconds)
        assertNull(viewModel.syncIntervalError)
    }

    @Test
    fun intervalBelowTheMinimumIsNeverSaved() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.syncInterval.setTextAndPlaceCursorAtEnd("2")
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(20, settingsStore.shoppingModeSettings.value.syncIntervalSeconds)
        val error = assertIs<SettingError.InvalidNumber>(viewModel.syncIntervalError)
        assertEquals("5", error.min)
    }

    @Test
    fun switchesAreSavedAtOnce() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setAutoSync(false)
        viewModel.setKeepScreenOn(false)
        viewModel.setDoneItemsLast(false)
        advanceUntilIdle()

        val settings = settingsStore.shoppingModeSettings.value
        assertFalse(settings.autoSync)
        assertFalse(settings.keepScreenOn)
        assertFalse(settings.doneItemsLast)
        assertEquals(20, settings.syncIntervalSeconds)
    }
}
