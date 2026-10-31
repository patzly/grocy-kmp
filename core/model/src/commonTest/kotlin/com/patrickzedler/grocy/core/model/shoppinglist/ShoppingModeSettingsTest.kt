package com.patrickzedler.grocy.core.model.shoppinglist

import kotlin.test.Test
import kotlin.test.assertEquals

class ShoppingModeSettingsTest {

    @Test
    fun missingValuesFallBackToDefaults() {
        assertEquals(ShoppingModeSettings(), ShoppingModeSettings.of(null, null, null, null))
    }

    @Test
    fun storedValuesAreKept() {
        assertEquals(
            ShoppingModeSettings(
                autoSync = false,
                syncIntervalSeconds = 60,
                keepScreenOn = false,
                doneItemsLast = false,
            ),
            ShoppingModeSettings.of(false, 60, false, false),
        )
    }

    @Test
    fun intervalBelowTheMinimumFallsBackToTheDefault() {
        val settings = ShoppingModeSettings.of(null, 1, null, null)

        assertEquals(
            ShoppingModeSettings.DEFAULT_SYNC_INTERVAL_SECONDS,
            settings.syncIntervalSeconds,
        )
    }
}
