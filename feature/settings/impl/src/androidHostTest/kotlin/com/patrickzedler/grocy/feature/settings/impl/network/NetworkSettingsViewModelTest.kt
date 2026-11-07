package com.patrickzedler.grocy.feature.settings.impl.network

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.patrickzedler.grocy.core.data.settings.NetworkSettingsRepository
import com.patrickzedler.grocy.core.model.network.NetworkSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
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

class NetworkSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settingsStore = FakeSettingsStore(
        networkSettings = NetworkSettings(httpProxyHost = "proxy.lan", httpProxyPort = 3128),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = NetworkSettingsViewModel(
        NetworkSettingsRepository(
            settingsStore,
            // Unconfined, so the settings are read right away like at app start
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
        ),
    ).also { advanceUntilIdle() }

    private fun TestScope.type(field: TextFieldState, text: String) {
        field.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()
    }

    @Test
    fun storedHostAndPortAreShownRightAway() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals("proxy.lan", viewModel.host.text.toString())
        assertEquals("3128", viewModel.port.text.toString())
    }

    @Test
    fun switchingTorOffersARestart() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setUseTor(true)
        advanceUntilIdle()

        assertTrue(settingsStore.networkSettings.value.useTor)
        assertTrue(viewModel.isRestartOffered)
    }

    @Test
    fun switchingTorBackToHowTheAppRunsOffersNoRestart() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.setUseTor(true)
        advanceUntilIdle()
        viewModel.onRestartAnswered()

        viewModel.setUseTor(false)
        advanceUntilIdle()

        assertFalse(viewModel.isRestartOffered)
        assertFalse(viewModel.restartRequired(settingsStore.networkSettings.value))
    }

    @Test
    fun enabledProxyNeedsARestartWithoutAsking() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.setHttpProxyEnabled(true)
        advanceUntilIdle()

        assertFalse(viewModel.isRestartOffered)
        assertTrue(viewModel.restartRequired(settingsStore.networkSettings.value))
    }

    @Test
    fun enablingTheProxyWithoutHostFocusesIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        type(viewModel.host, "")

        viewModel.setHttpProxyEnabled(true)

        assertTrue(viewModel.focusesHost)
    }

    @Test
    fun typedHostIsSavedOnceTypingPauses() = runTest(dispatcher) {
        val viewModel = viewModel()

        type(viewModel.host, " 10.0.0.2 ")

        assertEquals("10.0.0.2", settingsStore.networkSettings.value.httpProxyHost)
        assertFalse(viewModel.hostInvalid)
    }

    @Test
    fun hostWithSchemeIsNeverSaved() = runTest(dispatcher) {
        val viewModel = viewModel()

        type(viewModel.host, "http://proxy.lan")

        assertEquals("proxy.lan", settingsStore.networkSettings.value.httpProxyHost)
        assertTrue(viewModel.hostInvalid)
    }

    @Test
    fun typedPortIsSavedOnceTypingPauses() = runTest(dispatcher) {
        val viewModel = viewModel()

        type(viewModel.port, "8118")

        assertEquals(8118, settingsStore.networkSettings.value.httpProxyPort)
        assertNull(viewModel.portError)
    }

    @Test
    fun portOutsideItsRangeIsNeverSaved() = runTest(dispatcher) {
        val viewModel = viewModel()

        type(viewModel.port, "70000")

        assertEquals(3128, settingsStore.networkSettings.value.httpProxyPort)
        assertEquals(NetworkSettingsViewModel.PortError, viewModel.portError)
    }
}
