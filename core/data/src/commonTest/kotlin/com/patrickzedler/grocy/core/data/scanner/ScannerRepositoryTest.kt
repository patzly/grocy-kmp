package com.patrickzedler.grocy.core.data.scanner

import app.cash.turbine.test
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.FakeUnlockChecker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class ScannerRepositoryTest {

    private val settingsStore = FakeSettingsStore(scannerEngine = ScannerEngine.ML_KIT)
    private val checker = FakeUnlockChecker()

    private fun TestScope.repository(
        availableEngines: Set<ScannerEngine> = ScannerEngine.entries.toSet(),
        hasUnlockApp: Boolean = true,
    ) = ScannerRepository(
        settingsStore = settingsStore,
        unlockRepository = UnlockRepository(
            unlockChecker = checker.takeIf { hasUnlockApp },
            settingsStore = settingsStore,
            scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            checkDispatcher = UnconfinedTestDispatcher(testScheduler),
        ),
        availableEngines = availableEngines,
        scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun mlKitIsUsedWhenUnlocked() = runTest {
        checker.keyInstalled = true

        repository().activeEngine.test {
            assertEquals(ScannerEngine.ML_KIT, awaitItem())
        }
    }

    @Test
    fun mlKitIsUsedWhenUnlockCheckIsTurnedOff() = runTest {
        settingsStore.setCheckUnlockKey(false)

        repository().activeEngine.test {
            assertEquals(ScannerEngine.ML_KIT, awaitItem())
        }
    }

    @Test
    fun lockedMlKitFallsBackToZxing() = runTest {
        repository().activeEngine.test {
            assertEquals(ScannerEngine.ZXING, awaitItem())
        }
    }

    @Test
    fun engineMissingOnPlatformFallsBackToZxing() = runTest {
        checker.keyInstalled = true

        repository(availableEngines = setOf(ScannerEngine.ZXING), hasUnlockApp = false)
            .activeEngine.test {
                assertEquals(ScannerEngine.ZXING, awaitItem())
            }
    }

    @Test
    fun activeEngineIsThereRightAwayForTheFirstFrame() = runTest {
        checker.keyInstalled = true

        assertEquals(ScannerEngine.ML_KIT, repository().activeEngine.value)
    }

    @Test
    fun zxingIsAlwaysUsableMlKitOnlyWhereShippedAndUnlocked() = runTest {
        val unlocked = UnlockState(
            isKeyInstalled = true,
            isPlayStoreInstalled = true,
            checkUnlockKey = true,
        )
        val locked = unlocked.copy(isKeyInstalled = false)
        val play = repository()
        val foss = repository(availableEngines = setOf(ScannerEngine.ZXING))

        assertTrue(play.isUsable(ScannerEngine.ZXING, locked))
        assertTrue(foss.isUsable(ScannerEngine.ZXING, null))
        assertTrue(play.isUsable(ScannerEngine.ML_KIT, unlocked))
        assertFalse(play.isUsable(ScannerEngine.ML_KIT, locked))
        assertFalse(play.isUsable(ScannerEngine.ML_KIT, null))
        assertFalse(foss.isUsable(ScannerEngine.ML_KIT, unlocked))
    }

    @Test
    fun unlockingKeepsZxingUntilUserSelectsMlKit() = runTest {
        settingsStore.setScannerEngine(ScannerEngine.ZXING)
        val repository = repository()

        repository.activeEngine.test {
            assertEquals(ScannerEngine.ZXING, awaitItem())

            settingsStore.setCheckUnlockKey(false)
            expectNoEvents()
            assertEquals(ScannerEngine.ZXING, settingsStore.scannerEngine.value)

            repository.selectEngine(ScannerEngine.ML_KIT)
            assertEquals(ScannerEngine.ML_KIT, awaitItem())
        }
    }

    @Test
    fun selectionIsStoredAndApplied() = runTest {
        checker.keyInstalled = true
        val repository = repository()

        repository.activeEngine.test {
            assertEquals(ScannerEngine.ML_KIT, awaitItem())

            repository.selectEngine(ScannerEngine.ZXING)

            assertEquals(ScannerEngine.ZXING, awaitItem())
            assertEquals(ScannerEngine.ZXING, settingsStore.scannerEngine.value)
        }
    }
}
