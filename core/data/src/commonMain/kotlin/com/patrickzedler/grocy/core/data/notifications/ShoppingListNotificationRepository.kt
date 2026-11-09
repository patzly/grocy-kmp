package com.patrickzedler.grocy.core.data.notifications

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChangeNames
import com.patrickzedler.grocy.core.model.notifications.ShoppingListChanges
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationSettings
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Looks now and then whether a shopping list changed, while [setActive] says so. */
interface ShoppingListPoller {
    fun setActive(active: Boolean)
}

/**
 * Tells the user what others did on the shopping lists, like Bring. Grocy cannot tell anyone, so
 * [check] asks the server whether anything changed at all; only then the lists are downloaded.
 * Every download of the app counts, what it brought is compared with what this device stored
 * ([ShoppingListRepository.onRemoteChanges]), so the user's own changes never notify. All changes
 * found together are one notification, and one still showing grows instead of a second coming.
 */
class ShoppingListNotificationRepository(
    private val settingsStore: SettingsStore,
    private val connection: Flow<ServerConnection?>,
    private val serverConfig: Flow<ServerConfig>,
    private val api: GrocyApi,
    private val shopping: ShoppingListRepository,
    private val masterData: MasterDataRepository,
    private val notifier: Notifier,
    private val poller: ShoppingListPoller,
    private val scope: CoroutineScope,
) {
    /** Null only for the moment at app start until they are read. */
    val settings: StateFlow<ShoppingListNotificationSettings?> =
        settingsStore.shoppingListNotifications.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        // Here and not in start(): a process started only to check has to hear about it too
        shopping.onRemoteChanges = ::notifyAbout
    }

    /** Keeps the checks running only while someone wants them and can get them. */
    fun start() {
        scope.launch {
            combine(
                settingsStore.shoppingListNotifications,
                connection,
                serverConfig,
            ) { settings, server, config -> server != null && settings.appliesTo(config) }
                .distinctUntilChanged()
                .collect(poller::setActive)
        }
    }

    suspend fun setEnabled(type: ShoppingListNotificationType, enabled: Boolean) {
        val new = settingsStore.shoppingListNotifications.first().with(type, enabled)
        settingsStore.setShoppingListNotifications(new)
        if (!new.isEnabled) notifier.cancelShoppingListChanges()
    }

    /**
     * Downloads the lists if the server changed since the last look.
     *
     * @throws GrocyApiException if the server cannot be reached or refuses.
     */
    suspend fun check() {
        val config = serverConfig.first()
        if (!settingsStore.shoppingListNotifications.first().appliesTo(config)) return
        val server = connection.first() ?: return
        val changed = api.dbChangedTime(server)
        if (masterData.storedServerValue(CHECKED_STATE) == changed) return
        shopping.refresh()
        masterData.storeServerValue(CHECKED_STATE, changed)
    }

    private suspend fun notifyAbout(changes: ShoppingListChanges) {
        val settings = settingsStore.shoppingListNotifications.first()
        if (!settings.appliesTo(serverConfig.first())) return
        val wanted = ShoppingListChanges(
            added = changes.added.takeIf { ShoppingListNotificationType.ADDED in settings }
                .orEmpty(),
            checkedOff = changes.checkedOff
                .takeIf { ShoppingListNotificationType.CHECKED_OFF in settings }
                .orEmpty(),
        )
        if (wanted.isEmpty) return
        val products = productsNaming(wanted.added + wanted.checkedOff)
        val new = ShoppingListChangeNames(
            added = wanted.added.mapNotNull { nameOf(it, products) },
            checkedOff = wanted.checkedOff.mapNotNull { nameOf(it, products) },
        )
        if (new.isEmpty) return
        val shown = if (notifier.isShowingShoppingListChanges()) readNames() else null
        val names = shown?.plus(new) ?: new
        if (notifier.showShoppingListChanges(names)) {
            settingsStore.setShoppingListNotificationNames(json.encodeToString(names))
        }
    }

    private suspend fun productsNaming(items: List<ShoppingListItem>): Map<Int, Product> {
        val known = masterData.products.first().associateBy { it.id }
        if (items.all { it.productId == null || it.productId in known }) return known
        // A product someone created together with the item
        return try {
            masterData.refreshShoppingListNames()
            masterData.products.first().associateBy { it.id }
        } catch (_: GrocyApiException) {
            known
        }
    }

    private fun nameOf(item: ShoppingListItem, products: Map<Int, Product>): String? =
        item.productId?.let { products[it]?.name } ?: item.note?.trim()?.ifEmpty { null }

    private suspend fun readNames(): ShoppingListChangeNames? = try {
        settingsStore.readShoppingListNotificationNames()?.let {
            json.decodeFromString<ShoppingListChangeNames>(it)
        }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        // The server's change time at the last look
        const val CHECKED_STATE = "shopping_list_notification_state"
    }
}
