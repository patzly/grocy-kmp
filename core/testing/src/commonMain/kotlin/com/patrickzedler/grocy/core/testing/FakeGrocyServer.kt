package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.network.IngressSession
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.io.IOException

/**
 * A Grocy server simulated with Ktor's MockEngine, so tests run the real [GrocyApi] code.
 *
 * Answers like a healthy Grocy instance until a test changes a response with [respond] or makes
 * it [unreachable].
 *
 * Pass the test dispatcher as [requestDispatcher] when requests run inside code under test
 * (e.g. a ViewModel), so they finish within virtual time instead of on a real thread.
 *
 * Home Assistant connections get an ingress session from the fake as well; their requests reach
 * the same responses, below [INGRESS_PATH].
 */
class FakeGrocyServer(
    requestDispatcher: CoroutineDispatcher? = null,
) {

    private val responses = mutableMapOf<String, Pair<HttpStatusCode, String>>()

    /** Every request the server received, in order. */
    val requests = mutableListOf<HttpRequestData>()

    /** Requested paths in order without ingress prefix, e.g. "/api/system/info". */
    val requestedPaths: List<String>
        get() = requests.map { it.url.encodedPath.removePrefix(INGRESS_PATH) }

    /** When true, every request fails like a server that cannot be reached. */
    var unreachable = false

    /** Requests with these URL schemes fail like an unreachable server, e.g. "https". */
    val unreachableSchemes = mutableSetOf<String>()

    /** Thrown when a Home Assistant ingress session is created, e.g. a rejected token. */
    var ingressFailure: GrocyApiException? = null

    private val engineConfig = MockEngineConfig().apply {
        requestDispatcher?.let { dispatcher = it }
        addHandler { request ->
            requests += request
            if (unreachable || request.url.protocol.name in unreachableSchemes) {
                throw IOException("Simulated network failure")
            }
            val (status, body) = responses[request.url.encodedPath.removePrefix(INGRESS_PATH)]
                ?: (HttpStatusCode.NotFound to """{"error_message":"Not found"}""")
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        // One handler answers every request
        reuseHandlers = true
    }

    val api = GrocyApi(
        httpClient = createGrocyHttpClient(MockEngine(engineConfig)),
        ingressSessionSource = {
            ingressFailure?.let { throw it }
            IngressSession(ingressPath = INGRESS_PATH, sessionId = "ingress-session")
        },
        // The header is the default on Android and easier to assert
        apiKeyAsQueryParameter = false,
    )

    init {
        respond(SYSTEM_INFO_PATH, body = SYSTEM_INFO_JSON)
        respond(USER_PATH, body = USER_JSON)
    }

    fun respond(
        path: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "",
    ) {
        responses[path] = status to body
    }

    companion object {
        const val SYSTEM_INFO_PATH = "/api/system/info"
        const val USER_PATH = "/api/user"
        const val INGRESS_PATH = "/api/hassio_ingress/fake-token"

        const val SYSTEM_INFO_JSON =
            """{"grocy_version":{"Version":"4.7.1","ReleaseDate":"2026-09-04"},""" +
                """"php_version":"8.3.12","sqlite_version":"3.46.1"}"""

        const val USER_JSON =
            """[{"id":1,"username":"admin","first_name":"Demo","last_name":"User",""" +
                """"display_name":"Demo User","picture_file_name":null}]"""
    }
}
