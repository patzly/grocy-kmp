package com.patrickzedler.grocy.core.data

@OptIn(ExperimentalWasmJsInterop::class)
internal external interface Storage : JsAny {
    fun getItem(key: String): String?

    fun setItem(key: String, value: String)

    fun removeItem(key: String)
}

internal external val localStorage: Storage

/** String storage of the web stores, an interface so tests can replace the browser. */
internal interface KeyValueStorage {
    fun get(key: String): String?

    fun set(key: String, value: String)

    fun remove(key: String)
}

/**
 * The browser's localStorage. Browsers throw when storage is blocked or full (e.g. blocked site
 * data, private mode); the app then keeps working for the session instead of crashing.
 */
internal object BrowserLocalStorage : KeyValueStorage {

    override fun get(key: String): String? = runCatching { localStorage.getItem(key) }.getOrNull()

    override fun set(key: String, value: String) {
        runCatching { localStorage.setItem(key, value) }
    }

    override fun remove(key: String) {
        runCatching { localStorage.removeItem(key) }
    }
}
