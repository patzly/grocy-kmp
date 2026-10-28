package com.patrickzedler.grocy.feature.settings.impl.grocy

import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrocySettingsPagesTest {

    @Test
    fun pagesOfSwitchedOffFeaturesAreHidden() {
        val config = ServerConfig(shoppingList = false, recipes = false, batteries = false)

        val pages = GrocySettingsPage.entries.filter { it.isAvailable(config) }

        assertEquals(listOf(GrocySettingsPage.CHORES, GrocySettingsPage.TASKS), pages)
    }

    @Test
    fun everySwitchIsOnExactlyOnePage() {
        val switches = GrocySettingsPage.entries
            .flatMap { it.groups(ServerConfig()) }
            .flatMap { it.settings }
            .filterIsInstance<GrocySetting.Switch>()
            .map { it.setting }

        assertEquals(GrocySwitchSetting.entries.sorted(), switches.sorted())
    }

    @Test
    fun shoppingListChoiceOnlyWithSeveralLists() {
        fun choices(config: ServerConfig) = GrocySettingsPage.SHOPPING_LIST.groups(config)
            .flatMap { it.settings }
            .filterIsInstance<GrocySetting.AutoAddList>()

        assertEquals(1, choices(ServerConfig()).size)
        assertTrue(choices(ServerConfig(shoppingListMultipleLists = false)).isEmpty())
    }

    @Test
    fun switchesAndDaysReadGrocysDefaults() {
        val settings = UserSettings()

        assertTrue(GrocySwitchSetting.LIST_SIDE_BY_SIDE.valueOf(settings))
        assertFalse(GrocySwitchSetting.SWAP_TRACKING_BUTTONS.valueOf(settings))
        assertEquals(5, DueSoonDaysSetting.BATTERIES.valueOf(settings))
    }
}
