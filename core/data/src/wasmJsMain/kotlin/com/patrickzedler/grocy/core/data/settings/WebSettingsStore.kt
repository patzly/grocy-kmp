package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.BrowserLocalStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.BehaviorSettings
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

    private val behaviorSettingsState = MutableStateFlow(
        BehaviorSettings.of(
            productFactsSources = storage.get(BEHAVIOR_PRODUCT_FACTS),
            dateKeyboard = storage.get(BEHAVIOR_DATE_KEYBOARD)?.toBooleanStrictOrNull(),
            dateInputOrder = storage.get(BEHAVIOR_DATE_ORDER),
            barcodeUsesAmount = storage.get(BEHAVIOR_BARCODE_AMOUNT)?.toBooleanStrictOrNull(),
        ),
    )

    override val behaviorSettings: Flow<BehaviorSettings> = behaviorSettingsState.asStateFlow()

    override suspend fun setBehaviorSettings(settings: BehaviorSettings) {
        storage.set(
            BEHAVIOR_PRODUCT_FACTS,
            BehaviorSettings.storedSources(settings.productFactsSources),
        )
        storage.set(BEHAVIOR_DATE_KEYBOARD, settings.dateKeyboard.toString())
        storage.set(BEHAVIOR_DATE_ORDER, settings.dateInputOrder.name)
        storage.set(BEHAVIOR_BARCODE_AMOUNT, settings.barcodeUsesAmount.toString())
        behaviorSettingsState.value = settings
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

    private val scannerFormsState = MutableStateFlow(ScannerForm.of(storage.get(SCANNER_FORMS)))

    override val scannerForms: Flow<Set<ScannerForm>> = scannerFormsState.asStateFlow()

    override suspend fun setScannerForms(forms: Set<ScannerForm>) {
        storage.set(SCANNER_FORMS, ScannerForm.stored(forms))
        scannerFormsState.value = forms
    }

    private val choresOverviewOptionsState =
        MutableStateFlow(ChoresOverviewOptions.of(storage.get(CHORES_OVERVIEW_OPTIONS)))

    override val choresOverviewOptions: Flow<ChoresOverviewOptions> =
        choresOverviewOptionsState.asStateFlow()

    override suspend fun setChoresOverviewOptions(options: ChoresOverviewOptions) {
        storage.set(CHORES_OVERVIEW_OPTIONS, options.stored())
        choresOverviewOptionsState.value = options
    }

    private val choresJournalOptionsState =
        MutableStateFlow(ChoresJournalOptions.of(storage.get(CHORES_JOURNAL_OPTIONS)))

    override val choresJournalOptions: Flow<ChoresJournalOptions> =
        choresJournalOptionsState.asStateFlow()

    override suspend fun setChoresJournalOptions(options: ChoresJournalOptions) {
        storage.set(CHORES_JOURNAL_OPTIONS, options.stored())
        choresJournalOptionsState.value = options
    }

    private val tasksOverviewOptionsState =
        MutableStateFlow(TasksOverviewOptions.of(storage.get(TASKS_OVERVIEW_OPTIONS)))

    override val tasksOverviewOptions: Flow<TasksOverviewOptions> =
        tasksOverviewOptionsState.asStateFlow()

    override suspend fun setTasksOverviewOptions(options: TasksOverviewOptions) {
        storage.set(TASKS_OVERVIEW_OPTIONS, options.stored())
        tasksOverviewOptionsState.value = options
    }

    private val notificationSettingsState =
        MutableStateFlow(NotificationSettings.of(storage.get(NOTIFICATION_SETTINGS)))

    override val notificationSettings: Flow<NotificationSettings> =
        notificationSettingsState.asStateFlow()

    override suspend fun setNotificationSettings(settings: NotificationSettings) {
        storage.set(NOTIFICATION_SETTINGS, settings.stored())
        notificationSettingsState.value = settings
    }

    override suspend fun readNotificationSettings(): NotificationSettings =
        NotificationSettings.of(storage.get(NOTIFICATION_SETTINGS)).also {
            notificationSettingsState.value = it
        }

    override suspend fun readNotificationDeliveries(): NotificationDeliveries =
        NotificationDeliveries.of(storage.get(NOTIFICATION_DELIVERIES))

    override suspend fun setNotificationDeliveries(deliveries: NotificationDeliveries) {
        storage.set(NOTIFICATION_DELIVERIES, deliveries.stored())
    }

    private val batteriesOverviewOptionsState =
        MutableStateFlow(BatteriesOverviewOptions.of(storage.get(BATTERIES_OVERVIEW_OPTIONS)))

    override val batteriesOverviewOptions: Flow<BatteriesOverviewOptions> =
        batteriesOverviewOptionsState.asStateFlow()

    override suspend fun setBatteriesOverviewOptions(options: BatteriesOverviewOptions) {
        storage.set(BATTERIES_OVERVIEW_OPTIONS, options.stored())
        batteriesOverviewOptionsState.value = options
    }

    private val batteriesJournalOptionsState =
        MutableStateFlow(BatteriesJournalOptions.of(storage.get(BATTERIES_JOURNAL_OPTIONS)))

    override val batteriesJournalOptions: Flow<BatteriesJournalOptions> =
        batteriesJournalOptionsState.asStateFlow()

    override suspend fun setBatteriesJournalOptions(options: BatteriesJournalOptions) {
        storage.set(BATTERIES_JOURNAL_OPTIONS, options.stored())
        batteriesJournalOptionsState.value = options
    }

    private val equipmentOptionsState =
        MutableStateFlow(EquipmentOptions.of(storage.get(EQUIPMENT_OPTIONS)))

    override val equipmentOptions: Flow<EquipmentOptions> =
        equipmentOptionsState.asStateFlow()

    override suspend fun setEquipmentOptions(options: EquipmentOptions) {
        storage.set(EQUIPMENT_OPTIONS, options.stored())
        equipmentOptionsState.value = options
    }

    private val calendarViewState = MutableStateFlow(CalendarView.of(storage.get(CALENDAR_VIEW)))

    override val calendarView: Flow<CalendarView> = calendarViewState.asStateFlow()

    override suspend fun setCalendarView(view: CalendarView) {
        storage.set(CALENDAR_VIEW, view.stored())
        calendarViewState.value = view
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
        const val BEHAVIOR_PRODUCT_FACTS = "grocy.behaviorProductFacts"
        const val BEHAVIOR_DATE_KEYBOARD = "grocy.behaviorDateKeyboard"
        const val BEHAVIOR_DATE_ORDER = "grocy.behaviorDateOrder"
        const val BEHAVIOR_BARCODE_AMOUNT = "grocy.behaviorBarcodeAmount"
        const val SHOPPING_MODE_AUTO_SYNC = "grocy.shoppingModeAutoSync"
        const val SHOPPING_MODE_SYNC_INTERVAL = "grocy.shoppingModeSyncInterval"
        const val SHOPPING_MODE_KEEP_SCREEN_ON = "grocy.shoppingModeKeepScreenOn"
        const val SHOPPING_MODE_DONE_LAST = "grocy.shoppingModeDoneLast"
        const val RECIPES_FIELDS = "grocy.recipesFields"
        const val RECIPES_GROUPING = "grocy.recipesGrouping"
        const val RECIPES_SORT = "grocy.recipesSort"
        const val RECIPES_ASCENDING = "grocy.recipesAscending"
        const val RECIPES_VIEW = "grocy.recipesView"
        const val SCANNER_FORMS = "grocy.scannerForms"
        const val CHORES_OVERVIEW_OPTIONS = "grocy.choresOverviewOptions"
        const val CHORES_JOURNAL_OPTIONS = "grocy.choresJournalOptions"
        const val TASKS_OVERVIEW_OPTIONS = "grocy.tasksOverviewOptions"
        const val BATTERIES_OVERVIEW_OPTIONS = "grocy.batteriesOverviewOptions"
        const val BATTERIES_JOURNAL_OPTIONS = "grocy.batteriesJournalOptions"
        const val EQUIPMENT_OPTIONS = "grocy.equipmentOptions"
        const val CALENDAR_VIEW = "grocy.calendarView"
        const val NOTIFICATION_SETTINGS = "grocy.notificationSettings"
        const val NOTIFICATION_DELIVERIES = "grocy.notificationDeliveries"
    }
}
