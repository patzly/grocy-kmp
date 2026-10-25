package com.patrickzedler.grocy.feature.settings.impl.general

/** Name of the language [tag] in that language, capitalized, e.g. "Deutsch" for "de". */
internal expect fun languageName(tag: String): String

/**
 * Orders language names like a dictionary of the app's language: by their letters, accents
 * included ("Čeština" right after "Català"), not by character codes, which put every name
 * starting with an accent or another script at the end.
 */
internal expect fun languageNameComparator(): Comparator<String>
