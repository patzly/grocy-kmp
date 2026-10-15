package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.InMemoryStorage
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
}
