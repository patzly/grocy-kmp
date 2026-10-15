package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Access to an add-on through Home Assistant ingress. */
data class IngressSession(
    /** Path below the Home Assistant URL, e.g. "/api/hassio_ingress/<token>". */
    val ingressPath: String,
    /** Value of the `ingress_session` cookie. */
    val sessionId: String,
)

/** Creates ingress sessions; the real one talks to the Home Assistant WebSocket API. */
fun interface IngressSessionSource {

    /**
     * @throws GrocyApiException if Home Assistant is unreachable, rejects the token or has no
     *   add-on with the slug of [connection].
     */
    suspend fun createSession(connection: ServerConnection.HomeAssistant): IngressSession
}

/**
 * Reuses ingress sessions across requests. The supervisor ends a session 15 minutes after its
 * last use, so a session unused for [SESSION_REUSE_LIMIT] is replaced before it can expire.
 */
internal class HomeAssistantIngress(
    private val sessionSource: IngressSessionSource,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val mutex = Mutex()
    private val sessions = mutableMapOf<ServerConnection.HomeAssistant, CachedSession>()

    suspend fun session(connection: ServerConnection.HomeAssistant): IngressSession =
        mutex.withLock {
            sessions[connection]
                ?.takeIf { it.lastUse.elapsedNow() < SESSION_REUSE_LIMIT }
                ?.session
                ?: sessionSource.createSession(connection).also {
                    sessions[connection] = CachedSession(it, timeSource.markNow())
                }
        }

    /** Every request through ingress extends the session on the server. */
    suspend fun markUsed(connection: ServerConnection.HomeAssistant) {
        mutex.withLock {
            sessions[connection]?.let {
                sessions[connection] = it.copy(lastUse = timeSource.markNow())
            }
        }
    }

    suspend fun invalidate(connection: ServerConnection.HomeAssistant) {
        mutex.withLock { sessions.remove(connection) }
    }

    private data class CachedSession(
        val session: IngressSession,
        val lastUse: TimeMark,
    )

    companion object {
        val SESSION_REUSE_LIMIT = 10.minutes
    }
}
