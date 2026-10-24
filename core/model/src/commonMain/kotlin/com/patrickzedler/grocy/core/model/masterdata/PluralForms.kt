package com.patrickzedler.grocy.core.model.masterdata

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
    "cs" to 4, "es" to 3, "fr" to 3, "he" to 3, "iw" to 3, "it" to 3, "ja" to 1, "pl" to 4,
    "pt" to 3, "ru" to 4, "sk" to 4, "uk" to 4, "zh" to 1,
)
