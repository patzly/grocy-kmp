package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingValuesTest {

    @Test
    fun storedEngineNameIsRead() {
        assertEquals(ScannerEngine.ML_KIT, scannerEngineOf("ML_KIT"))
        assertEquals(ScannerEngine.ZXING, scannerEngineOf("ZXING"))
    }

    @Test
    fun missingOrUnknownEngineIsZxing() {
        assertEquals(ScannerEngine.ZXING, scannerEngineOf(null))
        assertEquals(ScannerEngine.ZXING, scannerEngineOf("REMOVED_ENGINE"))
    }

    @Test
    fun missingAppearanceIsTheDefault() {
        assertEquals(AppearanceSettings(), appearanceOf(null, null, null, null))
    }

    @Test
    fun unknownAppearanceValuesFallBackOneByOne() {
        assertEquals(
            AppearanceSettings(theme = AppTheme.DARK),
            appearanceOf(theme = "DARK", color = "RAINBOW", hue = 400f, contrast = "EXTREME"),
        )
    }

    @Test
    fun invalidHueFallsBackToTheDefault() {
        assertEquals(AppearanceSettings.DEFAULT_HUE, appearanceOf(null, null, Float.NaN, null).hue)
        assertEquals(360f, appearanceOf(null, null, 360f, null).hue)
    }
}
