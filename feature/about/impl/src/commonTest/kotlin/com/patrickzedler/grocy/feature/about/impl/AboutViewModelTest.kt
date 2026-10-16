package com.patrickzedler.grocy.feature.about.impl

import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.data.update.UpdateRepository
import com.patrickzedler.grocy.core.model.UpdateCheck
import com.patrickzedler.grocy.core.testing.FakeAppUpdater
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class AboutViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsStore = FakeSettingsStore()
    private val checker = FakeUnlockChecker()
    private val updater = FakeAppUpdater()
    private val updateRepository = UpdateRepository(updater)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Creates the ViewModel and keeps its state collected, as the screen does. */
    private fun TestScope.viewModel(hasUnlockApp: Boolean = true): AboutViewModel {
        val repository = UnlockRepository(
            unlockChecker = checker.takeIf { hasUnlockApp },
            settingsStore = settingsStore,
            checkDispatcher = dispatcher,
        )
        return AboutViewModel(repository, updateRepository).also { viewModel ->
            backgroundScope.launch { viewModel.unlockState.collect {} }
            advanceUntilIdle()
        }
    }

    private fun AboutViewModel.longClickKey(times: Int) = repeat(times) { onKeyLongClick() }

    @Test
    fun withoutUnlockAppThereIsNoState() = runTest {
        assertNull(viewModel(hasUnlockApp = false).unlockState.value)
    }

    @Test
    fun stateReflectsInstalledApps() = runTest {
        checker.keyInstalled = true

        val state = viewModel().unlockState.value

        assertEquals(true, state?.isKeyInstalled)
        assertEquals(true, state?.isPlayStoreInstalled)
    }

    @Test
    fun refreshPicksUpNewlyInstalledKey() = runTest {
        val viewModel = viewModel()
        assertEquals(false, viewModel.unlockState.value?.isKeyInstalled)

        checker.keyInstalled = true
        viewModel.refreshUnlockState()
        advanceUntilIdle()

        assertEquals(true, viewModel.unlockState.value?.isKeyInstalled)
    }

    @Test
    fun tenLongClicksIgnoreTheMissingKey() = runTest {
        val viewModel = viewModel()

        viewModel.longClickKey(9)
        advanceUntilIdle()
        assertTrue(settingsStore.checkUnlockKey.value)

        viewModel.longClickKey(1)
        advanceUntilIdle()
        assertFalse(settingsStore.checkUnlockKey.value)
        assertEquals(false, viewModel.unlockState.value?.checkUnlockKey)
    }

    @Test
    fun longClicksDoNothingWhenKeyIsInstalled() = runTest {
        checker.keyInstalled = true
        val viewModel = viewModel()

        viewModel.longClickKey(10)
        advanceUntilIdle()

        assertTrue(settingsStore.checkUnlockKey.value)
    }

    @Test
    fun longClicksDoNothingWithoutUnlockApp() = runTest {
        val viewModel = viewModel(hasUnlockApp = false)

        viewModel.longClickKey(10)
        advanceUntilIdle()

        assertTrue(settingsStore.checkUnlockKey.value)
    }

    @Test
    fun updateCheckShowsItsResult() = runTest {
        updater.result = UpdateCheck.Available("1.2.0")
        val viewModel = viewModel()

        viewModel.checkForUpdate()
        assertTrue(viewModel.isCheckingForUpdate)
        advanceUntilIdle()

        assertFalse(viewModel.isCheckingForUpdate)
        assertEquals(UpdateCheck.Available("1.2.0"), viewModel.updateCheck)
    }

    @Test
    fun upToDateIsShownToo() = runTest {
        val viewModel = viewModel()

        viewModel.checkForUpdate()
        advanceUntilIdle()

        assertEquals(UpdateCheck.UpToDate, viewModel.updateCheck)
    }

    @Test
    fun secondTapWhileCheckingStartsNoSecondCheck() = runTest {
        val viewModel = viewModel()

        viewModel.checkForUpdate()
        viewModel.checkForUpdate()
        advanceUntilIdle()

        assertEquals(1, updater.checks)
    }

    @Test
    fun dismissingAFoundUpdatePostponesIt() = runTest {
        updater.result = UpdateCheck.Available("1.2.0")
        updater.checksAutomatically = true
        val viewModel = viewModel()
        viewModel.checkForUpdate()
        advanceUntilIdle()

        viewModel.dismissUpdateCheck()
        backgroundScope.launch { updateRepository.runAutomaticChecks() }
        advanceTimeBy(1)

        assertNull(viewModel.updateCheck)
        assertNull(updateRepository.pendingUpdate.value)
    }

    @Test
    fun updateInstallsAndClosesTheDialog() = runTest {
        updater.result = UpdateCheck.Available("1.2.0")
        val viewModel = viewModel()
        viewModel.checkForUpdate()
        advanceUntilIdle()

        viewModel.installUpdate()
        advanceUntilIdle()

        assertNull(viewModel.updateCheck)
        assertEquals(1, updater.installs)
    }
}
