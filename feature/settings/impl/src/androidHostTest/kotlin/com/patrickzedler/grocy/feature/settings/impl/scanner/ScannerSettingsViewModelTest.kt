package com.patrickzedler.grocy.feature.settings.impl.scanner

import com.patrickzedler.grocy.core.data.scanner.ScannerRepository
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import com.patrickzedler.grocy.core.testing.FakeUnlockChecker
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ScannerSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsStore = FakeSettingsStore()
    private val checker = FakeUnlockChecker()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(isPlayApp: Boolean = true): ScannerSettingsViewModel {
        // Unconfined, so the state is read right away like at app start
        val scope = backgroundScope + UnconfinedTestDispatcher(testScheduler)
        val unlockRepository = UnlockRepository(
            unlockChecker = checker.takeIf { isPlayApp },
            settingsStore = settingsStore,
            scope = scope,
            checkDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        return ScannerSettingsViewModel(
            scannerRepository = ScannerRepository(
                settingsStore = settingsStore,
                unlockRepository = unlockRepository,
                availableEngines = if (isPlayApp) {
                    ScannerEngine.entries.toSet()
                } else {
                    setOf(ScannerEngine.ZXING)
                },
                scope = scope,
                canReadText = isPlayApp,
            ),
            unlockRepository = unlockRepository,
        )
    }

    private fun ScannerSettingsViewModel.isMlKitSelectable() =
        isSelectable(ScannerEngine.ML_KIT, unlockState.value)

    @Test
    fun fossAndWebOfferZxingAloneWithoutUnlockHint() = runTest(dispatcher) {
        val viewModel = viewModel(isPlayApp = false)

        assertEquals(listOf(ScannerEngine.ZXING), viewModel.engines)
        assertEquals(ScannerEngine.ZXING, viewModel.activeEngine.value)
        assertTrue(viewModel.isSelectable(ScannerEngine.ZXING, viewModel.unlockState.value))
        assertNull(viewModel.unlockState.value)
        assertFalse(viewModel.showsUnlockHint(viewModel.unlockState.value))
    }

    @Test
    fun lockedPlayAppOffersDisabledMlKitAndTheHint() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(listOf(ScannerEngine.ZXING, ScannerEngine.ML_KIT), viewModel.engines)
        assertTrue(viewModel.isSelectable(ScannerEngine.ZXING, viewModel.unlockState.value))
        assertFalse(viewModel.isMlKitSelectable())
        assertTrue(viewModel.showsUnlockHint(viewModel.unlockState.value))
    }

    @Test
    fun unlockHintNeedsGooglePlay() = runTest(dispatcher) {
        checker.playStoreInstalled = false

        val viewModel = viewModel()

        assertFalse(viewModel.isMlKitSelectable())
        assertFalse(viewModel.showsUnlockHint(viewModel.unlockState.value))
    }

    @Test
    fun installedKeyEnablesMlKitWithoutHint() = runTest(dispatcher) {
        checker.keyInstalled = true

        val viewModel = viewModel()

        assertTrue(viewModel.isMlKitSelectable())
        assertFalse(viewModel.showsUnlockHint(viewModel.unlockState.value))
    }

    @Test
    fun turnedOffKeyCheckEnablesMlKitWithoutHint() = runTest(dispatcher) {
        settingsStore.setCheckUnlockKey(false)

        val viewModel = viewModel()

        assertTrue(viewModel.isMlKitSelectable())
        assertFalse(viewModel.showsUnlockHint(viewModel.unlockState.value))
    }

    @Test
    fun keyBoughtInGooglePlayIsNoticedOnReturn() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertFalse(viewModel.isMlKitSelectable())

        checker.keyInstalled = true
        viewModel.refreshUnlockState()
        advanceUntilIdle()

        assertTrue(viewModel.isMlKitSelectable())
        assertFalse(viewModel.showsUnlockHint(viewModel.unlockState.value))
        // Unlocking alone never switches the engine
        assertEquals(ScannerEngine.ZXING, viewModel.activeEngine.value)
    }

    @Test
    fun chosenEngineIsStoredAndShown() = runTest(dispatcher) {
        checker.keyInstalled = true
        val viewModel = viewModel()

        viewModel.selectEngine(ScannerEngine.ML_KIT)
        advanceUntilIdle()

        assertEquals(ScannerEngine.ML_KIT, settingsStore.scannerEngine.value)
        assertEquals(ScannerEngine.ML_KIT, viewModel.activeEngine.value)
    }

    @Test
    fun removedKeyShowsZxingAndKeepsTheChoiceForLater() = runTest(dispatcher) {
        settingsStore.setScannerEngine(ScannerEngine.ML_KIT)
        val viewModel = viewModel()

        assertEquals(ScannerEngine.ZXING, viewModel.activeEngine.value)
        assertEquals(ScannerEngine.ML_KIT, settingsStore.scannerEngine.value)

        checker.keyInstalled = true
        viewModel.refreshUnlockState()
        advanceUntilIdle()

        assertEquals(ScannerEngine.ML_KIT, viewModel.activeEngine.value)
    }

    @Test
    fun playAppScansDueDatesWithoutUnlocking() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertTrue(viewModel.showsDueDates)
        assertEquals(true, viewModel.scanDueDates.value)
    }

    @Test
    fun fossAndWebShowNoDueDateScanning() = runTest(dispatcher) {
        val viewModel = viewModel(isPlayApp = false)

        assertFalse(viewModel.showsDueDates)
    }

    @Test
    fun turnedOffDueDateScanningIsStoredAndShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setScanDueDates(false)
        advanceUntilIdle()

        assertEquals(false, settingsStore.scanDueDates.value)
        assertEquals(false, viewModel.scanDueDates.value)
    }
}
