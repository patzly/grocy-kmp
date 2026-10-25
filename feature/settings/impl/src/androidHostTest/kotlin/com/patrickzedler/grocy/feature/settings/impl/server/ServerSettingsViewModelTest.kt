package com.patrickzedler.grocy.feature.settings.impl.server

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.server.GrocyVersionCheck
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.demoServerFor
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.FakeGrocyServer.Companion.grocyReleaseJson
import com.patrickzedler.grocy.feature.settings.impl.DemoServerSwitcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ServerSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    private val apiKey = ServerConnection.ApiKey("https://grocy.test", "k")
    private val credentialStore = FakeCredentialStore(apiKey)
    private val masterDataRepository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { credentialStore.connection.value },
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** Like at app start, the server is read before the screen exists. */
    private suspend fun TestScope.viewModel(): ServerSettingsViewModel {
        val serverRepository = ServerRepository(
            connection = credentialStore.connection,
            masterDataRepository = masterDataRepository,
            grocyReleases = server.grocyReleases,
            scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )
        // At app start the database has long answered when the screen opens
        serverRepository.server.first { it != null }
        val authRepository = AuthRepository(credentialStore, server.api) {
            masterDataRepository.clear()
        }
        return ServerSettingsViewModel(
            serverRepository = serverRepository,
            authRepository = authRepository,
            demoServerSwitcher = DemoServerSwitcher(authRepository, serverRepository),
        )
    }

    @Test
    fun serverIsThereWithoutWaiting() = runTest(dispatcher) {
        // No advance: a server that arrives later would let the items jump
        assertEquals(apiKey, viewModel().server.value?.connection)
    }

    @Test
    fun versionIsDownloadedOnOpen() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertTrue(viewModel.isLoadingVersion)
        assertNull(viewModel.server.value?.grocyVersion)

        advanceUntilIdle()

        assertFalse(viewModel.isLoadingVersion)
        assertEquals("4.7.1", viewModel.server.first { it?.grocyVersion != null }?.grocyVersion)
    }

    @Test
    fun offlineVersionStaysUnknownWithoutError() = runTest(dispatcher) {
        server.unreachable = true
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.server.value?.grocyVersion)
        assertFalse(viewModel.isLoadingVersion)
        assertNull(viewModel.reloadState.value.error)
    }

    @Test
    fun reloadedConfigurationOffersARestart() = runTest(dispatcher) {
        server.respond("/api/user/settings", body = """{"stock_decimal_places_amounts":"1"}""")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.reloadConfiguration()
        assertTrue(viewModel.reloadState.value.isRefreshing)
        advanceUntilIdle()

        assertTrue(viewModel.isRestartOffered)
        assertFalse(viewModel.reloadState.value.isRefreshing)
        assertEquals(1, masterDataRepository.userSettings.first().stockDecimalPlacesAmounts)
    }

    @Test
    fun answeredRestartIsNotOfferedAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.reloadConfiguration()
        advanceUntilIdle()

        viewModel.onRestartAnswered()

        assertFalse(viewModel.isRestartOffered)
    }

    @Test
    fun offlineReloadShowsAnErrorAndOffersNoRestart() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        server.unreachable = true

        viewModel.reloadConfiguration()
        advanceUntilIdle()

        assertEquals(RefreshError.OFFLINE, viewModel.reloadState.value.error)
        assertFalse(viewModel.isRestartOffered)
        viewModel.onReloadErrorShown()
        assertNull(viewModel.reloadState.value.error)
    }

    @Test
    fun logoutForgetsTheLoginButKeepsTheShownServer() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.server.first { it?.grocyVersion != null }

        viewModel.logout()
        advanceUntilIdle()

        assertNull(credentialStore.connection.value)
        // The screen slides out with its content instead of turning empty
        assertEquals(ServerState(apiKey, "4.7.1"), viewModel.server.value)
    }

    @Test
    fun otherDemoReplacesTheCurrentOneWithItsConfiguration() = runTest(dispatcher) {
        credentialStore.save(ServerConnection.Demo())
        val viewModel = viewModel()
        advanceUntilIdle()
        server.requests.clear()
        val german = demoServerFor("de")!!

        viewModel.switchDemoServer(german)
        assertTrue(viewModel.isSwitchingDemoServer)
        advanceUntilIdle()

        assertEquals(ServerConnection.Demo(german.url), credentialStore.connection.value)
        assertFalse(viewModel.isSwitchingDemoServer)
        assertTrue("/api/system/config" in server.requestedPaths)
        assertEquals("4.7.1", viewModel.server.first { it?.grocyVersion != null }?.grocyVersion)
    }

    @Test
    fun unreachableDemoKeepsTheCurrentOne() = runTest(dispatcher) {
        credentialStore.save(ServerConnection.Demo())
        val viewModel = viewModel()
        advanceUntilIdle()
        server.unreachable = true

        viewModel.switchDemoServer(demoServerFor("de")!!)
        advanceUntilIdle()

        assertEquals(ServerConnection.Demo(), credentialStore.connection.value)
        assertEquals(ServerSettingsError.DEMO_SERVER_UNREACHABLE, viewModel.error)
        viewModel.onErrorShown()
        assertNull(viewModel.error)
    }

    @Test
    fun currentDemoIsNotSwitchedTo() = runTest(dispatcher) {
        credentialStore.save(ServerConnection.Demo())
        val viewModel = viewModel()
        advanceUntilIdle()
        server.requests.clear()

        viewModel.switchDemoServer(demoServerFor("en")!!)
        advanceUntilIdle()

        assertEquals(emptyList(), server.requestedPaths)
    }

    @Test
    fun newerGrocyIsShownWithItsRelease() = runTest(dispatcher) {
        server.respond(FakeGrocyServer.GROCY_RELEASE_PATH, body = grocyReleaseJson("4.8.0"))
        val viewModel = viewModel()

        viewModel.checkGrocyVersion()
        assertTrue(viewModel.isCheckingVersion)
        advanceUntilIdle()

        assertFalse(viewModel.isCheckingVersion)
        assertEquals(
            GrocyVersionCheck.Available(
                latestVersion = "4.8.0",
                serverVersion = "4.7.1",
                releaseUrl = "https://github.com/grocy/grocy/releases/tag/v4.8.0",
            ),
            viewModel.versionCheck,
        )
        viewModel.onVersionCheckShown()
        assertNull(viewModel.versionCheck)
    }

    @Test
    fun failedVersionCheckIsAnError() = runTest(dispatcher) {
        server.unreachable = true
        val viewModel = viewModel()

        viewModel.checkGrocyVersion()
        advanceUntilIdle()

        assertNull(viewModel.versionCheck)
        assertEquals(ServerSettingsError.VERSION_CHECK_FAILED, viewModel.error)
    }
}
