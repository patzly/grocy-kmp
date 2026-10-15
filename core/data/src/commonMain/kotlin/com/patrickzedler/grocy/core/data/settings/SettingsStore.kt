package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.ScannerEngine
import kotlinx.coroutines.flow.Flow

interface SettingsStore {

    val checkUnlockKey: Flow<Boolean>

    suspend fun setCheckUnlockKey(checkUnlockKey: Boolean)

    /** The engine the user chose, [ScannerEngine.ZXING] by default. */
    val scannerEngine: Flow<ScannerEngine>

    suspend fun setScannerEngine(scannerEngine: ScannerEngine)
}
