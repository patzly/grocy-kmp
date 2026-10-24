package com.patrickzedler.grocy.feature.settings.impl.server

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
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
            scope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
        )
        // At app start the database has long answered when the screen opens
        serverRepository.server.first { it != null }
        val authRepository = AuthRepository(credentialStore, server.api) {
            masterDataRepository.clear()
        }
        return ServerSettingsViewModel(serverRepository, authRepository)
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
}
