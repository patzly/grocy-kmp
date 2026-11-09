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
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.network.CertificateSettings
import com.patrickzedler.grocy.core.model.network.NetworkSettings
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationSettings
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlinx.serialization.json.Json

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

    override val scanDueDates: Flow<Boolean> = preferences
        .map { it[SCAN_DUE_DATES] ?: true }
        .distinctUntilChanged()

    override suspend fun setScanDueDates(scanDueDates: Boolean) {
        dataStore.edit { it[SCAN_DUE_DATES] = scanDueDates }
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

    override val behaviorSettings: Flow<BehaviorSettings> = preferences
        .map {
            BehaviorSettings.of(
                productFactsSources = it[BEHAVIOR_PRODUCT_FACTS],
                dateKeyboard = it[BEHAVIOR_DATE_KEYBOARD],
                dateInputOrder = it[BEHAVIOR_DATE_ORDER],
                barcodeUsesAmount = it[BEHAVIOR_BARCODE_AMOUNT],
            )
        }
        .distinctUntilChanged()

    override suspend fun setBehaviorSettings(settings: BehaviorSettings) {
        dataStore.edit {
            it[BEHAVIOR_PRODUCT_FACTS] =
                BehaviorSettings.storedSources(settings.productFactsSources)
            it[BEHAVIOR_DATE_KEYBOARD] = settings.dateKeyboard
            it[BEHAVIOR_DATE_ORDER] = settings.dateInputOrder.name
            it[BEHAVIOR_BARCODE_AMOUNT] = settings.barcodeUsesAmount
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

    override val scannerForms: Flow<Set<ScannerForm>> = preferences
        .map { ScannerForm.of(it[SCANNER_FORMS]) }
        .distinctUntilChanged()

    override suspend fun setScannerForms(forms: Set<ScannerForm>) {
        dataStore.edit { it[SCANNER_FORMS] = ScannerForm.stored(forms) }
    }

    override val choresOverviewOptions: Flow<ChoresOverviewOptions> = preferences
        .map { ChoresOverviewOptions.of(it[CHORES_OVERVIEW_OPTIONS]) }
        .distinctUntilChanged()

    override suspend fun setChoresOverviewOptions(options: ChoresOverviewOptions) {
        dataStore.edit { it[CHORES_OVERVIEW_OPTIONS] = options.stored() }
    }

    override val choresJournalOptions: Flow<ChoresJournalOptions> = preferences
        .map { ChoresJournalOptions.of(it[CHORES_JOURNAL_OPTIONS]) }
        .distinctUntilChanged()

    override suspend fun setChoresJournalOptions(options: ChoresJournalOptions) {
        dataStore.edit { it[CHORES_JOURNAL_OPTIONS] = options.stored() }
    }

    override val tasksOverviewOptions: Flow<TasksOverviewOptions> = preferences
        .map { TasksOverviewOptions.of(it[TASKS_OVERVIEW_OPTIONS]) }
        .distinctUntilChanged()

    override suspend fun setTasksOverviewOptions(options: TasksOverviewOptions) {
        dataStore.edit { it[TASKS_OVERVIEW_OPTIONS] = options.stored() }
    }

    override val shoppingListNotifications: Flow<ShoppingListNotificationSettings> = preferences
        .map { ShoppingListNotificationSettings.of(it[SHOPPING_LIST_NOTIFICATIONS]) }
        .distinctUntilChanged()

    override suspend fun setShoppingListNotifications(settings: ShoppingListNotificationSettings) {
        dataStore.edit { it[SHOPPING_LIST_NOTIFICATIONS] = settings.stored() }
    }

    override suspend fun readShoppingListNotificationNames(): String? =
        preferences.first()[SHOPPING_LIST_NOTIFICATION_NAMES]

    override suspend fun setShoppingListNotificationNames(names: String) {
        dataStore.edit { it[SHOPPING_LIST_NOTIFICATION_NAMES] = names }
    }

    override val startLayout: Flow<String?> = preferences
        .map { it[START_LAYOUT] }
        .distinctUntilChanged()

    override suspend fun setStartLayout(layout: String) {
        dataStore.edit { it[START_LAYOUT] = layout }
    }

    override val notificationSettings: Flow<NotificationSettings> = preferences
        .map { NotificationSettings.of(it[NOTIFICATION_SETTINGS]) }
        .distinctUntilChanged()

    override suspend fun setNotificationSettings(settings: NotificationSettings) {
        dataStore.edit { it[NOTIFICATION_SETTINGS] = settings.stored() }
    }

    override suspend fun readNotificationSettings(): NotificationSettings =
        notificationSettings.first()

    override suspend fun readNotificationDeliveries(): NotificationDeliveries =
        NotificationDeliveries.of(preferences.first()[NOTIFICATION_DELIVERIES])

    override suspend fun setNotificationDeliveries(deliveries: NotificationDeliveries) {
        dataStore.edit { it[NOTIFICATION_DELIVERIES] = deliveries.stored() }
    }

    override val batteriesOverviewOptions: Flow<BatteriesOverviewOptions> = preferences
        .map { BatteriesOverviewOptions.of(it[BATTERIES_OVERVIEW_OPTIONS]) }
        .distinctUntilChanged()

    override suspend fun setBatteriesOverviewOptions(options: BatteriesOverviewOptions) {
        dataStore.edit { it[BATTERIES_OVERVIEW_OPTIONS] = options.stored() }
    }

    override val batteriesJournalOptions: Flow<BatteriesJournalOptions> = preferences
        .map { BatteriesJournalOptions.of(it[BATTERIES_JOURNAL_OPTIONS]) }
        .distinctUntilChanged()

    override suspend fun setBatteriesJournalOptions(options: BatteriesJournalOptions) {
        dataStore.edit { it[BATTERIES_JOURNAL_OPTIONS] = options.stored() }
    }

    override val equipmentOptions: Flow<EquipmentOptions> = preferences
        .map { EquipmentOptions.of(it[EQUIPMENT_OPTIONS]) }
        .distinctUntilChanged()

    override suspend fun setEquipmentOptions(options: EquipmentOptions) {
        dataStore.edit { it[EQUIPMENT_OPTIONS] = options.stored() }
    }

    override val calendarView: Flow<CalendarView> = preferences
        .map { CalendarView.of(it[CALENDAR_VIEW]) }
        .distinctUntilChanged()

    override suspend fun setCalendarView(view: CalendarView) {
        dataStore.edit { it[CALENDAR_VIEW] = view.stored() }
    }

    override val networkSettings: Flow<NetworkSettings> = preferences
        .map {
            NetworkSettings.of(
                useTor = it[NETWORK_TOR],
                httpProxyEnabled = it[NETWORK_HTTP_PROXY],
                httpProxyHost = it[NETWORK_HTTP_PROXY_HOST],
                httpProxyPort = it[NETWORK_HTTP_PROXY_PORT],
            )
        }
        .distinctUntilChanged()

    override val certificateSettings: Flow<CertificateSettings> = preferences
        .map { stored -> stored[CERTIFICATE_SETTINGS]?.let(::certificateSettingsOf) ?: CertificateSettings() }
        .distinctUntilChanged()

    override suspend fun setCertificateSettings(settings: CertificateSettings) {
        dataStore.edit { it[CERTIFICATE_SETTINGS] = Json.encodeToString(settings) }
    }

    // Unreadable data of a future version counts as none, connections then work like by default
    private fun certificateSettingsOf(stored: String): CertificateSettings? = try {
        Json.decodeFromString<CertificateSettings>(stored)
    } catch (_: IllegalArgumentException) {
        null
    }

    override suspend fun setNetworkSettings(settings: NetworkSettings) {
        dataStore.edit {
            it[NETWORK_TOR] = settings.useTor
            it[NETWORK_HTTP_PROXY] = settings.httpProxyEnabled
            it[NETWORK_HTTP_PROXY_HOST] = settings.httpProxyHost
            it[NETWORK_HTTP_PROXY_PORT] = settings.httpProxyPort
        }
    }

    override val recipesOptions: Flow<RecipesOptions> = preferences
        .map {
            RecipesOptions.of(
                fields = it[RECIPES_FIELDS],
                grouping = it[RECIPES_GROUPING],
                sortField = it[RECIPES_SORT],
                ascending = it[RECIPES_ASCENDING]?.toString(),
                view = it[RECIPES_VIEW],
            )
        }
        .distinctUntilChanged()

    override suspend fun setRecipesOptions(options: RecipesOptions) {
        dataStore.edit {
            it[RECIPES_FIELDS] = RecipesOptions.storedFields(options.fields)
            it[RECIPES_GROUPING] = options.grouping.name
            it[RECIPES_SORT] = options.sortField.name
            it[RECIPES_ASCENDING] = options.ascending
            it[RECIPES_VIEW] = options.view.name
        }
    }

    private companion object {
        const val FILE_NAME = "settings"
        val CHECK_UNLOCK_KEY = booleanPreferencesKey("check_unlock_key")
        val SCANNER_ENGINE = stringPreferencesKey("scanner_engine")
        val SCAN_DUE_DATES = booleanPreferencesKey("scan_due_dates")
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
        val BEHAVIOR_PRODUCT_FACTS = stringPreferencesKey("behavior_product_facts")
        val BEHAVIOR_DATE_KEYBOARD = booleanPreferencesKey("behavior_date_keyboard")
        val BEHAVIOR_DATE_ORDER = stringPreferencesKey("behavior_date_order")
        val BEHAVIOR_BARCODE_AMOUNT = booleanPreferencesKey("behavior_barcode_amount")
        val SHOPPING_MODE_AUTO_SYNC = booleanPreferencesKey("shopping_mode_auto_sync")
        val SHOPPING_MODE_SYNC_INTERVAL = intPreferencesKey("shopping_mode_sync_interval")
        val SHOPPING_MODE_KEEP_SCREEN_ON = booleanPreferencesKey("shopping_mode_keep_screen_on")
        val SHOPPING_MODE_DONE_LAST = booleanPreferencesKey("shopping_mode_done_last")
        val RECIPES_FIELDS = stringPreferencesKey("recipes_fields")
        val RECIPES_GROUPING = stringPreferencesKey("recipes_grouping")
        val RECIPES_SORT = stringPreferencesKey("recipes_sort")
        val RECIPES_ASCENDING = booleanPreferencesKey("recipes_ascending")
        val RECIPES_VIEW = stringPreferencesKey("recipes_view")
        val SCANNER_FORMS = stringPreferencesKey("scanner_forms")
        val CHORES_OVERVIEW_OPTIONS = stringPreferencesKey("chores_overview_options")
        val CHORES_JOURNAL_OPTIONS = stringPreferencesKey("chores_journal_options")
        val TASKS_OVERVIEW_OPTIONS = stringPreferencesKey("tasks_overview_options")
        val BATTERIES_OVERVIEW_OPTIONS = stringPreferencesKey("batteries_overview_options")
        val BATTERIES_JOURNAL_OPTIONS = stringPreferencesKey("batteries_journal_options")
        val EQUIPMENT_OPTIONS = stringPreferencesKey("equipment_options")
        val CALENDAR_VIEW = stringPreferencesKey("calendar_view")
        val CERTIFICATE_SETTINGS = stringPreferencesKey("certificate_settings")
        val NETWORK_TOR = booleanPreferencesKey("network_tor")
        val NETWORK_HTTP_PROXY = booleanPreferencesKey("network_http_proxy")
        val NETWORK_HTTP_PROXY_HOST = stringPreferencesKey("network_http_proxy_host")
        val NETWORK_HTTP_PROXY_PORT = intPreferencesKey("network_http_proxy_port")
        val NOTIFICATION_SETTINGS = stringPreferencesKey("notification_settings")
        val NOTIFICATION_DELIVERIES = stringPreferencesKey("notification_deliveries")
        val START_LAYOUT = stringPreferencesKey("start_layout")
        val SHOPPING_LIST_NOTIFICATIONS = stringPreferencesKey("shopping_list_notifications")
        val SHOPPING_LIST_NOTIFICATION_NAMES =
            stringPreferencesKey("shopping_list_notification_names")
    }
}
