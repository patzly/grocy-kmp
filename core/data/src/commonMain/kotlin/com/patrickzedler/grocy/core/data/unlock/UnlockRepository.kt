package com.patrickzedler.grocy.core.data.unlock

import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.model.UnlockState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update

class UnlockRepository(
    private val unlockChecker: UnlockChecker?,
    private val settingsStore: SettingsStore,
) {

    private val refreshRequests = MutableStateFlow(0)

    /** Null on platforms without an unlock app. */
    val unlockState: Flow<UnlockState?> = if (unlockChecker == null) {
        flowOf(null)
    } else {
        combine(settingsStore.checkUnlockKey, refreshRequests) { checkUnlockKey, _ ->
            UnlockState(
                isKeyInstalled = unlockChecker.isKeyInstalled(),
                isPlayStoreInstalled = unlockChecker.isPlayStoreInstalled(),
                checkUnlockKey = checkUnlockKey,
            )
        }.flowOn(Dispatchers.Default)
    }

    /** Checks the installed apps again, e.g. after returning from Google Play. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    suspend fun ignoreUnlockKey() {
        settingsStore.setCheckUnlockKey(false)
    }
}
