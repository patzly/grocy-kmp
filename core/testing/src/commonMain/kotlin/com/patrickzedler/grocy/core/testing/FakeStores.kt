package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.ServerConnection
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
