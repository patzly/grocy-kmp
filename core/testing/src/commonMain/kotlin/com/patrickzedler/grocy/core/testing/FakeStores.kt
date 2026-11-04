package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.scanner.ScannerForm
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [CredentialStore]; [connection] is a [StateFlow] for direct assertions. */
class FakeCredentialStore(
    initial: ServerConnection? = null,
) : CredentialStore {

    override val connection = MutableStateFlow(initial)

    override suspend fun save(connection: ServerConnection) {
        this.connection.value = connection
    }

    override suspend fun clear() {
        connection.value = null
    }
}

/** In-memory [SettingsStore] with the same defaults as the real stores. */
class FakeSettingsStore(
    checkUnlockKey: Boolean = true,
    scannerEngine: ScannerEngine = ScannerEngine.ZXING,
    appearance: AppearanceSettings = AppearanceSettings(),
    stockOverviewOptions: StockOverviewOptions = StockOverviewOptions(),
    shoppingListOptions: ShoppingListOptions = ShoppingListOptions(),
    shoppingModeSettings: ShoppingModeSettings = ShoppingModeSettings(),
    scannerForms: Set<ScannerForm> = emptySet(),
    recipesOptions: RecipesOptions = RecipesOptions(),
    choresOverviewOptions: ChoresOverviewOptions = ChoresOverviewOptions(),
    choresJournalOptions: ChoresJournalOptions = ChoresJournalOptions(),
    tasksOverviewOptions: TasksOverviewOptions = TasksOverviewOptions(),
    batteriesOverviewOptions: BatteriesOverviewOptions = BatteriesOverviewOptions(),
    batteriesJournalOptions: BatteriesJournalOptions = BatteriesJournalOptions(),
    equipmentOptions: EquipmentOptions = EquipmentOptions(),
    notificationSettings: NotificationSettings = NotificationSettings(),
    notificationDeliveries: NotificationDeliveries = NotificationDeliveries(),
) : SettingsStore {

    override val checkUnlockKey = MutableStateFlow(checkUnlockKey)

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        this.checkUnlockKey.value = checkUnlockKey
    }

    override val scannerEngine = MutableStateFlow(scannerEngine)

    override suspend fun setScannerEngine(scannerEngine: ScannerEngine) {
        this.scannerEngine.value = scannerEngine
    }

    override val appearance = MutableStateFlow(appearance)

    override suspend fun setAppearance(appearance: AppearanceSettings) {
        this.appearance.value = appearance
    }

    override val stockOverviewOptions = MutableStateFlow(stockOverviewOptions)

    override suspend fun setStockOverviewOptions(options: StockOverviewOptions) {
        stockOverviewOptions.value = options
    }

    override val shoppingListOptions = MutableStateFlow(shoppingListOptions)

    override suspend fun setShoppingListOptions(options: ShoppingListOptions) {
        shoppingListOptions.value = options
    }

    override val shoppingModeSettings = MutableStateFlow(shoppingModeSettings)

    override suspend fun setShoppingModeSettings(settings: ShoppingModeSettings) {
        shoppingModeSettings.value = settings
    }

    override val scannerForms = MutableStateFlow(scannerForms)

    override suspend fun setScannerForms(forms: Set<ScannerForm>) {
        scannerForms.value = forms
    }

    override val choresOverviewOptions = MutableStateFlow(choresOverviewOptions)

    override suspend fun setChoresOverviewOptions(options: ChoresOverviewOptions) {
        choresOverviewOptions.value = options
    }

    override val choresJournalOptions = MutableStateFlow(choresJournalOptions)

    override suspend fun setChoresJournalOptions(options: ChoresJournalOptions) {
        choresJournalOptions.value = options
    }

    override val tasksOverviewOptions = MutableStateFlow(tasksOverviewOptions)

    override suspend fun setTasksOverviewOptions(options: TasksOverviewOptions) {
        tasksOverviewOptions.value = options
    }

    override val notificationSettings = MutableStateFlow(notificationSettings)

    override suspend fun setNotificationSettings(settings: NotificationSettings) {
        notificationSettings.value = settings
    }

    override suspend fun readNotificationSettings(): NotificationSettings =
        notificationSettings.value

    val notificationDeliveries = MutableStateFlow(notificationDeliveries)

    override suspend fun readNotificationDeliveries(): NotificationDeliveries =
        notificationDeliveries.value

    override suspend fun setNotificationDeliveries(deliveries: NotificationDeliveries) {
        notificationDeliveries.value = deliveries
    }

    override val batteriesOverviewOptions = MutableStateFlow(batteriesOverviewOptions)

    override suspend fun setBatteriesOverviewOptions(options: BatteriesOverviewOptions) {
        batteriesOverviewOptions.value = options
    }

    override val batteriesJournalOptions = MutableStateFlow(batteriesJournalOptions)

    override suspend fun setBatteriesJournalOptions(options: BatteriesJournalOptions) {
        batteriesJournalOptions.value = options
    }

    override val equipmentOptions = MutableStateFlow(equipmentOptions)

    override suspend fun setEquipmentOptions(options: EquipmentOptions) {
        equipmentOptions.value = options
    }

    override val recipesOptions = MutableStateFlow(recipesOptions)

    override suspend fun setRecipesOptions(options: RecipesOptions) {
        recipesOptions.value = options
    }
}

/** [UnlockChecker] whose answers tests can change, e.g. to simulate an installation. */
class FakeUnlockChecker(
    var keyInstalled: Boolean = false,
    var playStoreInstalled: Boolean = true,
) : UnlockChecker {

    override fun isKeyInstalled(): Boolean = keyInstalled

    override fun isPlayStoreInstalled(): Boolean = playStoreInstalled
}

/** In-memory [AppLanguageStore]; [changes] counts how often the language was set. */
class FakeAppLanguageStore(
    initial: String? = null,
) : AppLanguageStore {

    override val language = MutableStateFlow(initial)

    var changes = 0
        private set

    override fun setLanguage(tag: String?) {
        changes++
        language.value = tag
    }
}
