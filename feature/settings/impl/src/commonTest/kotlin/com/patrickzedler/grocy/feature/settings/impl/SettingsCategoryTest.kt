package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
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
        assertEquals(UserSettingsRoute, SettingsCategory.USER.route)
    }

    @Test
    fun shownPageMarksItsCategory() {
        assertEquals(SettingsCategory.GENERAL, settingsCategoryOf(GeneralSettingsRoute))
        assertEquals(SettingsCategory.SERVER, settingsCategoryOf(ServerSettingsRoute))
        assertEquals(SettingsCategory.USER, settingsCategoryOf(UserSettingsRoute))
    }

    @Test
    fun pageOpenedFromACategoryMarksIt() {
        assertEquals(
            SettingsCategory.USER,
            settingsCategoryOf(StockSettingsRoute(inSettings = true)),
        )
    }

    @Test
    fun otherRoutesMarkNoCategory() {
        // The categories alone, or the log opened on top of them
        assertNull(settingsCategoryOf(SettingsRoute))
        assertNull(settingsCategoryOf(LogRoute))
        // The stock settings opened from the products
        assertNull(settingsCategoryOf(StockSettingsRoute()))
    }

    @Test
    fun categoriesWithoutPageOpenNothing() {
        assertNull(SettingsCategory.NETWORK.route)
    }
}
