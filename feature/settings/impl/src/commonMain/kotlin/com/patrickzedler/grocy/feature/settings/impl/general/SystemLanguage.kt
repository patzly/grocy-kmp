package com.patrickzedler.grocy.feature.settings.impl.general

/** BCP 47 tag of the device's language, also while the app uses another one; null if unknown. */
internal expect fun systemLanguageTag(): String?
