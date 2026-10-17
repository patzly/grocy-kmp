package com.patrickzedler.grocy.core.network.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

// Grocy returns numbers and flags as JSON numbers, as strings ("1", "2.5") or as booleans,
// depending on version and database driver; empty strings mean null.

internal typealias FlexInt = @Serializable(FlexibleIntSerializer::class) Int?
internal typealias FlexDouble = @Serializable(FlexibleDoubleSerializer::class) Double?
internal typealias FlexBoolean = @Serializable(FlexibleBooleanSerializer::class) Boolean?
internal typealias FlexString = @Serializable(FlexibleStringSerializer::class) String?

/** The primitive behind a value, null for JSON null and empty strings. */
private fun Decoder.primitiveOrNull(): JsonPrimitive? {
    val element = (this as JsonDecoder).decodeJsonElement()
    if (element is JsonNull || element !is JsonPrimitive) return null
    return element.takeUnless { it.isString && it.content.isBlank() }
}

internal object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val primitive = decoder.primitiveOrNull() ?: return null
        primitive.booleanOrNull?.let { return if (it) 1 else 0 }
        return primitive.content.toIntOrNull() ?: primitive.content.toDoubleOrNull()?.toInt()
    }

    override fun serialize(encoder: Encoder, value: Int?) =
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
}

internal object FlexibleDoubleSerializer : KSerializer<Double?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? =
        decoder.primitiveOrNull()?.content?.toDoubleOrNull()

    override fun serialize(encoder: Encoder, value: Double?) =
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
}

internal object FlexibleBooleanSerializer : KSerializer<Boolean?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean? {
        val primitive = decoder.primitiveOrNull() ?: return null
        primitive.booleanOrNull?.let { return it }
        return when (primitive.content.lowercase()) {
            "1", "true", "on" -> true
            "0", "false", "off" -> false
            else -> primitive.content.toDoubleOrNull()?.let { it != 0.0 }
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean?) =
        if (value == null) encoder.encodeNull() else encoder.encodeBoolean(value)
}

/** Strings, also when Grocy sends a number where text is expected (e.g. a userfield value). */
internal object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return (element as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content
    }

    override fun serialize(encoder: Encoder, value: String?) =
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
}
