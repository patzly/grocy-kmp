package com.patrickzedler.grocy.core.ui.sessions

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val url = "https://grocy.example.com"
    private val anna = SavedSession(ServerConnection.ApiKey(url, "a"), 1, "anna", "Anna")
    private val ben = SavedSession(ServerConnection.ApiKey(url, "b"), 2, "ben", "Ben")
    private val carl = SavedSession(ServerConnection.ApiKey(url, "c"), 3, "carl", "Carl")
    private val credentialStore = FakeCredentialStore(anna.connection).apply {
        sessions.value = listOf(anna, ben, carl)
    }
    private var localDataClears = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): SessionsViewModel {
        val server = FakeGrocyServer(requestDispatcher = dispatcher)
        return SessionsViewModel(
            AuthRepository(credentialStore, server.api) { localDataClears++ },
        )
    }

    @Test
    fun anotherChoiceWhileOneLoadsOpensTheLastOne() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val switched = mutableListOf<String>()

        viewModel.switchTo(ben) { switched += "ben" }
        viewModel.switchTo(carl) { switched += "carl" }
        advanceUntilIdle()

        assertEquals(carl.connection, credentialStore.connection.value)
        assertEquals(listOf("carl"), switched)
        assertNull(viewModel.switching)
    }

    @Test
    fun choosingTheOpenSessionAgainWhileAnotherLoadsKeepsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        var switched = 0

        viewModel.switchTo(ben) { switched++ }
        viewModel.switchTo(anna) { switched++ }
        advanceUntilIdle()

        assertEquals(anna.connection, credentialStore.connection.value)
        assertEquals(0, localDataClears)
        assertEquals(0, switched)
        assertNull(viewModel.switching)
    }

    @Test
    fun theSessionBeingOpenedIsShownUntilItIsThere() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.switchTo(ben)
        viewModel.switchTo(carl)

        assertEquals(carl, viewModel.switching)
    }
}
