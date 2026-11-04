package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.BehaviorSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class BehaviorSettingsRepositoryTest {

    @Test
    fun theStoredSettingsAreThereRightAwayForTheFirstFrame() = runTest {
        val store = FakeSettingsStore(behaviorSettings = BehaviorSettings(dateKeyboard = true))

        val repository = BehaviorSettingsRepository(
            store,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )

        assertEquals(true, repository.settings.value?.dateKeyboard)
    }

    @Test
    fun aChangeIsStored() = runTest {
        val store = FakeSettingsStore()
        val repository = BehaviorSettingsRepository(
            store,
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )

        repository.update { it.copy(barcodeUsesAmount = false) }

        assertEquals(false, store.behaviorSettings.value.barcodeUsesAmount)
    }
}
