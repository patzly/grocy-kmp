package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the app looks, read once at app start in [scope]. The theme needs it before the first frame
 * and the settings screens in theirs, so their controls never jump to the stored values.
 */
class AppearanceRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored appearance is read. */
    val appearance: StateFlow<AppearanceSettings?> =
        settingsStore.appearance.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current appearance; nothing happens before it is read. */
    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        val current = appearance.value ?: return
        settingsStore.setAppearance(transform(current))
    }
}
