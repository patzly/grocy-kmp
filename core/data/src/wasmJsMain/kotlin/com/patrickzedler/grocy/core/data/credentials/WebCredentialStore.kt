package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the connection in the browser's localStorage.
 *
 * Browsers offer no secure storage; the data is protected only by the same-origin policy.
 */
class WebCredentialStore : CredentialStore {

    private val state = MutableStateFlow(
        localStorage.getItem(STORAGE_KEY)?.let(ServerConnectionCodec::decode),
    )

    override val connection: Flow<ServerConnection?> = state.asStateFlow()

    override suspend fun save(connection: ServerConnection) {
        localStorage.setItem(STORAGE_KEY, ServerConnectionCodec.encode(connection))
        state.value = connection
    }

    override suspend fun clear() {
        localStorage.removeItem(STORAGE_KEY)
        state.value = null
    }

    private companion object {
        const val STORAGE_KEY = "grocy.serverConnection"
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private external interface Storage : JsAny {
    fun getItem(key: String): String?

    fun setItem(key: String, value: String)

    fun removeItem(key: String)
}

private external val localStorage: Storage
