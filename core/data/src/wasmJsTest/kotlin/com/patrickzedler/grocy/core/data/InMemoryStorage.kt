package com.patrickzedler.grocy.core.data

internal class InMemoryStorage(
    vararg entries: Pair<String, String>,
) : KeyValueStorage {

    val values = mutableMapOf(*entries)

    override fun get(key: String): String? = values[key]

    override fun set(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
