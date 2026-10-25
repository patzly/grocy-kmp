package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.data.server.GROCY_GITHUB_REPOSITORY
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.network.IngressSession
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
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
 *
 * [grocyReleases] reads Grocy's GitHub releases from the same fake, whose newest one is the
 * server's version until a test answers [GROCY_RELEASE_PATH] differently.
 */
class FakeGrocyServer(
    requestDispatcher: CoroutineDispatcher? = null,
) {

    // By "METHOD path" for method specific answers, by path for all methods
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
            val path = request.url.encodedPath.removePrefix(INGRESS_PATH)
            val (status, body) = responses["${request.method.value} $path"]
                ?: responses[path]
                // Like Grocy, writes without a configured answer succeed without content
                ?: (HttpStatusCode.NoContent to "").takeIf {
                    request.method == HttpMethod.Put || request.method == HttpMethod.Delete
                }
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

    private val httpClient = createGrocyHttpClient(MockEngine(engineConfig))

    val api = GrocyApi(
        httpClient = httpClient,
        ingressSessionSource = {
            ingressFailure?.let { throw it }
            IngressSession(ingressPath = INGRESS_PATH, sessionId = "ingress-session")
        },
        // The header is the default on Android and easier to assert
        apiKeyAsQueryParameter = false,
    )

    val grocyReleases = GitHubReleases(httpClient, GROCY_GITHUB_REPOSITORY)

    init {
        respond(SYSTEM_INFO_PATH, body = SYSTEM_INFO_JSON)
        respond(GROCY_RELEASE_PATH, body = grocyReleaseJson("4.7.1"))
        respond(USER_PATH, body = USER_JSON)
        MasterDataJson.defaults.forEach { (path, body) -> respond(path, body = body) }
        respond(
            path = PRODUCTS_PATH,
            body = """{"created_object_id":${MasterDataJson.CREATED_ID}}""",
            method = HttpMethod.Post,
        )
        // Saving a chore lets Grocy assign its next execution, which answers without content
        respond(
            path = "/api/chores/executions/calculate-next-assignments",
            status = HttpStatusCode.NoContent,
            method = HttpMethod.Post,
        )
        respond(
            path = "$PRODUCTS_PATH/${MasterDataJson.CREATED_ID}",
            body = MasterDataJson.CREATED_PRODUCT,
        )
    }

    /** Answers requests to [path], only those with [method] if given. */
    fun respond(
        path: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = "",
        method: HttpMethod? = null,
    ) {
        responses[if (method == null) path else "${method.value} $path"] = status to body
    }

    /** Requests with [method] to [path], e.g. the PUTs of a saved product. */
    fun requestsTo(path: String, method: HttpMethod): List<HttpRequestData> = requests.filter {
        it.method == method && it.url.encodedPath.removePrefix(INGRESS_PATH) == path
    }

    companion object {
        const val SYSTEM_INFO_PATH = "/api/system/info"
        const val USER_PATH = "/api/user"
        const val PRODUCTS_PATH = "/api/objects/products"
        const val INGRESS_PATH = "/api/hassio_ingress/fake-token"
        const val GROCY_RELEASE_PATH = "/repos/$GROCY_GITHUB_REPOSITORY/releases/latest"

        /** Grocy's newest release on GitHub as the REST API describes it. */
        fun grocyReleaseJson(version: String): String =
            """{"tag_name":"v$version","target_commitish":"master",""" +
                """"html_url":"https://github.com/$GROCY_GITHUB_REPOSITORY/releases/""" +
                """tag/v$version"}"""

        const val SYSTEM_INFO_JSON =
            """{"grocy_version":{"Version":"4.7.1","ReleaseDate":"2026-09-04"},""" +
                """"php_version":"8.3.12","sqlite_version":"3.46.1"}"""

        const val USER_JSON =
            """[{"id":1,"username":"admin","first_name":"Demo","last_name":"User",""" +
                """"display_name":"Demo User","picture_file_name":null}]"""
    }
}
