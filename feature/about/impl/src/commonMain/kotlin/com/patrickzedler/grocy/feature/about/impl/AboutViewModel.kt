package com.patrickzedler.grocy.feature.about.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.data.update.UpdateRepository
import com.patrickzedler.grocy.core.data.util.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.core.data.util.withMinimumDuration
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val KEY_LONG_CLICKS_TO_IGNORE = 10

internal class AboutViewModel(
    private val unlockRepository: UnlockRepository,
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    /** Read at app start, so the unlock items are there in the first frame. */
    val unlockState: StateFlow<UnlockState?> = unlockRepository.unlockState

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

    var isCheckingForUpdate by mutableStateOf(false)
        private set

    /** Answer of the last check the user asked for, shown as dialog until dismissed. */
    var updateCheck by mutableStateOf<UpdateCheck?>(null)
        private set

    fun checkForUpdate() {
        if (isCheckingForUpdate) return
        isCheckingForUpdate = true
        viewModelScope.launch {
            val result = try {
                withMinimumDuration(MINIMUM_LOADING_DURATION) { updateRepository.check() }
            } finally {
                isCheckingForUpdate = false
            }
            updateCheck = result
        }
    }

    /** Closing the dialog of a found update means later. */
    fun dismissUpdateCheck() {
        (updateCheck as? UpdateCheck.Available)?.let(updateRepository::postpone)
        updateCheck = null
    }

    fun installUpdate() {
        updateCheck = null
        viewModelScope.launch { updateRepository.install() }
    }
}
