package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.network.NetworkProxy
import com.patrickzedler.grocy.core.model.network.NetworkSettings
import com.patrickzedler.grocy.core.testing.FakeSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class NetworkSettingsRepositoryTest {

    private val proxySettings = NetworkSettings(httpProxyEnabled = true, httpProxyHost = "proxy.lan")

    private fun TestScope.repository(settingsStore: FakeSettingsStore) = NetworkSettingsRepository(
        settingsStore,
        // Unconfined, so the settings are read right away like at app start
        backgroundScope + UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun storedSettingsAreReadBeforeTheScreenAsksForThem() = runTest {
        val repository = repository(FakeSettingsStore(networkSettings = proxySettings))

        assertEquals(proxySettings, repository.settings.value)
        assertEquals(NetworkProxy.Http("proxy.lan", 8080), repository.proxy())
    }

    @Test
    fun changesApplyOnlyAfterARestart() = runTest {
        val settingsStore = FakeSettingsStore()
        val repository = repository(settingsStore)

        repository.update { proxySettings }

        assertEquals(NetworkProxy.System, repository.proxy())
        assertEquals(NetworkProxy.Http("proxy.lan", 8080), repository(settingsStore).proxy())
    }

    @Test
    fun torTurnedOffKeepsTorUntilARestart() = runTest {
        val settingsStore = FakeSettingsStore(networkSettings = NetworkSettings(useTor = true))
        val repository = repository(settingsStore)

        repository.update { it.copy(useTor = false) }

        assertEquals(NetworkProxy.Tor, repository.proxy())
    }

    @Test
    fun restartIsRequiredOnlyWhenRequestsWouldGoElsewhere() = runTest {
        val repository = repository(FakeSettingsStore(networkSettings = NetworkSettings()))

        // The host alone does not send anything elsewhere while the proxy is off
        assertFalse(repository.restartRequired(NetworkSettings(httpProxyHost = "proxy.lan")))
        assertTrue(repository.restartRequired(proxySettings))
        assertTrue(repository.restartRequired(NetworkSettings(useTor = true)))
    }

    @Test
    fun requestsWaitForTheStoredSettings() = runTest {
        val repository = NetworkSettingsRepository(
            FakeSettingsStore(networkSettings = NetworkSettings(useTor = true)),
            // Not read yet: the scope runs only when the test waits
            backgroundScope,
        )

        val proxy = async { repository.proxy() }

        assertEquals(NetworkProxy.Tor, proxy.await())
    }
}
