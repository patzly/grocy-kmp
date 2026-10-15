package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Slugs are inserted into a supervisor path, so nothing else may get through
private val ADDON_SLUG_REGEX = Regex("^[a-z0-9_]+$")

// A different shape could point the API key to another path or host ("//other.host")
private val INGRESS_PATH_REGEX = Regex("^/api/hassio_ingress/[A-Za-z0-9_-]+$")

/** Text messages of one WebSocket connection, abstracted so the protocol can be tested. */
internal interface WebSocketMessages {
    suspend fun send(text: String)

    suspend fun receive(): String
}

/**
 * Creates an ingress session the way the Home Assistant frontend does: through the WebSocket
 * API, because the REST proxy does not allow the supervisor's ingress endpoints.
 */
internal class WebSocketIngressSessionSource(
    private val httpClient: HttpClient,
) : IngressSessionSource {

    override suspend fun createSession(
        connection: ServerConnection.HomeAssistant,
    ): IngressSession {
        val session = try {
            httpClient.webSocketSession(webSocketUrl(connection.serverUrl))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw GrocyApiException.Unreachable(e)
        }
        val messages = object : WebSocketMessages {
            override suspend fun send(text: String) = session.send(Frame.Text(text))

            override suspend fun receive(): String {
                while (true) {
                    val frame = session.incoming.receive()
                    if (frame is Frame.Text) return frame.readText()
                }
            }
        }
        return try {
            withTimeout(HANDSHAKE_TIMEOUT) {
                IngressSessionHandshake(messages).run(connection)
            }
        } catch (e: TimeoutCancellationException) {
            throw GrocyApiException.Unreachable(e)
        } finally {
            session.close()
        }
    }

    private companion object {
        val HANDSHAKE_TIMEOUT = 15.seconds
    }
}

/** "https://ha.example.com" -> "wss://ha.example.com/api/websocket" */
internal fun webSocketUrl(serverUrl: String): String =
    serverUrl.replaceFirst(Regex("^http"), "ws") + "/api/websocket"

/** The message exchange that turns an access token into an [IngressSession]. */
internal class IngressSessionHandshake(
    private val messages: WebSocketMessages,
) {
    private var nextId = 1

    suspend fun run(connection: ServerConnection.HomeAssistant): IngressSession {
        if (!ADDON_SLUG_REGEX.matches(connection.addonSlug)) {
            throw GrocyApiException.AddonNotFound()
        }
        authenticate(connection.accessToken)

        val info = try {
            supervisorApi(endpoint = "/addons/${connection.addonSlug}/info", method = "get")
        } catch (e: SupervisorError) {
            throw GrocyApiException.AddonNotFound(e)
        }
        val ingressPath = info?.get("ingress_entry")?.jsonPrimitive?.contentOrNull
            ?.takeIf { INGRESS_PATH_REGEX.matches(it) }
            ?: throw GrocyApiException.AddonNotFound()

        val session = try {
            supervisorApi(endpoint = "/ingress/session", method = "post")
        } catch (e: SupervisorError) {
            throw GrocyApiException.NotHomeAssistant(e)
        }
        val sessionId = session?.get("session")?.jsonPrimitive?.contentOrNull
            ?: throw GrocyApiException.NotHomeAssistant()

        return IngressSession(ingressPath = ingressPath, sessionId = sessionId)
    }

    private suspend fun authenticate(accessToken: String) {
        if (receiveMessage().type != "auth_required") throw GrocyApiException.NotHomeAssistant()
        messages.send(
            buildJsonObject {
                put("type", "auth")
                put("access_token", accessToken)
            }.toString(),
        )
        when (receiveMessage().type) {
            "auth_ok" -> Unit
            "auth_invalid" -> throw GrocyApiException.HomeAssistantTokenRejected()
            else -> throw GrocyApiException.NotHomeAssistant()
        }
    }

    /** Calls a supervisor endpoint through Home Assistant and returns its result object. */
    private suspend fun supervisorApi(endpoint: String, method: String): JsonObject? {
        val id = nextId++
        messages.send(
            buildJsonObject {
                put("id", id)
                put("type", "supervisor/api")
                put("endpoint", endpoint)
                put("method", method)
            }.toString(),
        )
        while (true) {
            val message = receiveMessage()
            if (message.type != "result" || message.json["id"]?.jsonPrimitive?.intOrNull != id) {
                continue
            }
            val success = message.json["success"]?.jsonPrimitive?.contentOrNull == "true"
            if (!success) throw SupervisorError(message.json["error"].toString())
            return message.json["result"] as? JsonObject
        }
    }

    private suspend fun receiveMessage(): Message {
        val json = try {
            GrocyJson.parseToJsonElement(messages.receive()).jsonObject
        } catch (e: IllegalArgumentException) {
            throw GrocyApiException.NotHomeAssistant(e)
        }
        return Message(json)
    }

    private class Message(val json: JsonObject) {
        val type: String? = json["type"]?.jsonPrimitive?.contentOrNull
    }

    private class SupervisorError(message: String) : Exception(message)
}
