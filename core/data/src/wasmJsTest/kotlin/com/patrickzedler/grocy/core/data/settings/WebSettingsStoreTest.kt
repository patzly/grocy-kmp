package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.InMemoryStorage
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.BehaviorSettings
import com.patrickzedler.grocy.core.model.DateInputOrder
import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.equipment.EquipmentOptions
import com.patrickzedler.grocy.core.model.notifications.NotificationDeliveries
import com.patrickzedler.grocy.core.model.notifications.NotificationDelivery
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeGrouping
import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import com.patrickzedler.grocy.core.model.recipes.RecipesView
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class WebSettingsStoreTest {

    @Test
    fun checksUnlockKeyByDefault() = runTest {
        assertTrue(WebSettingsStore(InMemoryStorage()).checkUnlockKey.first())
    }

    @Test
    fun invalidStoredValueFallsBackToDefault() = runTest {
        val storage = InMemoryStorage(WebSettingsStore.CHECK_UNLOCK_KEY to "maybe")

        assertTrue(WebSettingsStore(storage).checkUnlockKey.first())
    }

    @Test
    fun changedValueIsStoredAndEmitted() = runTest {
        val storage = InMemoryStorage()
        val store = WebSettingsStore(storage)

        store.setCheckUnlockKey(false)

        assertFalse(store.checkUnlockKey.first())
        assertEquals("false", storage.values[WebSettingsStore.CHECK_UNLOCK_KEY])
        assertFalse(WebSettingsStore(storage).checkUnlockKey.first())
    }

    @Test
    fun scannerEngineIsStoredAndEmitted() = runTest {
        val storage = InMemoryStorage()
        val store = WebSettingsStore(storage)
        assertEquals(ScannerEngine.ZXING, store.scannerEngine.first())

        store.setScannerEngine(ScannerEngine.ML_KIT)

        assertEquals(ScannerEngine.ML_KIT, store.scannerEngine.first())
        assertEquals(ScannerEngine.ML_KIT, WebSettingsStore(storage).scannerEngine.first())
    }

    @Test
    fun scanDueDatesIsOnUntilTurnedOff() = runTest {
        val storage = InMemoryStorage()
        val store = WebSettingsStore(storage)
        assertEquals(true, store.scanDueDates.first())

        store.setScanDueDates(false)

        assertEquals(false, store.scanDueDates.first())
        assertEquals(false, WebSettingsStore(storage).scanDueDates.first())
    }

    @Test
    fun appearanceIsStoredAndRestored() = runTest {
        val storage = InMemoryStorage()
        val appearance = AppearanceSettings(
            theme = AppTheme.DARK,
            color = AppColor.DYNAMIC,
            hue = 42.5f,
            style = AppPaletteStyle.VIBRANT,
            contrast = AppContrast.HIGH,
        )

        WebSettingsStore(storage).setAppearance(appearance)

        assertEquals(appearance, WebSettingsStore(storage).appearance.first())
    }

    @Test
    fun storedAppearanceOfEarlierVersionsStaysReadable() = runTest {
        val storage = InMemoryStorage(
            WebSettingsStore.THEME to "LIGHT",
            WebSettingsStore.COLOR to "STATIC",
            WebSettingsStore.HUE to "200.0",
            WebSettingsStore.CONTRAST to "MEDIUM",
        )

        assertEquals(
            AppearanceSettings(
                theme = AppTheme.LIGHT,
                color = AppColor.STATIC,
                hue = 200f,
                contrast = AppContrast.MEDIUM,
            ),
            WebSettingsStore(storage).appearance.first(),
        )
    }

    @Test
    fun stockOverviewOptionsAreStoredAndRestored() = runTest {
        val storage = InMemoryStorage()
        val options = StockOverviewOptions(
            fields = setOf(StockOverviewField.VALUE),
            grouping = StockOverviewGrouping.PRODUCT_GROUP,
            sortField = StockOverviewField.VALUE,
            ascending = false,
        )

        WebSettingsStore(storage).setStockOverviewOptions(options)

        assertEquals(options, WebSettingsStore(storage).stockOverviewOptions.first())
    }

    @Test
    fun recipesOptionsAreStoredAndRestored() = runTest {
        val storage = InMemoryStorage()
        val options = RecipesOptions(
            fields = setOf(RecipeField.DESIRED_SERVINGS),
            grouping = RecipeGrouping.DUE_SCORE,
            sortField = RecipeField.DESIRED_SERVINGS,
            ascending = false,
            view = RecipesView.GALLERY,
        )

        WebSettingsStore(storage).setRecipesOptions(options)

        assertEquals(options, WebSettingsStore(storage).recipesOptions.first())
    }

    @Test
    fun shoppingListOptionsAreStoredAndRestored() = runTest {
        val storage = InMemoryStorage()
        val options = ShoppingListOptions(
            fields = setOf(ShoppingListField.LAST_PRICE_TOTAL),
            grouping = ShoppingListGrouping.DEFAULT_STORE,
        )

        WebSettingsStore(storage).setShoppingListOptions(options)

        assertEquals(options, WebSettingsStore(storage).shoppingListOptions.first())
    }

    @Test
    fun behaviorSettingsAreStoredAndRestored() = runTest {
        val storage = InMemoryStorage()
        val settings = BehaviorSettings(
            productFactsSources = setOf(ProductFactsSource.FOOD, ProductFactsSource.BEAUTY),
            dateKeyboard = true,
            dateInputOrder = DateInputOrder.MONTH_FIRST,
            barcodeUsesAmount = false,
        )

        WebSettingsStore(storage).setBehaviorSettings(settings)

        assertEquals(settings, WebSettingsStore(storage).behaviorSettings.first())
    }

    @Test
    fun shoppingModeSettingsAreStoredAndRestored() = runTest {
        val storage = InMemoryStorage()
        val settings = ShoppingModeSettings(
            autoSync = false,
            syncIntervalSeconds = 45,
            keepScreenOn = false,
            doneItemsLast = false,
        )

        WebSettingsStore(storage).setShoppingModeSettings(settings)

        assertEquals(settings, WebSettingsStore(storage).shoppingModeSettings.first())
    }

    @Test
    fun storedShoppingModeSettingsStayReadable() = runTest {
        val storage = InMemoryStorage()
        storage.set(WebSettingsStore.SHOPPING_MODE_AUTO_SYNC, "false")
        storage.set(WebSettingsStore.SHOPPING_MODE_SYNC_INTERVAL, "20")
        storage.set(WebSettingsStore.SHOPPING_MODE_KEEP_SCREEN_ON, "true")
        storage.set(WebSettingsStore.SHOPPING_MODE_DONE_LAST, "false")

        assertEquals(
            ShoppingModeSettings(
                autoSync = false,
                syncIntervalSeconds = 20,
                keepScreenOn = true,
                doneItemsLast = false,
            ),
            WebSettingsStore(storage).shoppingModeSettings.first(),
        )
    }

    @Test
    fun choresOverviewOptionsSurviveARestart() = runTest {
        val storage = InMemoryStorage()
        val options = ChoresOverviewOptions(grouping = ChoresOverviewGrouping.ASSIGNED_TO)

        WebSettingsStore(storage).setChoresOverviewOptions(options)

        assertEquals(options, WebSettingsStore(storage).choresOverviewOptions.first())
    }

    @Test
    fun choresJournalOptionsSurviveARestart() = runTest {
        val storage = InMemoryStorage()
        val options = ChoresJournalOptions(grouping = ChoresJournalGrouping.CHORE)

        WebSettingsStore(storage).setChoresJournalOptions(options)

        assertEquals(options, WebSettingsStore(storage).choresJournalOptions.first())
    }

    @Test
    fun tasksOverviewOptionsSurviveARestart() = runTest {
        val storage = InMemoryStorage()
        val options = TasksOverviewOptions(grouping = TasksOverviewGrouping.CATEGORY)

        WebSettingsStore(storage).setTasksOverviewOptions(options)

        assertEquals(options, WebSettingsStore(storage).tasksOverviewOptions.first())
    }

    @Test
    fun notificationSettingsAreStoredAndReadBack() = runTest {
        val storage = InMemoryStorage()
        val settings = NotificationSettings().let {
            it.with(NotificationKind.STOCK, it[NotificationKind.STOCK].copy(enabled = true))
        }

        WebSettingsStore(storage).setNotificationSettings(settings)

        assertEquals(settings, WebSettingsStore(storage).notificationSettings.first())
    }

    @Test
    fun whatAnotherTabStoredIsRead() = runTest {
        val storage = InMemoryStorage()
        val thisTab = WebSettingsStore(storage)
        val otherTab = WebSettingsStore(storage)
        val settings = NotificationSettings().let {
            it.with(NotificationKind.TASKS, it[NotificationKind.TASKS].copy(enabled = true))
        }
        val deliveries = NotificationDeliveries(
            mapOf(NotificationKind.TASKS to NotificationDelivery(day = 20_000)),
        )

        otherTab.setNotificationSettings(settings)
        otherTab.setNotificationDeliveries(deliveries)

        assertEquals(settings, thisTab.readNotificationSettings())
        assertEquals(settings, thisTab.notificationSettings.first())
        assertEquals(deliveries, thisTab.readNotificationDeliveries())
    }

    @Test
    fun batteriesAndEquipmentOptionsSurviveARestart() = runTest {
        val storage = InMemoryStorage()
        val overview = BatteriesOverviewOptions(grouping = BatteriesOverviewGrouping.USED_IN)
        val journal = BatteriesJournalOptions(grouping = BatteriesJournalGrouping.BATTERY)
        val equipment = EquipmentOptions(sortUserfield = "room")

        WebSettingsStore(storage).run {
            setBatteriesOverviewOptions(overview)
            setBatteriesJournalOptions(journal)
            setEquipmentOptions(equipment)
        }

        val restarted = WebSettingsStore(storage)
        assertEquals(overview, restarted.batteriesOverviewOptions.first())
        assertEquals(journal, restarted.batteriesJournalOptions.first())
        assertEquals(equipment, restarted.equipmentOptions.first())
    }
}
