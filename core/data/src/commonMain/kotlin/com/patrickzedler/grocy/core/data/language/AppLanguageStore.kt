package com.patrickzedler.grocy.core.data.language

import kotlinx.coroutines.flow.StateFlow

/**
 * The language of the app, kept the way the platform wants it: the per-app language of Android
 * (also in the system settings), browser storage on the web. Choosing another one restarts the
 * screen, so every text is loaded in the new language.
 */
interface AppLanguageStore {

    /** BCP 47 tag of the chosen language (one of `AppLanguages`), null for the system's. */
    val language: StateFlow<String?>

    fun setLanguage(tag: String?)
}
