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
import kotlinx.coroutines.flow.first
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

    @Test
    fun aLogoutKeepsTheSessionWithItsCredentials() = runTest {
        repository.saveLogin(connection)

        repository.logout()

        assertEquals(listOf(connection), repository.sessions.first().map { it.connection })
    }

    @Test
    fun aLogoutKeepsALoginOlderThanTheSessions() = runTest {
        credentialStore.save(connection)

        repository.logout()

        assertEquals(listOf(connection), credentialStore.sessions.value.map { it.connection })
    }

    @Test
    fun theFormerSessionComesRightAfterTheCurrentOne() = runTest {
        val other = ServerConnection.Demo()
        repository.saveLogin(connection)

        repository.saveLogin(other)

        assertEquals(listOf(other, connection), repository.sessions.first().map { it.connection })
    }

    @Test
    fun switchingOpensTheSessionAndChecksItFirst() = runTest {
        repository.saveLogin(connection)
        val session = repository.sessions.first().single()
        repository.saveLogin(ServerConnection.Demo())

        repository.switchTo(session)

        assertEquals(connection, credentialStore.connection.value)
        assertEquals(FakeGrocyServer.USER_PATH, server.requestedPaths.last())
    }

    @Test
    fun aSessionThatFailsLeavesTheCurrentOne() = runTest {
        repository.saveLogin(connection)
        val session = repository.sessions.first().single()
        val demo = ServerConnection.Demo()
        repository.saveLogin(demo)
        server.respond(FakeGrocyServer.USER_PATH, HttpStatusCode.Unauthorized)

        assertFailsWith<GrocyApiException.Unauthorized> { repository.switchTo(session) }

        assertEquals(demo, credentialStore.connection.value)
    }

    @Test
    fun removingAnotherSessionForgetsOnlyThatOne() = runTest {
        val demo = ServerConnection.Demo()
        repository.saveLogin(connection)
        repository.saveLogin(demo)
        val other = repository.sessions.first().last()

        repository.removeSession(other)

        assertEquals(listOf(demo), repository.sessions.first().map { it.connection })
        assertEquals(demo, credentialStore.connection.value)
    }

    @Test
    fun removingTheCurrentSessionLogsOutAndForgetsIt() = runTest {
        repository.saveLogin(connection)
        val current = repository.sessions.first().single()

        repository.removeSession(current)

        assertNull(credentialStore.connection.value)
        assertEquals(emptyList(), repository.sessions.first())
    }

    @Test
    fun aRepositoryWithoutSessionsKeepsNoCredentialsAfterALogout() = runTest {
        val watch = AuthRepository(credentialStore, server.api, keepsSessions = false)
        watch.saveLogin(connection)

        watch.logout()

        assertEquals(emptyList(), credentialStore.sessions.value)
    }

    @Test
    fun refreshingTheUserFillsInTheSession() = runTest {
        credentialStore.save(connection)

        repository.refreshCurrentUser()

        assertEquals("admin", repository.sessions.first().single().username)
    }
}
