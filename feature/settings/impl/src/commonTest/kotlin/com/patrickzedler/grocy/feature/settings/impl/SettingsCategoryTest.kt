package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_mode_settings
import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPageRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.NetworkSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.NotificationSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.ScannerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.settings.api.ShoppingModeSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.StockSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.UserSettingsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import com.patrickzedler.grocy.feature.settings.impl.shoppingmode.shoppingModeSettingsTitle
import kotlin.test.assertNull

class SettingsCategoryTest {

    @Test
    fun categoriesWithPageOpenIt() {
        assertEquals(GeneralSettingsRoute, SettingsCategory.GENERAL.route)
        assertEquals(ServerSettingsRoute, SettingsCategory.SERVER.route)
        assertEquals(NetworkSettingsRoute, SettingsCategory.NETWORK.route)
        assertEquals(ScannerSettingsRoute, SettingsCategory.SCANNER.route)
        assertEquals(NotificationSettingsRoute, SettingsCategory.NOTIFICATIONS.route)
        assertEquals(ShoppingModeSettingsRoute(), SettingsCategory.SHOPPING_LIST.route)
    }

    @Test
    fun theShoppingListHasItsPageInsideTheSettings() {
        assertEquals(Res.string.shopping_list_list, SettingsCategory.SHOPPING_LIST.title)
        assertEquals(Res.string.shopping_list_list, shoppingModeSettingsTitle(inSettings = true))
        assertEquals(
            Res.string.shopping_mode_settings,
            shoppingModeSettingsTitle(inSettings = false),
        )
    }

    @Test
    fun shownPageMarksItsCategory() {
        assertEquals(SettingsCategory.GENERAL, settingsCategoryOf(GeneralSettingsRoute))
        assertEquals(SettingsCategory.SERVER, settingsCategoryOf(ServerSettingsRoute))
        assertEquals(SettingsCategory.SERVER, settingsCategoryOf(GrocySettingsRoute))
        assertEquals(SettingsCategory.NETWORK, settingsCategoryOf(NetworkSettingsRoute))
        assertEquals(SettingsCategory.SCANNER, settingsCategoryOf(ScannerSettingsRoute))
        assertEquals(
            SettingsCategory.SHOPPING_LIST,
            settingsCategoryOf(ShoppingModeSettingsRoute()),
        )
    }

    @Test
    fun pageOpenedFromACategoryMarksIt() {
        assertEquals(
            SettingsCategory.SERVER,
            settingsCategoryOf(StockSettingsRoute(inSettings = true)),
        )
        assertEquals(
            SettingsCategory.SERVER,
            settingsCategoryOf(GrocySettingsPageRoute(GrocySettingsPage.TASKS)),
        )
        assertEquals(SettingsCategory.SERVER, settingsCategoryOf(UserSettingsRoute))
    }

    @Test
    fun otherRoutesMarkNoCategory() {
        // The categories alone, or the log opened on top of them
        assertNull(settingsCategoryOf(SettingsRoute))
        assertNull(settingsCategoryOf(LogRoute))
        // The stock settings opened from the products, the shopping mode's from the shopping mode
        assertNull(settingsCategoryOf(StockSettingsRoute()))
        assertNull(settingsCategoryOf(ShoppingModeSettingsRoute(inSettings = false)))
    }

    @Test
    fun shoppingModeFollowsTheNotifications() {
        val categories = SettingsCategory.entries

        assertEquals(
            categories.indexOf(SettingsCategory.NOTIFICATIONS) + 1,
            categories.indexOf(SettingsCategory.SHOPPING_LIST),
        )
    }
}
