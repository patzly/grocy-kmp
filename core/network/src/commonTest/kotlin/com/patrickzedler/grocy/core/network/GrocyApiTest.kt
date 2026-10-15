package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.User
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class GrocyApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private var ingressSessionsCreated = 0

    private fun api(
        apiKeyAsQueryParameter: Boolean = false,
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): GrocyApi {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        return GrocyApi(
            httpClient = createGrocyHttpClient(engine),
            ingressSessionSource = {
                ingressSessionsCreated++
                IngressSession("/api/hassio_ingress/token", "session-$ingressSessionsCreated")
            },
            apiKeyAsQueryParameter = apiKeyAsQueryParameter,
        )
    }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    @Test
    fun systemInfoMapsGrocyVersion() = runTest {
        val api = api { json(SYSTEM_INFO_JSON) }

        val info = api.systemInfo(DEMO)

        assertEquals(SystemInfo(grocyVersion = "4.7.1", grocyReleaseDate = "2026-09-04"), info)
    }

    @Test
    fun requestsGoToApiPathBelowServerUrl() = runTest {
        val api = api { json(SYSTEM_INFO_JSON) }

        api.systemInfo(ServerConnection.ApiKey("https://example.com/grocy", "key"))

        assertEquals("https://example.com/grocy/api/system/info", requests.single().url.toString())
    }

    @Test
    fun apiKeyConnectionSendsApiKeyHeader() = runTest {
        val api = api { json(SYSTEM_INFO_JSON) }

        api.systemInfo(ServerConnection.ApiKey("https://grocy.example.com", "secret-key"))

        assertEquals("secret-key", requests.single().headers[GrocyApi.API_KEY_HEADER])
    }

    @Test
    fun apiKeyCanBeSentAsQueryParameter() = runTest {
        val api = api(apiKeyAsQueryParameter = true) { json(SYSTEM_INFO_JSON) }

        api.systemInfo(ServerConnection.ApiKey("https://grocy.example.com", "secret-key"))

        val request = requests.single()
        assertEquals("secret-key", request.url.parameters[GrocyApi.API_KEY_HEADER])
        assertNull(request.headers[GrocyApi.API_KEY_HEADER])
    }

    @Test
    fun homeAssistantRequestsGoThroughIngressWithSessionCookie() = runTest {
        val api = api { json(SYSTEM_INFO_JSON) }

        api.systemInfo(HOME_ASSISTANT)

        val request = requests.single()
        assertEquals(
            "https://ha.example.com/api/hassio_ingress/token/api/system/info",
            request.url.toString(),
        )
        assertEquals("ingress_session=session-1", request.headers[HttpHeaders.Cookie])
        assertEquals("grocy-key", request.headers[GrocyApi.API_KEY_HEADER])
    }

    @Test
    fun homeAssistantSessionIsReused() = runTest {
        val api = api { json(SYSTEM_INFO_JSON) }

        api.systemInfo(HOME_ASSISTANT)
        api.systemInfo(HOME_ASSISTANT)

        assertEquals(1, ingressSessionsCreated)
    }

    @Test
    fun expiredIngressSessionIsReplacedOnce() = runTest {
        val api = api { request ->
            if (request.headers[HttpHeaders.Cookie] == "ingress_session=session-1") {
                json("", HttpStatusCode.Unauthorized)
            } else {
                json(SYSTEM_INFO_JSON)
            }
        }

        api.systemInfo(HOME_ASSISTANT)

        assertEquals(2, ingressSessionsCreated)
        assertEquals(2, requests.size)
    }

    @Test
    fun rejectionWithFreshIngressSessionIsUnauthorized() = runTest {
        val api = api { json("", HttpStatusCode.Unauthorized) }

        assertFailsWith<GrocyApiException.Unauthorized> { api.systemInfo(HOME_ASSISTANT) }
        assertEquals(2, ingressSessionsCreated)
    }

    @Test
    fun demoConnectionSendsNoApiKeyHeader() = runTest {
        val api = api { json(SYSTEM_INFO_JSON) }

        api.systemInfo(DEMO)

        assertNull(requests.single().headers[GrocyApi.API_KEY_HEADER])
    }

    @Test
    fun currentUserMapsTheOnlyListEntry() = runTest {
        val api = api { json(USER_JSON) }

        val user = api.currentUser(DEMO)

        assertEquals(User(id = 1, username = "admin", displayName = "Demo User"), user)
        assertEquals("/api/user", requests.single().url.encodedPath)
    }

    @Test
    fun currentUserWithoutEntryIsNoGrocyServer() = runTest {
        val api = api { json("[]") }

        assertFailsWith<GrocyApiException.NotGrocyServer> { api.currentUser(DEMO) }
    }

    @Test
    fun unauthorizedStatusIsUnauthorized() = runTest {
        val api = api { json("""{"error_message":"Unauthorized"}""", HttpStatusCode.Unauthorized) }

        assertFailsWith<GrocyApiException.Unauthorized> { api.currentUser(DEMO) }
    }

    @Test
    fun otherErrorStatusIsHttpError() = runTest {
        val api = api { json("{}", HttpStatusCode.InternalServerError) }

        val error = assertFailsWith<GrocyApiException.HttpError> { api.systemInfo(DEMO) }
        assertEquals(500, error.statusCode)
    }

    @Test
    fun htmlResponseIsNoGrocyServer() = runTest {
        val api = api {
            respond(
                content = "<!DOCTYPE html><html></html>",
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        assertFailsWith<GrocyApiException.NotGrocyServer> { api.systemInfo(DEMO) }
    }

    @Test
    fun unexpectedJsonIsNoGrocyServer() = runTest {
        val api = api { json("""{"status":"ok"}""") }

        assertFailsWith<GrocyApiException.NotGrocyServer> { api.systemInfo(DEMO) }
    }

    @Test
    fun networkFailureIsUnreachable() = runTest {
        val cause = IOException("Connection refused")
        val api = api { throw cause }

        val error = assertFailsWith<GrocyApiException.Unreachable> { api.systemInfo(DEMO) }
        assertTrue(error.cause is IOException)
    }

    @Test
    fun cancellationIsNotTurnedIntoAnApiError() = runTest {
        val api = api { throw CancellationException("cancelled") }

        assertFailsWith<CancellationException> { api.systemInfo(DEMO) }
    }

    private companion object {
        val DEMO = ServerConnection.Demo()
        val HOME_ASSISTANT = ServerConnection.HomeAssistant(
            serverUrl = "https://ha.example.com",
            accessToken = "ha-token",
            apiKey = "grocy-key",
        )

        // Shapes as documented in grocy.openapi.json, with fields the client does not use
        const val SYSTEM_INFO_JSON = """
            {
              "grocy_version": {"Version": "4.7.1", "ReleaseDate": "2026-09-04"},
              "php_version": "8.3.12",
              "sqlite_version": "3.46.1",
              "os": "Linux",
              "client": "Grocy KMP"
            }
        """

        const val USER_JSON = """
            [{
              "id": 1,
              "username": "admin",
              "first_name": "Demo",
              "last_name": "User",
              "display_name": "Demo User",
              "picture_file_name": null,
              "row_created_timestamp": "2026-09-04 12:00:00"
            }]
        """
    }
}
