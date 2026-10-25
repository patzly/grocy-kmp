package com.patrickzedler.grocy.core.data.server

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.testing.FakeGrocyServer.Companion.grocyReleaseJson
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class ServerRepositoryTest {

    private val server = FakeGrocyServer()
    private val database = createInMemoryGrocyDatabase()
    private val apiKey = ServerConnection.ApiKey("https://grocy.test", "k")
    private val credentialStore = FakeCredentialStore(apiKey)
    private val masterDataRepository = MasterDataRepository(
        api = server.api,
        database = database,
        connection = { credentialStore.connection.value },
    )

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun TestScope.repository() = ServerRepository(
        connection = credentialStore.connection,
        masterDataRepository = masterDataRepository,
        grocyReleases = server.grocyReleases,
        scope = backgroundScope,
    )

    @Test
    fun serverShowsTheConnectionAndTheDownloadedVersion() = runTest {
        val repository = repository()

        repository.refreshVersion()

        val state = repository.server.filterNotNull().filter { it.grocyVersion != null }.first()
        assertEquals(ServerState(apiKey, grocyVersion = "4.7.1"), state)
        assertFalse(state.isDemo)
    }

    @Test
    fun versionIsUnknownUntilDownloaded() = runTest {
        val state = repository().server.filterNotNull().first()

        assertNull(state.grocyVersion)
    }

    @Test
    fun demoServerIsRecognized() = runTest {
        credentialStore.save(ServerConnection.Demo())

        val state = repository().server.filterNotNull().first()

        assertTrue(state.isDemo)
    }

    @Test
    fun serverConfigIsTheStoredOne() = runTest {
        val repository = repository()
        server.respond("/api/system/config", body = """{"FEATURE_FLAG_STOCK_PRICE_TRACKING":0}""")

        masterDataRepository.reloadConfiguration()

        val config = repository.serverConfig.filterNotNull().filter { !it.stockPriceTracking }
        assertFalse(config.first().stockPriceTracking)
    }

    @Test
    fun noServerWhileLoggedOut() = runTest {
        val repository = repository()
        repository.server.filterNotNull().first()

        credentialStore.clear()

        assertNull(repository.server.filter { it == null }.first())
    }

    @Test
    fun serverWithTheNewestReleaseIsUpToDate() = runTest {
        val check = repository().checkGrocyVersion()

        assertEquals(GrocyVersionCheck.UpToDate("4.7.1"), check)
    }

    @Test
    fun newerReleaseIsOfferedWithItsPage() = runTest {
        server.respond(FakeGrocyServer.GROCY_RELEASE_PATH, body = grocyReleaseJson("4.10.0"))

        val check = repository().checkGrocyVersion()

        assertEquals(
            GrocyVersionCheck.Available(
                latestVersion = "4.10.0",
                serverVersion = "4.7.1",
                releaseUrl = "https://github.com/grocy/grocy/releases/tag/v4.10.0",
            ),
            check,
        )
    }

    @Test
    fun checkDownloadsTheServersVersionAnew() = runTest {
        val repository = repository()
        repository.refreshVersion()
        // The server was updated in the meantime
        server.respond(
            FakeGrocyServer.SYSTEM_INFO_PATH,
            body = """{"grocy_version":{"Version":"4.10.0","ReleaseDate":"2026-12-01"}}""",
        )
        server.respond(FakeGrocyServer.GROCY_RELEASE_PATH, body = grocyReleaseJson("4.10.0"))

        assertEquals(GrocyVersionCheck.UpToDate("4.10.0"), repository.checkGrocyVersion())
    }

    @Test
    fun unreachableServerIsComparedByItsStoredVersion() = runTest {
        val repository = repository()
        repository.refreshVersion()
        server.respond(FakeGrocyServer.SYSTEM_INFO_PATH, status = HttpStatusCode.BadGateway)
        server.respond(FakeGrocyServer.GROCY_RELEASE_PATH, body = grocyReleaseJson("5.0.0"))

        val check = repository.checkGrocyVersion()

        assertEquals("4.7.1", (check as GrocyVersionCheck.Available).serverVersion)
    }

    @Test
    fun unknownServerVersionFailsTheCheck() = runTest {
        server.respond(FakeGrocyServer.SYSTEM_INFO_PATH, status = HttpStatusCode.BadGateway)

        assertEquals(GrocyVersionCheck.Failed, repository().checkGrocyVersion())
    }

    @Test
    fun unreachableGitHubFailsTheCheck() = runTest {
        server.respond(FakeGrocyServer.GROCY_RELEASE_PATH, status = HttpStatusCode.Forbidden)

        assertEquals(GrocyVersionCheck.Failed, repository().checkGrocyVersion())
    }
}
