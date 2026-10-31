package com.patrickzedler.grocy.feature.settings.impl.shoppingmode

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import com.patrickzedler.grocy.feature.settings.impl.numberSettingError
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * The shopping mode's settings, kept on the device: auto sync and its interval (at least
 * [ShoppingModeSettings.MIN_SYNC_INTERVAL_SECONDS], saved once typing pauses and only when
 * valid), keeping the screen on and done items at the end. Every change is saved at once.
 */
@OptIn(FlowPreview::class)
internal class ShoppingModeSettingsViewModel(
    private val repository: ShoppingModeSettingsRepository,
) : ViewModel() {

    /** Read at app start, so the page shows the stored values in its first frame. */
    val settings: StateFlow<ShoppingModeSettings?> = repository.settings

    val syncInterval = TextFieldState(
        (settings.value?.syncIntervalSeconds ?: ShoppingModeSettings.DEFAULT_SYNC_INTERVAL_SECONDS)
            .toString(),
    )

    var syncIntervalError by mutableStateOf<SettingError?>(null)
        private set

    init {
        viewModelScope.launch {
            snapshotFlow { syncInterval.text.toString() }
                .drop(1)
                .debounce(TYPING_DELAY_MILLIS)
                .collect(::saveSyncInterval)
        }
    }

    private suspend fun saveSyncInterval(text: String) {
        val error = numberSettingError(
            text = text,
            min = ShoppingModeSettings.MIN_SYNC_INTERVAL_SECONDS.toDouble(),
            max = null,
            places = 0,
        )
        syncIntervalError = error
        val seconds = parseDecimal(text)?.toInt()
        if (error == null && seconds != null) {
            repository.update { it.copy(syncIntervalSeconds = seconds) }
        }
    }

    fun setAutoSync(autoSync: Boolean) = update { it.copy(autoSync = autoSync) }

    fun setKeepScreenOn(keepScreenOn: Boolean) = update { it.copy(keepScreenOn = keepScreenOn) }

    fun setDoneItemsLast(doneItemsLast: Boolean) = update { it.copy(doneItemsLast = doneItemsLast) }

    private fun update(transform: (ShoppingModeSettings) -> ShoppingModeSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    internal companion object {
        const val TYPING_DELAY_MILLIS = 800L
    }
}
