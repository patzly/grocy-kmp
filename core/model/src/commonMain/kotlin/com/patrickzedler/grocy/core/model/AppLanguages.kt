package com.patrickzedler.grocy.core.model

/**
 * BCP 47 tags of the languages the app is translated into (`values-<tag>` in core:resources, with
 * regions as `values-pt-rBR`), the languages of grocy-android. locales_config.xml of apps:shared
 * must list the same, the Android build reads its language filter from there.
 */
val AppLanguages: List<String> = listOf(
    "en", "ca", "cs", "de", "el", "es", "et", "fr", "he", "hu", "it", "ja", "nb", "nl", "pl",
    "pt-BR", "pt-PT", "ru", "sk", "sv", "tr", "uk", "zh-CN", "zh-TW",
)

/**
 * The translation used for [tag]: itself if listed, else its base language ("de-CH" uses "de"),
 * null for the system language or a language the app has no translation of.
 */
fun resolveAppLanguage(tag: String?, languages: List<String> = AppLanguages): String? {
    if (tag.isNullOrBlank()) return null
    return languages.firstOrNull { it.equals(tag, ignoreCase = true) }
        ?: languages.firstOrNull { it.equals(tag.substringBefore('-'), ignoreCase = true) }
}

/** Languages written from right to left, by ISO code (also "iw", Android's old code for Hebrew). */
private val RightToLeftLanguages = setOf(
    "ar", "ckb", "dv", "fa", "he", "iw", "ks", "ku", "ps", "sd", "ug", "ur", "yi",
)

/** True if the language of the BCP 47 [tag] is written from right to left, e.g. "he-IL". */
fun isRightToLeft(tag: String): Boolean =
    tag.substringBefore('-').lowercase() in RightToLeftLanguages
