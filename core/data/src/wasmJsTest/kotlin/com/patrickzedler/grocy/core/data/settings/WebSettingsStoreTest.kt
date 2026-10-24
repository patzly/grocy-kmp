package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.InMemoryStorage
import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
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
}
