package com.patrickzedler.grocy.core.data.credentials

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.patrickzedler.grocy.core.model.ServerConnection
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Stores the connection in DataStore, encrypted with a key from the Android Keystore.
 */
class AndroidCredentialStore(
    context: Context,
) : CredentialStore {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(FILE_NAME) },
    )
    private val cipher = KeystoreCipher(KEY_ALIAS)

    override val connection: Flow<ServerConnection?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
            preferences[CONNECTION_KEY]
                ?.let(cipher::decrypt)
                ?.let(ServerConnectionCodec::decode)
        }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()

    override suspend fun save(connection: ServerConnection) {
        val encrypted = withContext(Dispatchers.IO) {
            cipher.encrypt(ServerConnectionCodec.encode(connection))
        }
        dataStore.edit { it[CONNECTION_KEY] = encrypted }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(CONNECTION_KEY) }
    }

    private companion object {
        const val FILE_NAME = "credentials"
        const val KEY_ALIAS = "grocy_credentials"
        val CONNECTION_KEY = stringPreferencesKey("server_connection")
    }
}
