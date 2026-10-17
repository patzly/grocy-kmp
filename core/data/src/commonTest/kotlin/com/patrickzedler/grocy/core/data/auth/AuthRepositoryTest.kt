package com.patrickzedler.grocy.core.data.auth

import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class AuthRepositoryTest {

    private val credentialStore = FakeCredentialStore()
    private val server = FakeGrocyServer()
    private var localDataClears = 0
    private val repository = AuthRepository(credentialStore, server.api) { localDataClears++ }

    private val connection = ServerConnection.ApiKey("https://grocy.example.com", "secret")

    @Test
    fun authStateFollowsStoredConnection() = runTest {
        repository.authState.test {
            assertEquals(AuthState.LoggedOut, awaitItem())

            repository.saveLogin(connection)
            assertEquals(AuthState.LoggedIn(connection), awaitItem())

            repository.logout()
            assertEquals(AuthState.LoggedOut, awaitItem())
        }
    }

    @Test
    fun loginAndLogoutForgetTheLocalDataOfTheServer() = runTest {
        repository.saveLogin(connection)
        assertEquals(1, localDataClears)

        repository.logout()

        assertEquals(2, localDataClears)
    }

    @Test
    fun verifyChecksServerFirstAndThenTheApiKey() = runTest {
        repository.verify(connection)

        assertEquals(
            listOf(FakeGrocyServer.SYSTEM_INFO_PATH, FakeGrocyServer.USER_PATH),
            server.requestedPaths,
        )
    }

    @Test
    fun verifyDoesNotLogIn() = runTest {
        repository.verify(connection)

        assertNull(credentialStore.connection.value)
    }

    @Test
    fun verifyStopsWhenAddressIsNoGrocyServer() = runTest {
        server.respond(FakeGrocyServer.SYSTEM_INFO_PATH, body = "<html></html>")

        assertFailsWith<GrocyApiException.NotGrocyServer> { repository.verify(connection) }
        assertEquals(listOf(FakeGrocyServer.SYSTEM_INFO_PATH), server.requestedPaths)
    }

    @Test
    fun verifyFailsForRejectedApiKey() = runTest {
        server.respond(FakeGrocyServer.USER_PATH, HttpStatusCode.Unauthorized)

        assertFailsWith<GrocyApiException.Unauthorized> { repository.verify(connection) }
    }

    @Test
    fun verifyFailsForUnreachableServer() = runTest {
        server.unreachable = true

        assertFailsWith<GrocyApiException.Unreachable> { repository.verify(connection) }
    }
}
