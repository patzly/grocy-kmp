package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
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

    /**
     * "#/settings/general" for the top screen, its arguments as query ("#/masterdata/product?
     * productId=12"), so the address shows what the screen shows; arguments at their default
     * value are left out. The first screen keeps the plain address.
     */
    fun hash(backStack: List<NavKey>): String {
        if (backStack.size <= 1) return ""
        val route = json.encodeToJsonElement(routeSerializer, backStack.last()).jsonObject
        val type = route[TYPE_KEY]?.jsonPrimitive?.content ?: return ""
        val query = route
            .filterKeys { it != TYPE_KEY }
            .map { (name, value) ->
                val text = (value as? JsonPrimitive)?.content ?: value.toString()
                "${percentEncoded(name)}=${percentEncoded(text)}"
            }
        return if (query.isEmpty()) "#/$type" else "#/$type?${query.joinToString("&")}"
    }

    private companion object {
        // The class discriminator of kotlinx.serialization, the route's @SerialName
        const val TYPE_KEY = "type"
    }
}

/** [text] with everything but the unreserved characters of RFC 3986 percent-encoded as UTF-8. */
internal fun percentEncoded(text: String): String = buildString {
    text.encodeToByteArray().forEach { byte ->
        val char = (byte.toInt() and 0xFF).toChar()
        if (char.isLetterOrDigit() && char.code < 0x80 || char in "-._~") {
            append(char)
        } else {
            append('%')
            append(HEX_DIGITS[(byte.toInt() shr 4) and 0x0F])
            append(HEX_DIGITS[byte.toInt() and 0x0F])
        }
    }
}

private const val HEX_DIGITS = "0123456789ABCDEF"
