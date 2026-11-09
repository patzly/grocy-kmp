package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.serialization.json.Json

/** JSON form of [ServerConnection] for storage, shared by all platforms. */
object ServerConnectionCodec {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun encode(connection: ServerConnection): String =
        json.encodeToString<ServerConnection>(connection)

    /** Returns null for unreadable data, e.g. written by a future app version. */
    fun decode(value: String): ServerConnection? =
        try {
            json.decodeFromString<ServerConnection>(value)
        } catch (e: IllegalArgumentException) {
            null
        }

    fun encodeSessions(sessions: List<SavedSession>): String = json.encodeToString(sessions)

    /** Empty for unreadable data, e.g. written by a future app version. */
    fun decodeSessions(value: String): List<SavedSession> =
        try {
            json.decodeFromString<List<SavedSession>>(value)
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
}
