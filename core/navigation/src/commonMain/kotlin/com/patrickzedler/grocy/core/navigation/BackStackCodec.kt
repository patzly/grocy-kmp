package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule

/**
 * The back stack as text for the browser history, so a reload restores it, and the URL hash of
 * its screen.
 */
internal class BackStackCodec(routeSerializers: SerializersModule) {

    private val json = Json { serializersModule = routeSerializers }
    private val routeSerializer = PolymorphicSerializer(NavKey::class)
    private val stackSerializer = ListSerializer(routeSerializer)

    fun encode(backStack: List<NavKey>): String = json.encodeToString(stackSerializer, backStack)

    /** Null for text that is no back stack of this app version. */
    fun decode(text: String): List<NavKey>? = try {
        json.decodeFromString(stackSerializer, text).ifEmpty { null }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /** "#/settings/general" for the top screen; the first screen keeps the plain address. */
    fun hash(backStack: List<NavKey>): String {
        if (backStack.size <= 1) return ""
        val type = json.encodeToJsonElement(routeSerializer, backStack.last())
            .jsonObject[TYPE_KEY]?.jsonPrimitive?.content
        return if (type == null) "" else "#/$type"
    }

    private companion object {
        // The class discriminator of kotlinx.serialization, the route's @SerialName
        const val TYPE_KEY = "type"
    }
}
