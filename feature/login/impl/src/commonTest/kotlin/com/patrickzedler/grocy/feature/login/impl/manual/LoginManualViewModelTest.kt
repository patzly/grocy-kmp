package com.patrickzedler.grocy.feature.login.impl.manual

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.network.ServerUrl
import com.patrickzedler.grocy.core.testing.FakeCredentialStore
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import com.patrickzedler.grocy.feature.login.impl.LoginError
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

class LoginManualViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val credentialStore = FakeCredentialStore()
    private lateinit var viewModel: LoginManualViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = viewModel()
    }

    private fun viewModel(isHomeAssistantSupported: Boolean = true) = LoginManualViewModel(
        authRepository = AuthRepository(credentialStore, server.api),
        isHomeAssistantSupported = isHomeAssistantSupported,
    )

    private fun enterHomeAssistantLogin(viewModel: LoginManualViewModel = this.viewModel) {
        viewModel.selectServerType(ServerType.HOME_ASSISTANT)
        viewModel.homeAssistantUrl.setTextAndPlaceCursorAtEnd("ha.example.com")
        viewModel.homeAssistantToken.setTextAndPlaceCursorAtEnd("ha-token")
        viewModel.apiKey.setTextAndPlaceCursorAtEnd("secret")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun enterGrocyLogin(url: String = "grocy.example.com", apiKey: String = "secret") {
        viewModel.grocyUrl.setTextAndPlaceCursorAtEnd(url)
        viewModel.apiKey.setTextAndPlaceCursorAtEnd(apiKey)
    }

    @Test
    fun cannotConnectWithoutInput() {
        assertFalse(viewModel.canConnect)
    }

    @Test
    fun canConnectWithAddressAndApiKey() {
        enterGrocyLogin()

        assertTrue(viewModel.canConnect)
    }

    @Test
    fun cannotConnectWithInvalidAddressOrBlankKey() {
        enterGrocyLogin(url = "https://")
        assertFalse(viewModel.canConnect)

        enterGrocyLogin(apiKey = "   ")
        assertFalse(viewModel.canConnect)
    }

    @Test
    fun homeAssistantNeedsAddressTokenKeyAndSlug() {
        enterHomeAssistantLogin()
        assertTrue(viewModel.canConnect)

        viewModel.addonSlug.setTextAndPlaceCursorAtEnd(" ")
        assertFalse(viewModel.canConnect)

        viewModel.addonSlug.setTextAndPlaceCursorAtEnd("a0d7b954_grocy")
        viewModel.homeAssistantToken.setTextAndPlaceCursorAtEnd("")
        assertFalse(viewModel.canConnect)
    }

    @Test
    fun homeAssistantUsesGrocyAddonSlugByDefault() {
        assertEquals("a0d7b954_grocy", viewModel.addonSlug.text.toString())
    }

    @Test
    fun homeAssistantLoginStoresConnection() = runTest {
        enterHomeAssistantLogin()

        viewModel.connect()
        advanceUntilIdle()

        assertEquals(
            ServerConnection.HomeAssistant(
                serverUrl = "https://ha.example.com",
                accessToken = "ha-token",
                apiKey = "secret",
            ),
            credentialStore.connection.value,
        )
        assertTrue(
            server.requests.all {
                it.url.encodedPath.startsWith(FakeGrocyServer.INGRESS_PATH)
            },
        )
    }

    @Test
    fun rejectedHomeAssistantTokenShowsError() = runTest {
        server.ingressFailure = GrocyApiException.HomeAssistantTokenRejected()
        enterHomeAssistantLogin()

        viewModel.connect()
        advanceUntilIdle()

        assertEquals(LoginError.HomeAssistantTokenRejected, viewModel.error)
        assertNull(credentialStore.connection.value)
    }

    @Test
    fun homeAssistantCannotConnectWhereIngressIsUnsupported() = runTest {
        val browserViewModel = viewModel(isHomeAssistantSupported = false)
        enterHomeAssistantLogin(browserViewModel)

        browserViewModel.connect()
        advanceUntilIdle()

        assertFalse(browserViewModel.canConnect)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun switchingTypeKeepsApiKey() {
        enterGrocyLogin()

        viewModel.selectServerType(ServerType.HOME_ASSISTANT)
        viewModel.selectServerType(ServerType.GROCY)

        assertEquals("secret", viewModel.apiKey.text.toString())
        assertTrue(viewModel.canConnect)
    }

    @Test
    fun selectedSchemeAppliesToAddressWithoutScheme() {
        enterGrocyLogin(url = "192.168.1.10:9283")

        viewModel.selectScheme(ServerUrl.Scheme.Http)

        assertEquals("http://192.168.1.10:9283", viewModel.normalizedGrocyUrl)
    }

    @Test
    fun connectStoresNormalizedAddressAndTrimmedKey() = runTest {
        enterGrocyLogin(url = " grocy.example.com/api/ ", apiKey = " secret ")

        viewModel.connect()
        assertTrue(viewModel.isLoggingIn)
        assertFalse(viewModel.canConnect)
        advanceUntilIdle()

        assertEquals(
            ServerConnection.ApiKey("https://grocy.example.com", "secret"),
            credentialStore.connection.value,
        )
        assertEquals("secret", server.requests.last().headers["GROCY-API-KEY"])
    }

    @Test
    fun rejectedKeyShowsErrorAndAllowsRetry() = runTest {
        server.respond(FakeGrocyServer.USER_PATH, HttpStatusCode.Unauthorized)
        enterGrocyLogin()

        viewModel.connect()
        advanceUntilIdle()

        assertEquals(LoginError.Unauthorized, viewModel.error)
        assertFalse(viewModel.isLoggingIn)
        assertTrue(viewModel.canConnect)
        assertNull(credentialStore.connection.value)
    }

    @Test
    fun connectWithoutValidInputDoesNothing() = runTest {
        viewModel.connect()
        advanceUntilIdle()

        assertFalse(viewModel.isLoggingIn)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun changingServerTypeOrSchemeClearsError() = runTest {
        server.unreachable = true
        enterGrocyLogin()
        viewModel.connect()
        advanceUntilIdle()
        assertEquals(LoginError.Unreachable, viewModel.error)

        viewModel.selectScheme(ServerUrl.Scheme.Http)
        assertNull(viewModel.error)

        viewModel.connect()
        advanceUntilIdle()
        viewModel.selectServerType(ServerType.HOME_ASSISTANT)
        assertNull(viewModel.error)
    }
}
