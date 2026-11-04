package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.BehaviorSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The behavior settings, read once at app start in [scope], so date fields and the settings page
 * show the stored values in their first frame.
 */
class BehaviorSettingsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored settings are read. */
    val settings: StateFlow<BehaviorSettings?> =
        settingsStore.behaviorSettings.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current settings; nothing happens before they are read. */
    suspend fun update(transform: (BehaviorSettings) -> BehaviorSettings) {
        val current = settings.value ?: return
        settingsStore.setBehaviorSettings(transform(current))
    }
}
