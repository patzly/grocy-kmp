package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.BrowserLocalStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSettingsStore internal constructor(
    private val storage: KeyValueStorage,
) : SettingsStore {

    constructor() : this(BrowserLocalStorage)

    private val checkUnlockKeyState = MutableStateFlow(
        storage.get(CHECK_UNLOCK_KEY)?.toBooleanStrictOrNull() ?: true,
    )

    override val checkUnlockKey: Flow<Boolean> = checkUnlockKeyState.asStateFlow()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        storage.set(CHECK_UNLOCK_KEY, checkUnlockKey.toString())
        checkUnlockKeyState.value = checkUnlockKey
    }

    private val scannerEngineState = MutableStateFlow(
        scannerEngineOf(storage.get(SCANNER_ENGINE)),
    )

    override val scannerEngine: Flow<ScannerEngine> = scannerEngineState.asStateFlow()

    override suspend fun setScannerEngine(scannerEngine: ScannerEngine) {
        storage.set(SCANNER_ENGINE, scannerEngine.name)
        scannerEngineState.value = scannerEngine
    }

    private val appearanceState = MutableStateFlow(
        appearanceOf(
            theme = storage.get(THEME),
            color = storage.get(COLOR),
            hue = storage.get(HUE)?.toFloatOrNull(),
            contrast = storage.get(CONTRAST),
            style = storage.get(STYLE),
        ),
    )

    override val appearance: Flow<AppearanceSettings> = appearanceState.asStateFlow()

    override suspend fun setAppearance(appearance: AppearanceSettings) {
        storage.set(THEME, appearance.theme.name)
        storage.set(COLOR, appearance.color.name)
        storage.set(HUE, appearance.hue.toString())
        storage.set(CONTRAST, appearance.contrast.name)
        storage.set(STYLE, appearance.style.name)
        appearanceState.value = appearance
    }

    private val stockOverviewOptionsState = MutableStateFlow(
        StockOverviewOptions.of(
            fields = storage.get(STOCK_OVERVIEW_FIELDS),
            grouping = storage.get(STOCK_OVERVIEW_GROUPING),
            sortField = storage.get(STOCK_OVERVIEW_SORT),
            ascending = storage.get(STOCK_OVERVIEW_ASCENDING),
        ),
    )

    override val stockOverviewOptions: Flow<StockOverviewOptions> =
        stockOverviewOptionsState.asStateFlow()

    override suspend fun setStockOverviewOptions(options: StockOverviewOptions) {
        storage.set(STOCK_OVERVIEW_FIELDS, StockOverviewOptions.storedFields(options.fields))
        storage.set(STOCK_OVERVIEW_GROUPING, options.grouping.name)
        storage.set(STOCK_OVERVIEW_SORT, options.sortField.name)
        storage.set(STOCK_OVERVIEW_ASCENDING, options.ascending.toString())
        stockOverviewOptionsState.value = options
    }

    private val shoppingListOptionsState = MutableStateFlow(
        ShoppingListOptions.of(
            fields = storage.get(SHOPPING_LIST_FIELDS),
            grouping = storage.get(SHOPPING_LIST_GROUPING),
        ),
    )

    override val shoppingListOptions: Flow<ShoppingListOptions> =
        shoppingListOptionsState.asStateFlow()

    override suspend fun setShoppingListOptions(options: ShoppingListOptions) {
        storage.set(SHOPPING_LIST_FIELDS, ShoppingListOptions.storedFields(options.fields))
        storage.set(SHOPPING_LIST_GROUPING, options.grouping.name)
        shoppingListOptionsState.value = options
    }

    private val shoppingModeSettingsState = MutableStateFlow(
        ShoppingModeSettings.of(
            autoSync = storage.get(SHOPPING_MODE_AUTO_SYNC)?.toBooleanStrictOrNull(),
            syncIntervalSeconds = storage.get(SHOPPING_MODE_SYNC_INTERVAL)?.toIntOrNull(),
            keepScreenOn = storage.get(SHOPPING_MODE_KEEP_SCREEN_ON)?.toBooleanStrictOrNull(),
            doneItemsLast = storage.get(SHOPPING_MODE_DONE_LAST)?.toBooleanStrictOrNull(),
        ),
    )

    override val shoppingModeSettings: Flow<ShoppingModeSettings> =
        shoppingModeSettingsState.asStateFlow()

    override suspend fun setShoppingModeSettings(settings: ShoppingModeSettings) {
        storage.set(SHOPPING_MODE_AUTO_SYNC, settings.autoSync.toString())
        storage.set(SHOPPING_MODE_SYNC_INTERVAL, settings.syncIntervalSeconds.toString())
        storage.set(SHOPPING_MODE_KEEP_SCREEN_ON, settings.keepScreenOn.toString())
        storage.set(SHOPPING_MODE_DONE_LAST, settings.doneItemsLast.toString())
        shoppingModeSettingsState.value = settings
    }

    private val recipesOptionsState = MutableStateFlow(
        RecipesOptions.of(
            fields = storage.get(RECIPES_FIELDS),
            grouping = storage.get(RECIPES_GROUPING),
            sortField = storage.get(RECIPES_SORT),
            ascending = storage.get(RECIPES_ASCENDING),
            view = storage.get(RECIPES_VIEW),
        ),
    )

    override val recipesOptions: Flow<RecipesOptions> = recipesOptionsState.asStateFlow()

    override suspend fun setRecipesOptions(options: RecipesOptions) {
        storage.set(RECIPES_FIELDS, RecipesOptions.storedFields(options.fields))
        storage.set(RECIPES_GROUPING, options.grouping.name)
        storage.set(RECIPES_SORT, options.sortField.name)
        storage.set(RECIPES_ASCENDING, options.ascending.toString())
        storage.set(RECIPES_VIEW, options.view.name)
        recipesOptionsState.value = options
    }

    internal companion object {
        const val CHECK_UNLOCK_KEY = "grocy.checkUnlockKey"
        const val SCANNER_ENGINE = "grocy.scannerEngine"
        const val THEME = "grocy.theme"
        const val COLOR = "grocy.color"
        const val HUE = "grocy.hue"
        const val CONTRAST = "grocy.contrast"
        const val STYLE = "grocy.style"
        const val STOCK_OVERVIEW_FIELDS = "grocy.stockOverviewFields"
        const val STOCK_OVERVIEW_GROUPING = "grocy.stockOverviewGrouping"
        const val STOCK_OVERVIEW_SORT = "grocy.stockOverviewSort"
        const val STOCK_OVERVIEW_ASCENDING = "grocy.stockOverviewAscending"
        const val SHOPPING_LIST_FIELDS = "grocy.shoppingListFields"
        const val SHOPPING_LIST_GROUPING = "grocy.shoppingListGrouping"
        const val SHOPPING_MODE_AUTO_SYNC = "grocy.shoppingModeAutoSync"
        const val SHOPPING_MODE_SYNC_INTERVAL = "grocy.shoppingModeSyncInterval"
        const val SHOPPING_MODE_KEEP_SCREEN_ON = "grocy.shoppingModeKeepScreenOn"
        const val SHOPPING_MODE_DONE_LAST = "grocy.shoppingModeDoneLast"
        const val RECIPES_FIELDS = "grocy.recipesFields"
        const val RECIPES_GROUPING = "grocy.recipesGrouping"
        const val RECIPES_SORT = "grocy.recipesSort"
        const val RECIPES_ASCENDING = "grocy.recipesAscending"
        const val RECIPES_VIEW = "grocy.recipesView"
    }
}
