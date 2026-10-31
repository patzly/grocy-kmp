package com.patrickzedler.grocy.core.forms

import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.masterdata.UserfieldType
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Why a userfield value is invalid, like Grocy's validation of its inputs. */
enum class UserfieldError { REQUIRED, INVALID_NUMBER, INVALID_DATE, INVALID_DATETIME }

/** Checks a userfield value as Grocy stores it; null when it is fine. */
fun validateUserfield(field: Userfield, value: String?): UserfieldError? {
    if (value.isNullOrBlank()) {
        return if (field.inputRequired && field.type.isEditable) UserfieldError.REQUIRED else null
    }
    return when (field.type) {
        UserfieldType.NUMBER_INTEGRAL ->
            UserfieldError.INVALID_NUMBER.takeUnless { INTEGER.matches(value) }
        UserfieldType.NUMBER_DECIMAL, UserfieldType.NUMBER_CURRENCY ->
            UserfieldError.INVALID_NUMBER.takeIf { parseDecimal(value) == null }
        UserfieldType.DATE -> UserfieldError.INVALID_DATE.takeUnless { DATE.matches(value) }
        UserfieldType.DATETIME ->
            UserfieldError.INVALID_DATETIME.takeUnless { DATETIME.matches(value) }
        else -> null
    }
}

/** Files and images are uploaded in Grocy's web frontend, the app shows them read only. */
val UserfieldType.isEditable: Boolean
    get() = this != UserfieldType.FILE && this != UserfieldType.IMAGE

/** Numbers are stored with "." whatever the user typed. */
fun normalizeUserfieldNumber(type: UserfieldType, text: String): String {
    val value = parseDecimal(text) ?: return text.trim()
    return when (type) {
        UserfieldType.NUMBER_INTEGRAL -> value.toLong().toString()
        else -> formatDecimal(value, maxDecimals = MAX_DECIMALS)
    }
}

/** Title and link of a "link-with-title" value, stored as JSON object by Grocy. */
fun decodeLinkWithTitle(value: String?): Pair<String, String> {
    if (value.isNullOrBlank()) return "" to ""
    return try {
        val json = Json.parseToJsonElement(value).jsonObject
        val title = json["title"]?.jsonPrimitive?.content.orEmpty()
        title to json["link"]?.jsonPrimitive?.content.orEmpty()
    } catch (_: SerializationException) {
        "" to value
    } catch (_: IllegalArgumentException) {
        "" to value
    }
}

/** Title and link as the JSON object Grocy stores, empty when both are blank. */
fun encodeLinkWithTitle(title: String, link: String): String =
    if (title.isBlank() && link.isBlank()) {
        ""
    } else {
        JsonObject(mapOf("title" to JsonPrimitive(title), "link" to JsonPrimitive(link)))
            .toString()
    }

/** Chosen options of a "preset-checklist", comma separated in Grocy. */
fun decodeChecklist(value: String?): Set<String> =
    value.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/** In the order of the field's options, like Grocy's multi select. */
fun encodeChecklist(selected: Set<String>, options: List<String>): String =
    options.filter { it in selected }.joinToString(",")

/** The value a new object starts with; Grocy's "now" default is left to the user. */
fun defaultUserfieldValue(field: Userfield): String? =
    field.defaultValue?.takeUnless { it.equals("now", ignoreCase = true) }

/** Values Grocy treats as the same: nothing and an empty text. */
fun sameUserfieldValue(a: String?, b: String?): Boolean = a.orEmpty() == b.orEmpty()

private const val MAX_DECIMALS = 10
private val INTEGER = Regex("""-?\d+""")
private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
private val DATETIME = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}(:\d{2})?""")
