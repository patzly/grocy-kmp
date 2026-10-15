package com.patrickzedler.grocy.core.data.settings

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
}
