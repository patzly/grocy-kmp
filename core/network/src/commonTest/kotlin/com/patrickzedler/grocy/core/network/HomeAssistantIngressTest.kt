package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest

class HomeAssistantIngressTest {

    private val timeSource = TestTimeSource()
    private var created = 0
    private val ingress = HomeAssistantIngress(
        sessionSource = {
            created++
            IngressSession("/api/hassio_ingress/token", "session-$created")
        },
        timeSource = timeSource,
    )
    private val connection = ServerConnection.HomeAssistant(
        serverUrl = "https://ha.example.com",
        accessToken = "token",
        apiKey = "key",
    )

    @Test
    fun recentlyUsedSessionIsReused() = runTest {
        ingress.session(connection)
        timeSource += 9.minutes

        assertEquals("session-1", ingress.session(connection).sessionId)
    }

    @Test
    fun sessionUnusedForTooLongIsReplaced() = runTest {
        ingress.session(connection)
        timeSource += HomeAssistantIngress.SESSION_REUSE_LIMIT

        assertEquals("session-2", ingress.session(connection).sessionId)
    }

    @Test
    fun useExtendsTheSession() = runTest {
        ingress.session(connection)
        timeSource += 8.minutes
        ingress.markUsed(connection)
        timeSource += 8.minutes

        assertEquals("session-1", ingress.session(connection).sessionId)
    }

    @Test
    fun invalidatedSessionIsReplaced() = runTest {
        ingress.session(connection)

        ingress.invalidate(connection)

        assertEquals("session-2", ingress.session(connection).sessionId)
    }

    @Test
    fun connectionsHaveSeparateSessions() = runTest {
        ingress.session(connection)

        val other = ingress.session(connection.copy(apiKey = "other"))

        assertEquals("session-2", other.sessionId)
    }
}
