package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the shopping mode syncs and shows the list, read once at app start in [scope], so the
 * shopping mode and its settings show the stored values in their first frame.
 */
class ShoppingModeSettingsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored settings are read. */
    val settings: StateFlow<ShoppingModeSettings?> =
        settingsStore.shoppingModeSettings.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current settings; nothing happens before they are read. */
    suspend fun update(transform: (ShoppingModeSettings) -> ShoppingModeSettings) {
        val current = settings.value ?: return
        settingsStore.setShoppingModeSettings(transform(current))
    }
}
