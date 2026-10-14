package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.data.BrowserLocalStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage
import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the connection in the browser's localStorage.
 *
 * Browsers offer no secure storage; the data is protected only by the same-origin policy.
 */
class WebCredentialStore internal constructor(
    private val storage: KeyValueStorage,
) : CredentialStore {

    constructor() : this(BrowserLocalStorage)

    private val state = MutableStateFlow(
        storage.get(STORAGE_KEY)?.let(ServerConnectionCodec::decode),
    )

    override val connection: Flow<ServerConnection?> = state.asStateFlow()

    override suspend fun save(connection: ServerConnection) {
        storage.set(STORAGE_KEY, ServerConnectionCodec.encode(connection))
        state.value = connection
    }

    override suspend fun clear() {
        storage.remove(STORAGE_KEY)
        state.value = null
    }

    internal companion object {
        const val STORAGE_KEY = "grocy.serverConnection"
    }
}
