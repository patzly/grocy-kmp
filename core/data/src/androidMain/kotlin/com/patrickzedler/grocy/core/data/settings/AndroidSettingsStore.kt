package com.patrickzedler.grocy.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class AndroidSettingsStore(
    context: Context,
) : SettingsStore {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(FILE_NAME) },
    )

    private val preferences = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val checkUnlockKey: Flow<Boolean> = preferences
        .map { it[CHECK_UNLOCK_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        dataStore.edit { it[CHECK_UNLOCK_KEY] = checkUnlockKey }
    }

    override val scannerEngine: Flow<ScannerEngine> = preferences
        .map { scannerEngineOf(it[SCANNER_ENGINE]) }
        .distinctUntilChanged()

    override suspend fun setScannerEngine(scannerEngine: ScannerEngine) {
        dataStore.edit { it[SCANNER_ENGINE] = scannerEngine.name }
    }

    override val appearance: Flow<AppearanceSettings> = preferences
        .map { appearanceOf(it[THEME], it[COLOR], it[HUE], it[CONTRAST], it[STYLE]) }
        .distinctUntilChanged()

    override suspend fun setAppearance(appearance: AppearanceSettings) {
        dataStore.edit {
            it[THEME] = appearance.theme.name
            it[COLOR] = appearance.color.name
            it[HUE] = appearance.hue
            it[CONTRAST] = appearance.contrast.name
            it[STYLE] = appearance.style.name
        }
    }

    override val stockOverviewOptions: Flow<StockOverviewOptions> = preferences
        .map {
            StockOverviewOptions.of(
                fields = it[STOCK_OVERVIEW_FIELDS],
                grouping = it[STOCK_OVERVIEW_GROUPING],
                sortField = it[STOCK_OVERVIEW_SORT],
                ascending = it[STOCK_OVERVIEW_ASCENDING]?.toString(),
            )
        }
        .distinctUntilChanged()

    override suspend fun setStockOverviewOptions(options: StockOverviewOptions) {
        dataStore.edit {
            it[STOCK_OVERVIEW_FIELDS] = StockOverviewOptions.storedFields(options.fields)
            it[STOCK_OVERVIEW_GROUPING] = options.grouping.name
            it[STOCK_OVERVIEW_SORT] = options.sortField.name
            it[STOCK_OVERVIEW_ASCENDING] = options.ascending
        }
    }

    override val shoppingListOptions: Flow<ShoppingListOptions> = preferences
        .map { ShoppingListOptions.of(it[SHOPPING_LIST_FIELDS], it[SHOPPING_LIST_GROUPING]) }
        .distinctUntilChanged()

    override suspend fun setShoppingListOptions(options: ShoppingListOptions) {
        dataStore.edit {
            it[SHOPPING_LIST_FIELDS] = ShoppingListOptions.storedFields(options.fields)
            it[SHOPPING_LIST_GROUPING] = options.grouping.name
        }
    }

    override val shoppingModeSettings: Flow<ShoppingModeSettings> = preferences
        .map {
            ShoppingModeSettings.of(
                autoSync = it[SHOPPING_MODE_AUTO_SYNC],
                syncIntervalSeconds = it[SHOPPING_MODE_SYNC_INTERVAL],
                keepScreenOn = it[SHOPPING_MODE_KEEP_SCREEN_ON],
                doneItemsLast = it[SHOPPING_MODE_DONE_LAST],
            )
        }
        .distinctUntilChanged()

    override suspend fun setShoppingModeSettings(settings: ShoppingModeSettings) {
        dataStore.edit {
            it[SHOPPING_MODE_AUTO_SYNC] = settings.autoSync
            it[SHOPPING_MODE_SYNC_INTERVAL] = settings.syncIntervalSeconds
            it[SHOPPING_MODE_KEEP_SCREEN_ON] = settings.keepScreenOn
            it[SHOPPING_MODE_DONE_LAST] = settings.doneItemsLast
        }
    }

    private companion object {
        const val FILE_NAME = "settings"
        val CHECK_UNLOCK_KEY = booleanPreferencesKey("check_unlock_key")
        val SCANNER_ENGINE = stringPreferencesKey("scanner_engine")
        val THEME = stringPreferencesKey("theme")
        val COLOR = stringPreferencesKey("color")
        val HUE = floatPreferencesKey("hue")
        val CONTRAST = stringPreferencesKey("contrast")
        val STYLE = stringPreferencesKey("style")
        val STOCK_OVERVIEW_FIELDS = stringPreferencesKey("stock_overview_fields")
        val STOCK_OVERVIEW_GROUPING = stringPreferencesKey("stock_overview_grouping")
        val STOCK_OVERVIEW_SORT = stringPreferencesKey("stock_overview_sort")
        val STOCK_OVERVIEW_ASCENDING = booleanPreferencesKey("stock_overview_ascending")
        val SHOPPING_LIST_FIELDS = stringPreferencesKey("shopping_list_fields")
        val SHOPPING_LIST_GROUPING = stringPreferencesKey("shopping_list_grouping")
        val SHOPPING_MODE_AUTO_SYNC = booleanPreferencesKey("shopping_mode_auto_sync")
        val SHOPPING_MODE_SYNC_INTERVAL = intPreferencesKey("shopping_mode_sync_interval")
        val SHOPPING_MODE_KEEP_SCREEN_ON = booleanPreferencesKey("shopping_mode_keep_screen_on")
        val SHOPPING_MODE_DONE_LAST = booleanPreferencesKey("shopping_mode_done_last")
    }
}
