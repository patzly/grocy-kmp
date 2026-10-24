package com.patrickzedler.grocy.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class AndroidSettingsStore(
    context: Context,
) : SettingsStore {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(FILE_NAME) },
    )

    private val preferences = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val checkUnlockKey: Flow<Boolean> = preferences
        .map { it[CHECK_UNLOCK_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        dataStore.edit { it[CHECK_UNLOCK_KEY] = checkUnlockKey }
    }

    override val scannerEngine: Flow<ScannerEngine> = preferences
        .map { scannerEngineOf(it[SCANNER_ENGINE]) }
        .distinctUntilChanged()

    override suspend fun setScannerEngine(scannerEngine: ScannerEngine) {
        dataStore.edit { it[SCANNER_ENGINE] = scannerEngine.name }
    }

    override val appearance: Flow<AppearanceSettings> = preferences
        .map { appearanceOf(it[THEME], it[COLOR], it[HUE], it[CONTRAST], it[STYLE]) }
        .distinctUntilChanged()

    override suspend fun setAppearance(appearance: AppearanceSettings) {
        dataStore.edit {
            it[THEME] = appearance.theme.name
            it[COLOR] = appearance.color.name
            it[HUE] = appearance.hue
            it[CONTRAST] = appearance.contrast.name
            it[STYLE] = appearance.style.name
        }
    }

    private companion object {
        const val FILE_NAME = "settings"
        val CHECK_UNLOCK_KEY = booleanPreferencesKey("check_unlock_key")
        val SCANNER_ENGINE = stringPreferencesKey("scanner_engine")
        val THEME = stringPreferencesKey("theme")
        val COLOR = stringPreferencesKey("color")
        val HUE = floatPreferencesKey("hue")
        val CONTRAST = stringPreferencesKey("contrast")
        val STYLE = stringPreferencesKey("style")
    }
}
