package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSettingsStore : SettingsStore {

    private val checkUnlockKeyState = MutableStateFlow(
        localStorage.getItem(CHECK_UNLOCK_KEY)?.toBooleanStrictOrNull() ?: true,
    )

    override val checkUnlockKey: Flow<Boolean> = checkUnlockKeyState.asStateFlow()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        localStorage.setItem(CHECK_UNLOCK_KEY, checkUnlockKey.toString())
        checkUnlockKeyState.value = checkUnlockKey
    }

    private companion object {
        const val CHECK_UNLOCK_KEY = "grocy.checkUnlockKey"
    }
}
