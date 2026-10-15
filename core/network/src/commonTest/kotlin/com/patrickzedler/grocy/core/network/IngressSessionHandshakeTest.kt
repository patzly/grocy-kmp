package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class IngressSessionHandshakeTest {

    /** Answers like Home Assistant, messages are taken in order. */
    private class ScriptedMessages(vararg incoming: String) : WebSocketMessages {
        private val incoming = ArrayDeque(incoming.toList())
        val sent = mutableListOf<String>()

        override suspend fun send(text: String) {
            sent += text
        }

        override suspend fun receive(): String =
            incoming.removeFirstOrNull() ?: error("Home Assistant would wait for a message")
    }

    private val connection = ServerConnection.HomeAssistant(
        serverUrl = "https://ha.example.com",
        accessToken = "ha-token",
        apiKey = "grocy-key",
    )

    private fun ScriptedMessages.sentJson(index: Int) =
        Json.parseToJsonElement(sent[index]).jsonObject

    @Test
    fun createsSessionForAddonIngressPath() = runTest {
        val messages = ScriptedMessages(
            AUTH_REQUIRED,
            AUTH_OK,
            """{"id":1,"type":"result","success":true,"result":{"ingress":true,""" +
                """"ingress_entry":"/api/hassio_ingress/abc-123_X"}}""",
            """{"id":2,"type":"result","success":true,"result":{"session":"s3ssion"}}""",
        )

        val session = IngressSessionHandshake(messages).run(connection)

        assertEquals(IngressSession("/api/hassio_ingress/abc-123_X", "s3ssion"), session)
        assertEquals("ha-token", messages.sentJson(0)["access_token"]?.jsonPrimitive?.content)
        assertEquals(
            "/addons/a0d7b954_grocy/info",
            messages.sentJson(1)["endpoint"]?.jsonPrimitive?.content,
        )
        assertEquals("/ingress/session", messages.sentJson(2)["endpoint"]?.jsonPrimitive?.content)
        assertEquals("post", messages.sentJson(2)["method"]?.jsonPrimitive?.content)
    }

    @Test
    fun skipsMessagesOfOtherRequests() = runTest {
        val messages = ScriptedMessages(
            AUTH_REQUIRED,
            AUTH_OK,
            """{"id":7,"type":"event","event":{}}""",
            """{"id":1,"type":"result","success":true,""" +
                """"result":{"ingress_entry":"/api/hassio_ingress/token"}}""",
            """{"id":2,"type":"result","success":true,"result":{"session":"s"}}""",
        )

        assertEquals("s", IngressSessionHandshake(messages).run(connection).sessionId)
    }

    @Test
    fun rejectedTokenIsReported() = runTest {
        val messages = ScriptedMessages(
            AUTH_REQUIRED,
            """{"type":"auth_invalid","message":"Invalid access token or password"}""",
        )

        assertFailsWith<GrocyApiException.HomeAssistantTokenRejected> {
            IngressSessionHandshake(messages).run(connection)
        }
    }

    @Test
    fun unknownAddonIsReported() = runTest {
        val messages = ScriptedMessages(
            AUTH_REQUIRED,
            AUTH_OK,
            """{"id":1,"type":"result","success":false,""" +
                """"error":{"code":"unknown_error","message":"Addon does not exist"}}""",
        )

        assertFailsWith<GrocyApiException.AddonNotFound> {
            IngressSessionHandshake(messages).run(connection)
        }
    }

    @Test
    fun addonWithoutIngressIsReported() = runTest {
        val messages = ScriptedMessages(
            AUTH_REQUIRED,
            AUTH_OK,
            """{"id":1,"type":"result","success":true,"result":{"ingress":false}}""",
        )

        assertFailsWith<GrocyApiException.AddonNotFound> {
            IngressSessionHandshake(messages).run(connection)
        }
    }

    @Test
    fun ingressPathThatCouldLeaveHomeAssistantIsRejected() = runTest {
        listOf("//evil.example.com/x", "/api/hassio_ingress/../../evil", "https://evil").forEach {
            val messages = ScriptedMessages(
                AUTH_REQUIRED,
                AUTH_OK,
                """{"id":1,"type":"result","success":true,"result":{"ingress_entry":"$it"}}""",
            )

            assertFailsWith<GrocyApiException.AddonNotFound> {
                IngressSessionHandshake(messages).run(connection)
            }
        }
    }

    @Test
    fun invalidSlugIsRejectedBeforeAnythingIsSent() = runTest {
        val messages = ScriptedMessages()

        assertFailsWith<GrocyApiException.AddonNotFound> {
            IngressSessionHandshake(messages).run(connection.copy(addonSlug = "../core/info"))
        }
        assertTrue(messages.sent.isEmpty())
    }

    @Test
    fun otherServerIsNoHomeAssistant() = runTest {
        assertFailsWith<GrocyApiException.NotHomeAssistant> {
            IngressSessionHandshake(ScriptedMessages("<html></html>")).run(connection)
        }
        assertFailsWith<GrocyApiException.NotHomeAssistant> {
            IngressSessionHandshake(ScriptedMessages("""{"type":"hello"}""")).run(connection)
        }
    }

    @Test
    fun webSocketUrlFollowsScheme() {
        assertEquals("wss://ha.example.com/api/websocket", webSocketUrl("https://ha.example.com"))
        assertEquals("ws://192.168.1.2:8123/api/websocket", webSocketUrl("http://192.168.1.2:8123"))
        assertContains(webSocketUrl("https://example.com/ha"), "/ha/api/websocket")
    }

    private companion object {
        const val AUTH_REQUIRED = """{"type":"auth_required","ha_version":"2026.10.1"}"""
        const val AUTH_OK = """{"type":"auth_ok","ha_version":"2026.10.1"}"""
    }
}
