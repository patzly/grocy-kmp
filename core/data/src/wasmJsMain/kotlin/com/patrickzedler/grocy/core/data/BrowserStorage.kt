package com.patrickzedler.grocy.core.data

@OptIn(ExperimentalWasmJsInterop::class)
internal external interface Storage : JsAny {
    fun getItem(key: String): String?

    fun setItem(key: String, value: String)

    fun removeItem(key: String)
}

internal external val localStorage: Storage
