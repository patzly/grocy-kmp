package com.patrickzedler.grocy.core.data.server

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
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

    private fun TestScope.repository() =
        ServerRepository(credentialStore.connection, masterDataRepository, backgroundScope)

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
    fun noServerWhileLoggedOut() = runTest {
        val repository = repository()
        repository.server.filterNotNull().first()

        credentialStore.clear()

        assertNull(repository.server.filter { it == null }.first())
    }
}
