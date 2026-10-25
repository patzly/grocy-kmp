package com.patrickzedler.grocy.core.model.masterdata

import kotlin.math.abs

/**
 * Number of plural forms Grocy's translation of [language] (a tag like "de" or "pt-BR") has.
 * Grocy asks for a unit's plural forms only in languages with more than two.
 */
fun grocyPluralFormCount(language: String): Int {
    val base = language.substringBefore('-').substringBefore('_').lowercase()
    return PLURAL_FORMS[base] ?: 2
}

// From the "Plural-Forms" headers of Grocy's translations
private val PLURAL_FORMS = mapOf(
    "cs" to 4, "es" to 3, "fr" to 3, "he" to 3, "iw" to 3, "it" to 3, "ja" to 1, "ko" to 1,
    "lt" to 4, "pl" to 4, "pt" to 3, "ro" to 3, "ru" to 4, "sk" to 4, "sl" to 4, "uk" to 4,
    "zh" to 1,
)

/**
 * Which plural form of [language] Grocy uses for [amount], by the "Plural-Forms" rule of its
 * translation; 0 is the singular.
 */
fun grocyPluralIndex(language: String, amount: Double): Int {
    val n = abs(amount)
    val whole = n % 1.0 == 0.0
    val mod10 = n % 10
    val mod100 = n % 100
    val base = language.substringBefore('-').substringBefore('_').lowercase()
    return when (base) {
        "ja", "ko", "zh" -> 0
        "cs" -> when {
            n == 1.0 && whole -> 0
            n >= 2 && n <= 4 && whole -> 1
            !whole -> 2
            else -> 3
        }
        "es", "it" -> when {
            n == 1.0 -> 0
            n != 0.0 && n % 1_000_000 == 0.0 -> 1
            else -> 2
        }
        "fr", "pt" -> when {
            n == 0.0 || n == 1.0 -> 0
            n % 1_000_000 == 0.0 -> 1
            else -> 2
        }
        "he", "iw" -> when {
            n == 1.0 && whole -> 0
            n == 2.0 && whole -> 1
            else -> 2
        }
        "lt" -> when {
            mod10 == 1.0 && (mod100 > 19 || mod100 < 11) -> 0
            mod10 >= 2 && mod10 <= 9 && (mod100 > 19 || mod100 < 11) -> 1
            !whole -> 2
            else -> 3
        }
        "pl" -> when {
            n == 1.0 -> 0
            mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) -> 1
            mod10 >= 0 && mod10 <= 1 || mod10 >= 5 && mod10 <= 9 ||
                mod100 >= 12 && mod100 <= 14 -> 2
            else -> 3
        }
        "ro" -> when {
            n == 1.0 -> 0
            mod100 > 19 || mod100 == 0.0 && n != 0.0 -> 2
            else -> 1
        }
        "ru" -> when {
            mod10 == 1.0 && mod100 != 11.0 -> 0
            mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) -> 1
            mod10 == 0.0 || mod10 >= 5 && mod10 <= 9 || mod100 >= 11 && mod100 <= 14 -> 2
            else -> 3
        }
        "sk" -> when {
            whole && n == 1.0 -> 0
            whole && n >= 2 && n <= 4 -> 1
            !whole -> 2
            else -> 3
        }
        "sl" -> when (mod100) {
            1.0 -> 0
            2.0 -> 1
            3.0, 4.0 -> 2
            else -> 3
        }
        "tr" -> if (n > 1) 1 else 0
        "uk" -> when {
            whole && mod10 == 1.0 && mod100 != 11.0 -> 0
            whole && mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) -> 1
            whole && (mod10 == 0.0 || mod10 >= 5 && mod10 <= 9 || mod100 >= 11 && mod100 <= 14) ->
                2
            else -> 3
        }
        else -> if (n != 1.0) 1 else 0
    }
}

/**
 * The unit's name for [amount] in [language] like Grocy's web frontend: the singular for form 0,
 * otherwise the line of [QuantityUnit.pluralForms] for that form, the plural name where it has
 * none.
 */
fun QuantityUnit.nameFor(amount: Double, language: String): String {
    val index = grocyPluralIndex(language, amount)
    val plural = namePlural?.takeIf { it.isNotBlank() } ?: name
    if (index == 0) return name
    // As Grocy reads them: form 0 is the name, the lines of the plural forms follow
    val forms = pluralForms.orEmpty().lines()
    return forms.getOrNull(index - 1)?.trim()?.takeIf { it.isNotEmpty() } ?: plural
}
