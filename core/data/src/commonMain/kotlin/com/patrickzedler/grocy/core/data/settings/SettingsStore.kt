package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
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

    /** How the shopping mode syncs and shows the list, the defaults until changed. */
    val shoppingModeSettings: Flow<ShoppingModeSettings>

    suspend fun setShoppingModeSettings(settings: ShoppingModeSettings)

    /** Fields, grouping, sort and view of the recipes, Grocy's defaults until changed. */
    val recipesOptions: Flow<RecipesOptions>

    suspend fun setRecipesOptions(options: RecipesOptions)

    /** The forms that open with their scanner, because it was used there last. */
    val scannerForms: Flow<Set<ScannerForm>>

    suspend fun setScannerForms(forms: Set<ScannerForm>)

    /** How the user set up the chores overview. */
    val choresOverviewOptions: Flow<ChoresOverviewOptions>

    suspend fun setChoresOverviewOptions(options: ChoresOverviewOptions)

    /** How the user set up the chores journal. */
    val choresJournalOptions: Flow<ChoresJournalOptions>

    suspend fun setChoresJournalOptions(options: ChoresJournalOptions)

    /** How the user set up the tasks page. */
    val tasksOverviewOptions: Flow<TasksOverviewOptions>

    suspend fun setTasksOverviewOptions(options: TasksOverviewOptions)

    /** Which notifications the user turned on, none until changed. */
    val notificationSettings: Flow<NotificationSettings>

    suspend fun setNotificationSettings(settings: NotificationSettings)

    /**
     * The settings as stored right now, also what another tab of the web app stored since this
     * one started; what decides whether to notify.
     */
    suspend fun readNotificationSettings(): NotificationSettings

    /** What was delivered as stored right now, so tabs of the web app do not notify twice. */
    suspend fun readNotificationDeliveries(): NotificationDeliveries

    suspend fun setNotificationDeliveries(deliveries: NotificationDeliveries)

    /** How the user set up the batteries overview. */
    val batteriesOverviewOptions: Flow<BatteriesOverviewOptions>

    suspend fun setBatteriesOverviewOptions(options: BatteriesOverviewOptions)

    /** How the user set up the batteries journal. */
    val batteriesJournalOptions: Flow<BatteriesJournalOptions>

    suspend fun setBatteriesJournalOptions(options: BatteriesJournalOptions)

    /** How the user set up the equipment list. */
    val equipmentOptions: Flow<EquipmentOptions>

    suspend fun setEquipmentOptions(options: EquipmentOptions)
}
