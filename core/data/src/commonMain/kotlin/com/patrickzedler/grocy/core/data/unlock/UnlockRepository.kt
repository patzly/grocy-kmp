package com.patrickzedler.grocy.core.data.unlock

import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.model.UnlockState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The unlock state, read once at app start in [scope], so screens have it in their first frame
 * instead of adding the unlock items a moment later.
 */
class UnlockRepository(
    private val unlockChecker: UnlockChecker?,
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
    // Package manager lookups block, so they run off the main thread
    private val checkDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val refreshRequests = MutableStateFlow(0)

    /** Null on platforms without an unlock app, and for the moment until it is read. */
    val unlockState: StateFlow<UnlockState?> = if (unlockChecker == null) {
        MutableStateFlow(null)
    } else {
        combine(settingsStore.checkUnlockKey, refreshRequests) { checkUnlockKey, _ ->
            UnlockState(
                isKeyInstalled = unlockChecker.isKeyInstalled(),
                isPlayStoreInstalled = unlockChecker.isPlayStoreInstalled(),
                checkUnlockKey = checkUnlockKey,
            )
        }.flowOn(checkDispatcher).stateIn(scope, SharingStarted.Eagerly, null)
    }

    /** Checks the installed apps again, e.g. after returning from Google Play. */
    fun refresh() {
        refreshRequests.update { it + 1 }
    }

    suspend fun ignoreUnlockKey() {
        settingsStore.setCheckUnlockKey(false)
    }
}
