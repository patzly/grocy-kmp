package com.patrickzedler.grocy.core.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.auth.AuthState
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.network.ServerUrl
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row of the watch's list. */
data class WearShoppingItem(
    val id: Int,
    val title: String,
    /** Amount, unit and note, null if there is nothing to add to the title. */
    val details: String?,
    val done: Boolean,
    val sync: ShoppingListItemSync?,
)

/** The list the watch shows. */
data class WearShoppingState(
    val lists: List<ShoppingList> = emptyList(),
    val listId: Int? = null,
    /** Not done first, each by name, so what is left stays at the top while shopping. */
    val items: List<WearShoppingItem> = emptyList(),
)

/** What the watch shows. */
sealed interface WearState {
    /** The stored login is not read yet. */
    data object Loading : WearState

    data object LoggedOut : WearState

    data class LoggedIn(val shopping: WearShoppingState) : WearState
}

/** The login entered on the watch. */
data class WearLoginForm(
    val serverUrl: String = "",
    val apiKey: String = "",
    val isConnecting: Boolean = false,
    val error: WearLoginError? = null,
)

enum class WearLoginError {
    INVALID_ADDRESS,
    FAILED,
}

/**
 * The watch's only screen state: the login and one shopping list to show and check off. Offline
 * first like the phone: check offs are stored and queued by [ShoppingListRepository], which
 * replays them per item against the server's current state.
 */
class WearViewModel(
    private val auth: AuthRepository,
    private val shopping: ShoppingListRepository,
    private val masterData: MasterDataRepository,
    private val sync: WearConnectionSync,
    private val phone: PhoneLink,
) : ViewModel() {

    private val selectedListId = MutableStateFlow<Int?>(null)

    private val mutableIsRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = mutableIsRefreshing.asStateFlow()

    private val mutableLogin = MutableStateFlow(WearLoginForm())
    val login: StateFlow<WearLoginForm> = mutableLogin.asStateFlow()

    private val shoppingState = combine(
        shopping.lists,
        shopping.items,
        shopping.syncStates,
        combine(masterData.products, masterData.quantityUnits, ::Pair),
        selectedListId,
    ) { lists, items, syncStates, (products, units), selected ->
        val listId = lists.firstOrNull { it.id == selected }?.id ?: lists.firstOrNull()?.id
        WearShoppingState(
            lists = lists,
            listId = listId,
            items = wearShoppingItems(items, listId, products, units, syncStates),
        )
    }

    val state: StateFlow<WearState> = combine(auth.authState, shoppingState) { auth, shopping ->
        when (auth) {
            AuthState.LoggedOut -> WearState.LoggedOut
            is AuthState.LoggedIn -> WearState.LoggedIn(shopping)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WearState.Loading)

    init {
        // A new login, from the phone too, needs its data; a check off offline does not wait
        viewModelScope.launch {
            auth.authState.map { it is AuthState.LoggedIn }.distinctUntilChanged().collect {
                if (it) refresh() else askPhone()
            }
        }
    }

    /** The app comes to the front: the list may have changed, or the phone may have a login. */
    fun onStart() {
        viewModelScope.launch {
            if (auth.authState.first() is AuthState.LoggedIn) refresh() else askPhone()
        }
    }

    private suspend fun askPhone() {
        val published = phone.publishedConnection()
        if (published != null) sync.onPhoneConnection(published) else phone.requestConnection()
    }

    /** Sends what is queued and downloads the lists; offline the stored ones stay. */
    fun refreshNow() {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() {
        if (mutableIsRefreshing.value) return
        mutableIsRefreshing.value = true
        try {
            shopping.refresh()
            val products = masterData.products.first().map { it.id }.toSet()
            val units = masterData.quantityUnits.first().map { it.id }.toSet()
            // The products are the biggest download, only when something is missing
            val isMissing = shopping.items.first().any { item ->
                (item.productId != null && item.productId !in products) ||
                    (item.quId != null && item.quId !in units)
            }
            if (isMissing) masterData.refreshProductsAndUnits()
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // Offline the stored list goes on working, the rows show what is not sent yet
        } finally {
            mutableIsRefreshing.value = false
        }
    }

    fun selectList(listId: Int) {
        selectedListId.value = listId
    }

    fun setDone(itemId: Int, done: Boolean) {
        viewModelScope.launch { shopping.setDone(itemId, done) }
    }

    fun setServerUrl(serverUrl: String) {
        mutableLogin.value = mutableLogin.value.copy(serverUrl = serverUrl, error = null)
    }

    fun setApiKey(apiKey: String) {
        mutableLogin.value = mutableLogin.value.copy(apiKey = apiKey.trim(), error = null)
    }

    /** Connects with what was entered on the phone's keyboard. */
    fun connect() {
        val form = mutableLogin.value
        val url = ServerUrl.normalize(form.serverUrl)
        if (url == null) {
            mutableLogin.value = form.copy(error = WearLoginError.INVALID_ADDRESS)
            return
        }
        connect(ServerConnection.ApiKey(url, form.apiKey))
    }

    fun connectToDemo() = connect(ServerConnection.Demo())

    private fun connect(connection: ServerConnection) {
        if (mutableLogin.value.isConnecting) return
        mutableLogin.value = mutableLogin.value.copy(isConnecting = true, error = null)
        viewModelScope.launch {
            mutableLogin.value = try {
                auth.verify(connection)
                sync.onManualLogin(connection)
                WearLoginForm()
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException) {
                mutableLogin.value.copy(isConnecting = false, error = WearLoginError.FAILED)
            }
        }
    }
}

/** The rows of [listId]. */
internal fun wearShoppingItems(
    items: List<ShoppingListItem>,
    listId: Int?,
    products: List<Product>,
    units: List<QuantityUnit>,
    syncStates: Map<Int, ShoppingListItemSync>,
): List<WearShoppingItem> = items
    .filter { it.listId == listId }
    .map { item ->
        val productName = products.firstOrNull { it.id == item.productId }?.name
        val note = item.note?.trim()?.ifEmpty { null }
        val amount = amountText(item, units).takeIf { item.productId != null }
        WearShoppingItem(
            id = item.id,
            title = productName ?: note.orEmpty(),
            details = listOfNotNull(amount, note.takeIf { productName != null })
                .joinToString(" · ")
                .ifEmpty { null },
            done = item.done,
            sync = syncStates[item.id],
        )
    }
    .sortedWith(compareBy({ it.done }, { it.title.lowercase() }))

private fun amountText(item: ShoppingListItem, units: List<QuantityUnit>): String {
    val unit = units.firstOrNull { it.id == item.quId }
    val name = if (item.amount == 1.0) unit?.name else unit?.namePlural ?: unit?.name
    return listOfNotNull(formatDecimal(item.amount, AMOUNT_DECIMALS), name).joinToString(" ")
}

private const val AMOUNT_DECIMALS = 2
