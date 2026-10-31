package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.InMemoryStorage
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
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
}
