package com.patrickzedler.grocy.core.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.auth.AuthState
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.network.ServerUrl
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
 * The watch's screen state: the login and one shopping list to show and check off. Offline first
 * like the phone: check offs are stored and queued, then replayed per item against the server's
 * current state ([WearShoppingList]).
 */
class WearViewModel(
    private val auth: AuthRepository,
    private val shopping: WearShoppingList,
    private val sync: WearConnectionSync,
    private val phone: PhoneLink,
) : ViewModel() {

    private val mutableIsRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = mutableIsRefreshing.asStateFlow()

    private val mutableLogin = MutableStateFlow(WearLoginForm())
    val login: StateFlow<WearLoginForm> = mutableLogin.asStateFlow()

    val state: StateFlow<WearState> = combine(auth.authState, shopping.state) { auth, shopping ->
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // Offline the stored list goes on working, the rows show what is not sent yet
        } finally {
            mutableIsRefreshing.value = false
        }
    }

    /**
     * Downloads the items every interval while auto sync is on, until cancelled: the screen runs
     * it while it is visible, so what others check off shows up by itself. Failures stay quiet,
     * the next interval tries again.
     */
    suspend fun syncWhileShown() {
        shopping.syncIntervalSeconds.collectLatest { interval ->
            if (interval == null) return@collectLatest
            while (true) {
                delay(interval.seconds)
                syncOnce()
            }
        }
    }

    private suspend fun syncOnce() {
        if (auth.authState.first() !is AuthState.LoggedIn) return
        try {
            shopping.syncItems()
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // Offline or the server is down, the rows show what is not sent yet
        }
    }

    fun selectList(listId: Int) = shopping.selectList(listId)

    fun setDone(itemId: Int, done: Boolean) {
        viewModelScope.launch { shopping.setDone(itemId, done) }
    }

    fun setStatus(status: ShoppingListStatus?) = shopping.setStatus(status)

    fun setGrouping(grouping: ShoppingListGrouping) {
        viewModelScope.launch { shopping.setGrouping(grouping) }
    }

    fun setAutoSync(autoSync: Boolean) {
        viewModelScope.launch { shopping.setAutoSync(autoSync) }
    }

    fun setDoneItemsLast(doneItemsLast: Boolean) {
        viewModelScope.launch { shopping.setDoneItemsLast(doneItemsLast) }
    }

    fun setSyncInterval(seconds: Int) {
        viewModelScope.launch { shopping.setSyncInterval(seconds) }
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
