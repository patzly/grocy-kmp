package com.patrickzedler.grocy.feature.settings.impl.general

internal actual fun systemLanguageTag(): String? = browserLanguage()

/** index.html replaces navigator.language with the app's language and keeps the browser's. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.grocySystemLanguage || navigator.language || null")
private external fun browserLanguage(): String?
