package com.patrickzedler.grocy.core.data.scanner

import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.UnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Decides which barcode engine scans, read once at app start in [scope].
 *
 * [availableEngines] are the engines the platform ships (ML Kit only in the Play app). ZXing is
 * the default; unlocking never switches the engine, the user has to select ML Kit.
 */
class ScannerRepository(
    private val settingsStore: SettingsStore,
    unlockRepository: UnlockRepository,
    val availableEngines: Set<ScannerEngine>,
    scope: CoroutineScope,
) {

    /**
     * The chosen engine if it can be used, otherwise ZXing, which is always available. Null for
     * the moment until it is read.
     */
    val activeEngine: StateFlow<ScannerEngine?> = combine(
        settingsStore.scannerEngine,
        unlockRepository.unlockState,
    ) { selected, unlockState ->
        if (isUsable(selected, unlockState)) selected else ScannerEngine.ZXING
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** Whether the platform ships [engine] and [unlockState] allows it. */
    fun isUsable(engine: ScannerEngine, unlockState: UnlockState?): Boolean =
        engine in availableEngines &&
            (engine != ScannerEngine.ML_KIT || unlockState?.isUnlocked == true)

    suspend fun selectEngine(engine: ScannerEngine) {
        settingsStore.setScannerEngine(engine)
    }
}
