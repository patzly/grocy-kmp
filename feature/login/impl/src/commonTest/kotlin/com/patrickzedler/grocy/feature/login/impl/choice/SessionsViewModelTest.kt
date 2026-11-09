package com.patrickzedler.grocy.feature.login.impl.choice

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.core.ui.sessions.SessionError
import com.patrickzedler.grocy.core.ui.sessions.SessionsViewModel
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class SessionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val current = ServerConnection.ApiKey("https://a.example.com", "a")
    private val other = SavedSession(ServerConnection.ApiKey("https://b.example.com", "b"))
    private val store = FakeCredentialStore(current).apply {
        sessions.value = listOf(other)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SessionsViewModel(AuthRepository(store, server.api))

    @Test
    fun theCurrentSessionLeadsTheList() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(
            listOf(current, other.connection),
            viewModel.sessions.value.map { it.connection },
        )
    }

    @Test
    fun openingASessionLogsInAndTellsWhenItIsDone() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        var switched = false

        viewModel.switchTo(other) { switched = true }
        assertEquals(other, viewModel.switching)
        advanceUntilIdle()

        assertEquals(other.connection, store.connection.value)
        assertEquals(other.connection, viewModel.sessions.value.first().connection)
        assertTrue(switched)
        assertNull(viewModel.switching)
    }

    @Test
    fun aRejectedSessionSaysSoAndChangesNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        server.respond(FakeGrocyServer.USER_PATH, HttpStatusCode.Unauthorized)
        var switched = false

        viewModel.switchTo(other) { switched = true }
        advanceUntilIdle()

        assertEquals(current, store.connection.value)
        assertEquals(SessionError.REJECTED, viewModel.error)
        assertFalse(switched)
    }

    @Test
    fun removingForgetsTheSession() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.remove(other)
        advanceUntilIdle()

        assertEquals(listOf(current), viewModel.sessions.value.map { it.connection })
    }

    @Test
    fun aLogoutKeepsTheListForTheLogin() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.logout()
        advanceUntilIdle()

        assertNull(store.connection.value)
        assertEquals(2, viewModel.sessions.value.size)
    }
}
