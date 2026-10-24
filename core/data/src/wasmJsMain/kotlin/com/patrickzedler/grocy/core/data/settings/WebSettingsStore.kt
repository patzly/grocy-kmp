package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.data.BrowserLocalStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebSettingsStore internal constructor(
    private val storage: KeyValueStorage,
) : SettingsStore {

    constructor() : this(BrowserLocalStorage)

    private val checkUnlockKeyState = MutableStateFlow(
        storage.get(CHECK_UNLOCK_KEY)?.toBooleanStrictOrNull() ?: true,
    )

    override val checkUnlockKey: Flow<Boolean> = checkUnlockKeyState.asStateFlow()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        storage.set(CHECK_UNLOCK_KEY, checkUnlockKey.toString())
        checkUnlockKeyState.value = checkUnlockKey
    }

    private val scannerEngineState = MutableStateFlow(
        scannerEngineOf(storage.get(SCANNER_ENGINE)),
    )

    override val scannerEngine: Flow<ScannerEngine> = scannerEngineState.asStateFlow()

    override suspend fun setScannerEngine(scannerEngine: ScannerEngine) {
        storage.set(SCANNER_ENGINE, scannerEngine.name)
        scannerEngineState.value = scannerEngine
    }

    private val appearanceState = MutableStateFlow(
        appearanceOf(
            theme = storage.get(THEME),
            color = storage.get(COLOR),
            hue = storage.get(HUE)?.toFloatOrNull(),
            contrast = storage.get(CONTRAST),
            style = storage.get(STYLE),
        ),
    )

    override val appearance: Flow<AppearanceSettings> = appearanceState.asStateFlow()

    override suspend fun setAppearance(appearance: AppearanceSettings) {
        storage.set(THEME, appearance.theme.name)
        storage.set(COLOR, appearance.color.name)
        storage.set(HUE, appearance.hue.toString())
        storage.set(CONTRAST, appearance.contrast.name)
        storage.set(STYLE, appearance.style.name)
        appearanceState.value = appearance
    }

    internal companion object {
        const val CHECK_UNLOCK_KEY = "grocy.checkUnlockKey"
        const val SCANNER_ENGINE = "grocy.scannerEngine"
        const val THEME = "grocy.theme"
        const val COLOR = "grocy.color"
        const val HUE = "grocy.hue"
        const val CONTRAST = "grocy.contrast"
        const val STYLE = "grocy.style"
    }
}
