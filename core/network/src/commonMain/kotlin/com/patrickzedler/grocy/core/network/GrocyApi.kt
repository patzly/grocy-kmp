package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.network.dto.SystemInfoDto
import com.patrickzedler.grocy.core.network.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/**
 * The Grocy REST API.
 *
 * [ingressSessionSource] creates Home Assistant ingress sessions, [apiKeyAsQueryParameter]
 * decides where the API key goes; both have platform defaults and are replaced in tests.
 */
class GrocyApi(
    private val httpClient: HttpClient,
    ingressSessionSource: IngressSessionSource = WebSocketIngressSessionSource(httpClient),
    private val apiKeyAsQueryParameter: Boolean = sendApiKeyAsQueryParameter,
) {

    private val ingress = HomeAssistantIngress(ingressSessionSource)

    suspend fun systemInfo(connection: ServerConnection): SystemInfo =
        get<SystemInfoDto>(connection, "system", "info").toModel()

    /** Grocy returns the current user as a list with exactly one entry. */
    suspend fun currentUser(connection: ServerConnection): User =
        get<List<UserDto>>(connection, "user").singleOrNull()?.toModel()
            ?: throw GrocyApiException.NotGrocyServer()

    private suspend inline fun <reified T> get(
        connection: ServerConnection,
        vararg path: String,
    ): T {
        var response = send(connection, path)
        if (response.status == HttpStatusCode.Unauthorized &&
            connection is ServerConnection.HomeAssistant
        ) {
            // Also the answer to an expired ingress session, so one fresh session decides
            ingress.invalidate(connection)
            response = send(connection, path)
        }
        when {
            response.status == HttpStatusCode.Unauthorized -> throw GrocyApiException.Unauthorized()
            !response.status.isSuccess() -> throw GrocyApiException.HttpError(response.status.value)
        }
        if (connection is ServerConnection.HomeAssistant) {
            ingress.markUsed(connection)
        }
        return try {
            response.body<T>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw GrocyApiException.NotGrocyServer(e)
        }
    }

    private suspend fun send(
        connection: ServerConnection,
        path: Array<out String>,
    ): HttpResponse {
        val ingressSession = (connection as? ServerConnection.HomeAssistant)
            ?.let { ingress.session(it) }
        val baseUrl = connection.serverUrl + ingressSession?.ingressPath.orEmpty()
        val apiKey = connection.apiKey
        return request {
            httpClient.get(baseUrl) {
                url {
                    appendPathSegments("api", *path)
                    if (apiKey != null && apiKeyAsQueryParameter) {
                        parameters.append(API_KEY_HEADER, apiKey)
                    }
                }
                if (apiKey != null && !apiKeyAsQueryParameter) {
                    header(API_KEY_HEADER, apiKey)
                }
                if (ingressSession != null) {
                    header(HttpHeaders.Cookie, "$INGRESS_COOKIE=${ingressSession.sessionId}")
                }
            }
        }
    }

    private suspend inline fun request(block: () -> HttpResponse): HttpResponse =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, not Exception: on Wasm a failed fetch arrives as a Kotlin Error
            throw GrocyApiException.Unreachable(e)
        }

    companion object {
        /** Header name, and query parameter name where the header cannot be used. */
        const val API_KEY_HEADER = "GROCY-API-KEY"

        private const val INGRESS_COOKIE = "ingress_session"
    }
}

private val ServerConnection.apiKey: String?
    get() = when (this) {
        is ServerConnection.ApiKey -> apiKey
        is ServerConnection.HomeAssistant -> apiKey
        is ServerConnection.Demo -> null
    }
