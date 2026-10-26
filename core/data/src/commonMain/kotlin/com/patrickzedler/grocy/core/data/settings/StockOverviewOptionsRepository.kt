package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the user set up the stock overview, read once at app start in [scope], so the screen shows
 * its fields, groups and order in its first frame.
 */
class StockOverviewOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored options are read. */
    val options: StateFlow<StockOverviewOptions?> =
        settingsStore.stockOverviewOptions.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current options; nothing happens before they are read. */
    suspend fun update(transform: (StockOverviewOptions) -> StockOverviewOptions) {
        val current = options.value ?: return
        settingsStore.setStockOverviewOptions(transform(current))
    }
}
