package com.patrickzedler.grocy.feature.login.impl.scan

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.scanner.Barcode
import com.patrickzedler.grocy.core.scanner.BarcodeFormat
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

class LoginScanViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val credentialStore = FakeCredentialStore()
    private lateinit var viewModel: LoginScanViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginScanViewModel(AuthRepository(credentialStore, server.api))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun scan(value: String) =
        viewModel.onBarcodeScanned(Barcode(value, BarcodeFormat.QR_CODE))

    @Test
    fun scannedApiKeyLogsIn() = runTest {
        scan("https://grocy.example.com/api|secret")
        assertTrue(viewModel.uiState.value.isLoggingIn)

        advanceUntilIdle()

        assertEquals(
            ServerConnection.ApiKey("https://grocy.example.com", "secret"),
            credentialStore.connection.value,
        )
    }

    @Test
    fun httpCodeTriesHttpsFirst() = runTest {
        scan("http://grocy.example.com/api|secret")
        advanceUntilIdle()

        assertEquals(
            ServerConnection.ApiKey("https://grocy.example.com", "secret"),
            credentialStore.connection.value,
        )
        assertTrue(server.requests.all { it.url.protocol.name == "https" })
    }

    @Test
    fun httpCodeFallsBackToHttpWithoutHttps() = runTest {
        server.unreachableSchemes += "https"

        scan("http://192.168.1.10:9283/api|secret")
        advanceUntilIdle()

        assertEquals(
            ServerConnection.ApiKey("http://192.168.1.10:9283", "secret"),
            credentialStore.connection.value,
        )
    }

    @Test
    fun rejectedKeyIsNotRetriedWithHttp() = runTest {
        server.respond(FakeGrocyServer.USER_PATH, HttpStatusCode.Unauthorized)

        scan("http://grocy.example.com/api|wrong")
        advanceUntilIdle()

        assertEquals(LoginError.Unauthorized, viewModel.uiState.value.error)
        assertTrue(server.requests.none { it.url.protocol.name == "http" })
        assertNull(credentialStore.connection.value)
    }

    @Test
    fun unreachableServerShowsErrorAndAllowsNextScan() = runTest {
        server.unreachable = true

        scan("https://grocy.example.com/api|secret")
        advanceUntilIdle()

        assertEquals(
            LoginScanUiState(isLoggingIn = false, error = LoginError.Unreachable),
            viewModel.uiState.value,
        )
    }

    @Test
    fun calendarCodeShowsHint() {
        scan("https://grocy.example.com/api/calendar/ical?secret=abc")

        assertEquals(LoginError.CalendarCode, viewModel.uiState.value.error)
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun foreignCodeShowsHint() {
        scan("4006381333931")

        assertEquals(LoginError.NotLoginCode, viewModel.uiState.value.error)
    }

    @Test
    fun scansWhileLoggingInAreIgnored() = runTest {
        scan("https://grocy.example.com/api|secret")
        scan("https://other.example.com/api|other")
        advanceUntilIdle()

        assertEquals("https://grocy.example.com", credentialStore.connection.value?.serverUrl)
        assertFalse(server.requests.any { it.url.host == "other.example.com" })
    }

    @Test
    fun validScanClearsPreviousError() = runTest {
        scan("4006381333931")

        scan("https://grocy.example.com/api|secret")

        assertNull(viewModel.uiState.value.error)
    }
}
