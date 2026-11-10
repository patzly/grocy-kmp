package com.patrickzedler.grocy.core.model.shoppinglist

/**
 * How the shopping mode behaves, kept on the device: whether it downloads the list by itself
 * every [syncIntervalSeconds] so several people can shop together, whether it keeps the screen
 * on and whether done items move to the end of their group; and whether what is typed as the
 * product of a new item and names none becomes its note ([unknownProductAsNote]).
 */
data class ShoppingModeSettings(
    val autoSync: Boolean = true,
    val syncIntervalSeconds: Int = DEFAULT_SYNC_INTERVAL_SECONDS,
    val keepScreenOn: Boolean = true,
    val doneItemsLast: Boolean = true,
    val unknownProductAsNote: Boolean = false,
) {
    companion object {
        const val DEFAULT_SYNC_INTERVAL_SECONDS = 10

        /** Shorter intervals would only load the server without anyone noticing. */
        const val MIN_SYNC_INTERVAL_SECONDS = 5

        /** Settings from their stored parts; missing or invalid values fall back to defaults. */
        fun of(
            autoSync: Boolean?,
            syncIntervalSeconds: Int?,
            keepScreenOn: Boolean?,
            doneItemsLast: Boolean?,
            unknownProductAsNote: Boolean? = null,
        ): ShoppingModeSettings {
            val default = ShoppingModeSettings()
            return ShoppingModeSettings(
                autoSync = autoSync ?: default.autoSync,
                syncIntervalSeconds = syncIntervalSeconds
                    ?.takeIf { it >= MIN_SYNC_INTERVAL_SECONDS }
                    ?: default.syncIntervalSeconds,
                keepScreenOn = keepScreenOn ?: default.keepScreenOn,
                doneItemsLast = doneItemsLast ?: default.doneItemsLast,
                unknownProductAsNote = unknownProductAsNote ?: default.unknownProductAsNote,
            )
        }
    }
}
