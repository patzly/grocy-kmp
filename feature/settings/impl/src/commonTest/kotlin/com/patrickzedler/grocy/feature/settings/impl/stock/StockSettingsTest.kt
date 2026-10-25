package com.patrickzedler.grocy.feature.settings.impl.stock

import com.patrickzedler.grocy.core.model.server.ProductPresets
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StockSettingsTest {

    private val settings = UserSettings(
        stockDecimalPlacesAmounts = 2,
        stockDueSoonDays = 7,
        stockDefaultPurchaseAmount = 1.5,
        productPresets = ProductPresets(locationId = 3, defaultDueDays = -1),
    )

    @Test
    fun numbersShowTheStoredValues() {
        assertEquals("7", StockNumberSetting.DUE_SOON_DAYS.textOf(settings, '.'))
        assertEquals("1,5", StockNumberSetting.PURCHASE_AMOUNT.textOf(settings, ','))
        assertEquals("-1", StockNumberSetting.PRESET_DUE_DAYS.textOf(settings, '.'))
    }

    @Test
    fun numbersFollowGrocysRanges() {
        assertNull(StockNumberSetting.PRESET_DUE_DAYS.errorOf("-1", settings))
        assertEquals(
            SettingError.InvalidNumber("1", null, 0),
            StockNumberSetting.DUE_SOON_DAYS.errorOf("0", settings),
        )
        assertEquals(
            SettingError.InvalidNumber("0", "10", 0),
            StockNumberSetting.DECIMALS_AMOUNTS.errorOf("11", settings),
        )
        // Amounts allow the decimal places of amounts
        assertNull(StockNumberSetting.PURCHASE_AMOUNT.errorOf("0,25", settings))
        assertEquals(
            SettingError.InvalidNumber("0", null, 2),
            StockNumberSetting.PURCHASE_AMOUNT.errorOf("0.125", settings),
        )
        assertEquals(
            SettingError.InvalidNumber("1", null, 0),
            StockNumberSetting.DUE_SOON_DAYS.errorOf("", settings),
        )
    }

    @Test
    fun valuesAreStoredAsGrocysTypes() {
        assertEquals(
            UserSettingValue.Whole(5),
            StockNumberSetting.DUE_SOON_DAYS.valueOf("5", settings),
        )
        assertEquals(
            UserSettingValue.Number(0.5),
            StockNumberSetting.CONSUME_AMOUNT.valueOf("0,5", settings),
        )
        assertEquals(UserSettingValue.Whole(-1), StockPresetSetting.LOCATION.valueOf(null as Int?))
        assertEquals(UserSettingValue.Whole(4), StockPresetSetting.QUANTITY_UNIT.valueOf(4))
    }

    @Test
    fun presetsAndSwitchesReadTheStoredSettings() {
        assertEquals(3, StockPresetSetting.LOCATION.valueOf(settings))
        assertNull(StockPresetSetting.PRODUCT_GROUP.valueOf(settings))
        assertEquals(
            true,
            StockSwitchSetting.TREAT_OPENED_AS_OUT_OF_STOCK.valueOf(settings),
        )
    }
}
