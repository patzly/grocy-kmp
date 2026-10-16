package com.patrickzedler.grocy.feature.settings.impl.general

import java.util.Locale

internal actual fun languageName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}
