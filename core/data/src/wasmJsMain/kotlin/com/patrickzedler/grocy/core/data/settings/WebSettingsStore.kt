package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.BrowserLocalStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSettingsStore internal constructor(
    private val storage: KeyValueStorage,
) : SettingsStore {

    constructor() : this(BrowserLocalStorage)

    private val checkUnlockKeyState = MutableStateFlow(
        storage.get(CHECK_UNLOCK_KEY)?.toBooleanStrictOrNull() ?: true,
    )

    override val checkUnlockKey: Flow<Boolean> = checkUnlockKeyState.asStateFlow()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        storage.set(CHECK_UNLOCK_KEY, checkUnlockKey.toString())
        checkUnlockKeyState.value = checkUnlockKey
    }

    internal companion object {
        const val CHECK_UNLOCK_KEY = "grocy.checkUnlockKey"
    }
}
