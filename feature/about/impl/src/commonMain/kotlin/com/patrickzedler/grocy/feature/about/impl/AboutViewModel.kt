package com.patrickzedler.grocy.feature.about.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.model.UnlockState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val KEY_LONG_CLICKS_TO_IGNORE = 10

internal class AboutViewModel(
    private val unlockRepository: UnlockRepository,
) : ViewModel() {

    val unlockState: StateFlow<UnlockState?> = unlockRepository.unlockState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = null,
    )

    private var keyLongClickCount = 0

    fun refreshUnlockState() {
        unlockRepository.refresh()
    }

    fun onKeyLongClick() {
        val state = unlockState.value ?: return
        if (state.isKeyInstalled || !state.checkUnlockKey) return
        keyLongClickCount++
        if (keyLongClickCount >= KEY_LONG_CLICKS_TO_IGNORE) {
            keyLongClickCount = 0
            viewModelScope.launch { unlockRepository.ignoreUnlockKey() }
        }
    }
}
