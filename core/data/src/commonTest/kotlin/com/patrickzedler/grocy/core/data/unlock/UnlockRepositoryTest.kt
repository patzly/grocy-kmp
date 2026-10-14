package com.patrickzedler.grocy.core.data.unlock

import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.FakeUnlockChecker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class UnlockRepositoryTest {

    private val settingsStore = FakeSettingsStore()
    private val checker = FakeUnlockChecker()

    @Test
    fun platformWithoutUnlockAppHasNoState() = runTest {
        val repository = UnlockRepository(
            unlockChecker = null,
            settingsStore = settingsStore,
            checkDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        repository.unlockState.test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun stateCombinesInstalledAppsAndSetting() = runTest {
        checker.keyInstalled = true
        val repository = repository()

        repository.unlockState.test {
            assertEquals(
                UnlockState(
                    isKeyInstalled = true,
                    isPlayStoreInstalled = true,
                    checkUnlockKey = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun refreshChecksInstalledAppsAgain() = runTest {
        val repository = repository()

        repository.unlockState.test {
            assertFalse(awaitItem()!!.isKeyInstalled)

            // E.g. the user bought the key on Google Play and returned
            checker.keyInstalled = true
            repository.refresh()

            assertEquals(true, awaitItem()?.isKeyInstalled)
        }
    }

    @Test
    fun ignoreUnlockKeyIsStoredAndEmitted() = runTest {
        val repository = repository()

        repository.unlockState.test {
            assertEquals(true, awaitItem()?.checkUnlockKey)

            repository.ignoreUnlockKey()

            assertEquals(false, awaitItem()?.checkUnlockKey)
            assertFalse(settingsStore.checkUnlockKey.value)
        }
    }

    private fun TestScope.repository() = UnlockRepository(
        unlockChecker = checker,
        settingsStore = settingsStore,
        checkDispatcher = UnconfinedTestDispatcher(testScheduler),
    )
}
