package com.patrickzedler.grocy.apps.wear

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.credentials.AndroidCredentialStore
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.database.createGrocyDatabase
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.core.wear.DataLayerPhoneLink
import com.patrickzedler.grocy.core.wear.PhoneLoginMarker
import com.patrickzedler.grocy.core.wear.WearConnectionSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

/** Manual dependency injection of the watch app, created once per process. */
class WearGraph(context: Context) {
    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val api = GrocyApi(createGrocyHttpClient())

    private val credentialStore = AndroidCredentialStore(appContext)

    private val database = createGrocyDatabase(appContext)

    val masterData = MasterDataRepository(
        api = api,
        database = database,
        connection = { credentialStore.connection.first() },
    )

    val shopping = ShoppingListRepository(
        api = api,
        database = database,
        scope = scope,
        connection = { credentialStore.connection.first() },
        networkAvailable = networkAvailable(),
    )

    val auth = AuthRepository(credentialStore, api) { masterData.clear() }

    val sync = WearConnectionSync(credentialStore, auth, SharedPreferencesMarker(appContext))

    val phone = DataLayerPhoneLink(appContext)

    /** For work that must outlive a service, which ends as soon as its callback returns. */
    val appScope: CoroutineScope
        get() = scope

    // The default network of the watch: its own or the phone's over Bluetooth
    private fun networkAvailable(): Flow<Unit> = callbackFlow {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Unit)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }

    private class SharedPreferencesMarker(context: Context) : PhoneLoginMarker {
        private val preferences = context.getSharedPreferences("wear_login", Context.MODE_PRIVATE)

        override suspend fun isFromPhone(): Boolean = preferences.getBoolean(KEY, false)

        override suspend fun setFromPhone(fromPhone: Boolean) {
            preferences.edit().putBoolean(KEY, fromPhone).apply()
        }

        private companion object {
            const val KEY = "from_phone"
        }
    }
}
