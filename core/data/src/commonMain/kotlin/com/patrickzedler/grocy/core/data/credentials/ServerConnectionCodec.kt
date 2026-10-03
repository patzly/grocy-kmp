package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.serialization.json.Json

/** JSON form of [ServerConnection] for storage, shared by all platforms. */
internal object ServerConnectionCodec {

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
}
