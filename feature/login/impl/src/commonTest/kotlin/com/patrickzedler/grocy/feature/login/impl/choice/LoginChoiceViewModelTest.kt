package com.patrickzedler.grocy.feature.login.impl.choice

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.feature.login.impl.LoginError
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class LoginChoiceViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val credentialStore = FakeCredentialStore()
    private lateinit var viewModel: LoginChoiceViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginChoiceViewModel(AuthRepository(credentialStore, server.api))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun demoLoginStoresDemoConnection() = runTest {
        viewModel.loginWithDemoServer()
        assertTrue(viewModel.uiState.value.isLoggingIn)

        advanceUntilIdle()

        assertEquals(ServerConnection.Demo(), credentialStore.connection.value)
        // Stays in loading state while the app switches to the start screen
        assertTrue(viewModel.uiState.value.isLoggingIn)
    }

    @Test
    fun failedDemoLoginShowsError() = runTest {
        server.unreachable = true

        viewModel.loginWithDemoServer()
        advanceUntilIdle()

        assertEquals(
            LoginChoiceUiState(isLoggingIn = false, error = LoginError.Unreachable),
            viewModel.uiState.value,
        )
        assertNull(credentialStore.connection.value)
    }

    @Test
    fun retryClearsPreviousError() = runTest {
        server.unreachable = true
        viewModel.loginWithDemoServer()
        advanceUntilIdle()

        server.unreachable = false
        viewModel.loginWithDemoServer()

        assertNull(viewModel.uiState.value.error)
        advanceUntilIdle()
        assertEquals(ServerConnection.Demo(), credentialStore.connection.value)
    }

    @Test
    fun secondClickWhileLoggingInIsIgnored() = runTest {
        viewModel.loginWithDemoServer()
        viewModel.loginWithDemoServer()
        advanceUntilIdle()

        assertEquals(
            listOf(FakeGrocyServer.SYSTEM_INFO_PATH, FakeGrocyServer.USER_PATH),
            server.requestedPaths,
        )
        assertNull(viewModel.uiState.value.error)
    }
}
