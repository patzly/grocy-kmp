package com.patrickzedler.grocy.feature.settings.impl.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.scanner.ScannerRepository
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.UnlockState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The scanner engine the user chooses from what the app ships: both in the Play app, ML Kit there
 * only once unlocked, ZXing alone in the FOSS and web apps.
 */
internal class ScannerSettingsViewModel(
    private val scannerRepository: ScannerRepository,
    private val unlockRepository: UnlockRepository,
) : ViewModel() {

    /** Read at app start, so the page shows the engine in its first frame. */
    val activeEngine: StateFlow<ScannerEngine?> = scannerRepository.activeEngine

    /** Null without an unlock app, which hides everything related to it. */
    val unlockState: StateFlow<UnlockState?> = unlockRepository.unlockState

    /** The choices of this app, in the order of [ScannerEngine]. */
    val engines: List<ScannerEngine> =
        ScannerEngine.entries.filter { it in scannerRepository.availableEngines }

    fun isSelectable(engine: ScannerEngine, unlockState: UnlockState?): Boolean =
        scannerRepository.isUsable(engine, unlockState)

    /** Buying needs Google Play, like the unlock item in About. */
    fun showsUnlockHint(unlockState: UnlockState?): Boolean =
        ScannerEngine.ML_KIT in engines &&
            unlockState?.isUnlocked == false &&
            unlockState.isPlayStoreInstalled

    fun selectEngine(engine: ScannerEngine) {
        viewModelScope.launch { scannerRepository.selectEngine(engine) }
    }

    /** Checks the installed apps again, e.g. after returning from Google Play. */
    fun refreshUnlockState() {
        unlockRepository.refresh()
    }
}
