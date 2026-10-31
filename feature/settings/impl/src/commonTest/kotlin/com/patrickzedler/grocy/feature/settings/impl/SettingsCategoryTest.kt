package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPageRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.settings.api.ShoppingModeSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.StockSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.UserSettingsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsCategoryTest {

    @Test
    fun categoriesWithPageOpenIt() {
        assertEquals(GeneralSettingsRoute, SettingsCategory.GENERAL.route)
        assertEquals(ServerSettingsRoute, SettingsCategory.SERVER.route)
        assertEquals(GrocySettingsRoute, SettingsCategory.GROCY.route)
        assertEquals(ShoppingModeSettingsRoute(), SettingsCategory.SHOPPING_MODE.route)
    }

    @Test
    fun shownPageMarksItsCategory() {
        assertEquals(SettingsCategory.GENERAL, settingsCategoryOf(GeneralSettingsRoute))
        assertEquals(SettingsCategory.SERVER, settingsCategoryOf(ServerSettingsRoute))
        assertEquals(SettingsCategory.GROCY, settingsCategoryOf(GrocySettingsRoute))
        assertEquals(
            SettingsCategory.SHOPPING_MODE,
            settingsCategoryOf(ShoppingModeSettingsRoute()),
        )
    }

    @Test
    fun pageOpenedFromACategoryMarksIt() {
        assertEquals(
            SettingsCategory.GROCY,
            settingsCategoryOf(StockSettingsRoute(inSettings = true)),
        )
        assertEquals(
            SettingsCategory.GROCY,
            settingsCategoryOf(GrocySettingsPageRoute(GrocySettingsPage.TASKS)),
        )
        assertEquals(SettingsCategory.GROCY, settingsCategoryOf(UserSettingsRoute))
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
            categories.indexOf(SettingsCategory.SHOPPING_MODE),
        )
    }

    @Test
    fun categoriesWithoutPageOpenNothing() {
        assertNull(SettingsCategory.NETWORK.route)
    }
}
