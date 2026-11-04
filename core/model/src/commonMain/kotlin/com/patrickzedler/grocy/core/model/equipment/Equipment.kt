package com.patrickzedler.grocy.core.model.equipment

import kotlin.io.encoding.Base64

/** A piece of Grocy's equipment; [description] is HTML, Grocy's "Notes". */
data class Equipment(
    val id: Int,
    val name: String,
    val description: String? = null,
    /** The stored file of its instruction manual, in Grocy's file group `equipmentmanuals`. */
    val instructionManualFileName: String? = null,
    val userfields: Map<String, String?> = emptyMap(),
)

/**
 * The name the user knows of a file userfield's value, which is Grocy's
 * `base64(stored name)_base64(name)`; null when it is none.
 */
fun userfieldFileNameOf(value: String): String? {
    val encoded = value.substringAfter('_', missingDelimiterValue = "")
    if (encoded.isEmpty()) return null
    return try {
        Base64.decode(encoded).decodeToString()
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Whether [bytes] start like a PDF; the viewers show nothing else, whatever a file is called. */
fun isPdf(bytes: ByteArray): Boolean =
    bytes.size >= PDF_MAGIC.size && PDF_MAGIC.indices.all { bytes[it] == PDF_MAGIC[it] }

private val PDF_MAGIC = "%PDF-".encodeToByteArray()
