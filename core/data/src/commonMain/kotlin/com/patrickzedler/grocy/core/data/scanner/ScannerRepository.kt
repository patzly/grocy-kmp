package com.patrickzedler.grocy.core.data.scanner

import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.model.ScannerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Decides which barcode engine scans.
 *
 * [availableEngines] are the engines the platform ships (ML Kit only in the Play app). ZXing is
 * the default; unlocking never switches the engine, the user has to select ML Kit.
 */
class ScannerRepository(
    private val settingsStore: SettingsStore,
    unlockRepository: UnlockRepository,
    private val availableEngines: Set<ScannerEngine>,
) {

    /** The chosen engine if it can be used, otherwise ZXing, which is always available. */
    val activeEngine: Flow<ScannerEngine> = combine(
        settingsStore.scannerEngine,
        unlockRepository.unlockState,
    ) { selected, unlockState ->
        val isUsable = selected in availableEngines &&
            (selected != ScannerEngine.ML_KIT || unlockState?.isUnlocked == true)
        if (isUsable) selected else ScannerEngine.ZXING
    }.distinctUntilChanged()

    suspend fun selectEngine(engine: ScannerEngine) {
        settingsStore.setScannerEngine(engine)
    }
}
