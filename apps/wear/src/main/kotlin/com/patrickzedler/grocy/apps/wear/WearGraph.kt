package com.patrickzedler.grocy.apps.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.wear.tiles.TileService
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.auth.AuthState
import com.patrickzedler.grocy.core.data.credentials.AndroidCredentialStore
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.AndroidSettingsStore
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.database.createGrocyDatabase
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.LocalNetworkGate
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.core.wear.DataLayerPhoneLink
import com.patrickzedler.grocy.core.wear.PhoneLoginMarker
import com.patrickzedler.grocy.core.wear.WearConnectionSync
import com.patrickzedler.grocy.core.wear.WearShoppingList
import com.patrickzedler.grocy.core.wear.WearTileSummary
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Manual dependency injection of the watch app, created once per process. */
class WearGraph(context: Context) {
    private val appContext = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Holds requests into the local network while Android asks the user (Android 17). */
    val localNetworkGate: LocalNetworkGate? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            LocalNetworkGate {
                appContext.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) ==
                    PackageManager.PERMISSION_GRANTED
            }
        } else {
            null
        }

    private val api = GrocyApi(createGrocyHttpClient(localNetworkGate))

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

    private val settingsStore = AndroidSettingsStore(appContext)

    val shoppingList = WearShoppingList(
        shopping = shopping,
        masterData = masterData,
        options = ShoppingListOptionsRepository(settingsStore, scope),
        modeSettings = ShoppingModeSettingsRepository(settingsStore, scope),
        language = Locale.getDefault().language,
    )

    /** What the tile shows, null while nobody is logged in. */
    val tileSummary: Flow<WearTileSummary?> =
        combine(auth.authState, shoppingList.tileSummary) { auth, summary ->
            summary.takeIf { auth is AuthState.LoggedIn }
        }.distinctUntilChanged()

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

    init {
        val listPreferences = appContext.getSharedPreferences("wear_list", Context.MODE_PRIVATE)
        // Kept across processes, so the tile and a new start show the list chosen last
        if (listPreferences.contains(KEY_LIST)) {
            shopping.selectList(listPreferences.getInt(KEY_LIST, 0))
        }
        scope.launch {
            shopping.selectedListId.filterNotNull().collect {
                listPreferences.edit().putInt(KEY_LIST, it).apply()
            }
        }
        // The tile asks for itself only rarely, a changed list should show up right away
        scope.launch {
            tileSummary.drop(1).collect {
                TileService.getUpdater(appContext)
                    .requestUpdate(ShoppingListTileService::class.java)
            }
        }
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

    private companion object {
        const val KEY_LIST = "list_id"
    }
}
