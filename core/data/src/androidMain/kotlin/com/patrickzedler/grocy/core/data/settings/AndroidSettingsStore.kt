package com.patrickzedler.grocy.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
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

    override val checkUnlockKey: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences -> preferences[CHECK_UNLOCK_KEY] ?: true }
        .distinctUntilChanged()

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        dataStore.edit { it[CHECK_UNLOCK_KEY] = checkUnlockKey }
    }

    private companion object {
        const val FILE_NAME = "settings"
        val CHECK_UNLOCK_KEY = booleanPreferencesKey("check_unlock_key")
    }
}
