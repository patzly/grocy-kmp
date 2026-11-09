package com.patrickzedler.grocy.core.model

import kotlinx.serialization.Serializable

/**
 * A login kept on the device, so the user can return to it without entering it again. The user is
 * null until it was asked for once, e.g. for a login of a version without sessions.
 */
@Serializable
data class SavedSession(
    val connection: ServerConnection,
    val userId: Int? = null,
    val username: String? = null,
    val displayName: String? = null,
) {
    /** The address without its scheme, what a user calls the instance. */
    val host: String
        get() = connection.serverUrl.substringAfter("://")

    /** Who is logged in, the instance as long as that is unknown. */
    val name: String
        get() = displayName?.ifBlank { null } ?: username?.ifBlank { null } ?: host

    /** The letter of a profile circle: the username's, as far as it is known. */
    val initial: String
        get() = (username?.ifBlank { null } ?: name).firstCharacter().uppercase()

    /**
     * Whether both are the same account: every demo is one, an account of a server is told by its
     * user, and as long as that is unknown by its credentials.
     */
    fun isSameAs(other: SavedSession): Boolean {
        val first = connection
        val second = other.connection
        return when {
            first is ServerConnection.Demo && second is ServerConnection.Demo -> true
            first::class != second::class || first.serverUrl != second.serverUrl -> false
            userId != null && other.userId != null -> userId == other.userId
            else -> first == second
        }
    }
}

// One whole character, an emoji or other surrogate pair is two chars
private fun String.firstCharacter(): String =
    if (length > 1 && first().isHighSurrogate()) take(2) else take(1)

/** [saved] without repeated accounts; of those the first, the most recently used, stays. */
private fun distinctSessions(saved: List<SavedSession>): List<SavedSession> =
    saved.fold(emptyList()) { kept, session ->
        if (kept.any { it.isSameAs(session) }) kept else kept + session
    }

/**
 * Every session the device keeps, most recently used first; while logged in ([current]) that one
 * leads, also if it was stored before the app kept sessions.
 */
fun sessionsOf(current: ServerConnection?, saved: List<SavedSession>): List<SavedSession> {
    val sessions = distinctSessions(saved)
    if (current == null) return sessions
    val session = sessions.firstOrNull { it.connection == current } ?: SavedSession(current)
    return listOf(session) + sessions.filterNot { it.isSameAs(session) }
}

/** [saved] after logging in with [connection] as [user], which replaces the same account. */
fun sessionsAfterLogin(
    saved: List<SavedSession>,
    connection: ServerConnection,
    user: User?,
): List<SavedSession> {
    val known = saved.firstOrNull { it.connection == connection }
    val session = user?.let {
        SavedSession(connection, it.id, it.username, it.displayName)
    } ?: known ?: SavedSession(connection)
    return listOf(session) + distinctSessions(saved).filterNot { it.isSameAs(session) }
}

/**
 * The session a swipe goes to from the current one ([sessions] starts with it), null without
 * another. The order must not follow the last use: a swipe would only ever return to the session
 * before, and a third one could never be reached.
 */
fun nextSession(sessions: List<SavedSession>): SavedSession? {
    if (sessions.size < 2) return null
    val cycle = sessions.sortedBy { "${it.connection.serverUrl}\n${it.userId}\n${it.username}" }
    return cycle[(cycle.indexOf(sessions.first()) + 1) % cycle.size]
}
