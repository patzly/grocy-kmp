package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import kotlinx.coroutines.flow.Flow

interface SettingsStore {

    val checkUnlockKey: Flow<Boolean>

    suspend fun setCheckUnlockKey(checkUnlockKey: Boolean)

    /** The engine the user chose, [ScannerEngine.ZXING] by default. */
    val scannerEngine: Flow<ScannerEngine>

    suspend fun setScannerEngine(scannerEngine: ScannerEngine)

    val appearance: Flow<AppearanceSettings>

    suspend fun setAppearance(appearance: AppearanceSettings)

    /** Fields, grouping and sort of the stock overview, Grocy's defaults until changed. */
    val stockOverviewOptions: Flow<StockOverviewOptions>

    suspend fun setStockOverviewOptions(options: StockOverviewOptions)

    /** Shown fields and grouping of the shopping list, Grocy's defaults until changed. */
    val shoppingListOptions: Flow<ShoppingListOptions>

    suspend fun setShoppingListOptions(options: ShoppingListOptions)
}
