package com.patrickzedler.grocy.core.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsStore {

    val checkUnlockKey: Flow<Boolean>

    suspend fun setCheckUnlockKey(checkUnlockKey: Boolean)
}
