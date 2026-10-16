package com.patrickzedler.grocy.core.data.language

import com.patrickzedler.grocy.core.data.BrowserLocalStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps the language in browser storage. index.html hands it to the browser's language list
 * before the app starts, where Compose reads the language of the texts; a change therefore
 * reloads the page.
 */
class WebAppLanguageStore internal constructor(
    private val storage: KeyValueStorage,
    private val reload: () -> Unit,
) : AppLanguageStore {

    constructor() : this(BrowserLocalStorage, ::reloadPage)

    private val languageState = MutableStateFlow(storage.get(LANGUAGE)?.ifBlank { null })

    override val language: StateFlow<String?> = languageState.asStateFlow()

    override fun setLanguage(tag: String?) {
        if (tag == languageState.value) return
        if (tag == null) storage.remove(LANGUAGE) else storage.set(LANGUAGE, tag)
        languageState.value = tag
        reload()
    }

    internal companion object {
        // index.html reads the same key
        const val LANGUAGE = "grocy.language"
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => location.reload()")
private external fun reloadPage()
