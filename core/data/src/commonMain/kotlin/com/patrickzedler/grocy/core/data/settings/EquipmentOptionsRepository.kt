package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the user set up the equipment list, read once at app start in [scope], so the screen shows
 * its fields, groups and order in its first frame.
 */
class EquipmentOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored options are read. */
    val options: StateFlow<EquipmentOptions?> =
        settingsStore.equipmentOptions.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current options; nothing happens before they are read. */
    suspend fun update(transform: (EquipmentOptions) -> EquipmentOptions) {
        val current = options.value ?: return
        settingsStore.setEquipmentOptions(transform(current))
    }
}
