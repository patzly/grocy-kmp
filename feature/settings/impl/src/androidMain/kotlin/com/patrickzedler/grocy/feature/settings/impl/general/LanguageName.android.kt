package com.patrickzedler.grocy.feature.settings.impl.general

import java.text.Collator
import java.util.Locale

internal actual fun languageName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}

internal actual fun languageNameComparator(): Comparator<String> {
    val collator = Collator.getInstance(Locale.getDefault())
    return Comparator { a, b -> collator.compare(a, b) }
}
