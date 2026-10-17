package com.patrickzedler.grocy.core.data

@OptIn(ExperimentalWasmJsInterop::class)
internal external interface Storage : JsAny {
    fun getItem(key: String): String?

    fun setItem(key: String, value: String)

    fun removeItem(key: String)
}

internal external val localStorage: Storage

internal external val sessionStorage: Storage

/** String storage of the web stores, an interface so tests can replace the browser. */
internal interface KeyValueStorage {
    fun get(key: String): String?

    fun set(key: String, value: String)

    fun remove(key: String)
}

/**
 * A storage of the browser. Browsers throw when storage is blocked or full (e.g. blocked site
 * data, private mode); the app then keeps working for the session instead of crashing.
 */
internal open class BrowserStorage(private val storage: () -> Storage) : KeyValueStorage {

    override fun get(key: String): String? = runCatching { storage().getItem(key) }.getOrNull()

    override fun set(key: String, value: String) {
        runCatching { storage().setItem(key, value) }
    }

    override fun remove(key: String) {
        runCatching { storage().removeItem(key) }
    }
}

/** Kept until the site data is cleared. */
internal object BrowserLocalStorage : BrowserStorage({ localStorage })

/** Kept per tab (or installed app window) across reloads, gone once it closes. */
internal object BrowserSessionStorage : BrowserStorage({ sessionStorage })
