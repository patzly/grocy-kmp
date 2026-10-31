package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The shopping mode: the shown shopping list for the store, items checked off with a tap
 * (offline too, sent at once where possible) and, with auto sync, downloaded again every few
 * seconds while the screen is shown ([syncWhileShown]), so several people can shop together.
 * List, status filter and table options are the shopping list's own.
 */
internal class ShoppingModeViewModel(
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val shoppingLists: ShoppingListRepository,
    private val optionsRepository: ShoppingListOptionsRepository,
    private val settingsRepository: ShoppingModeSettingsRepository,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    decimalSeparator: Char = platformDecimalSeparator(),
    /** The app language, whose rules name the units of amounts. */
    language: String,
) : ViewModel() {

    /** Read at app start, so the screen knows them in its first frame. */
    val settings: StateFlow<ShoppingModeSettings?> = settingsRepository.settings

    private val refreshController = RefreshController(viewModelScope) {
        refreshShoppingList(masterData, stock, shoppingLists)
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    val state: StateFlow<ShoppingListUiState?> = shoppingListState(
        masterData = masterData,
        stock = stock,
        shoppingLists = shoppingLists,
        optionsRepository = optionsRepository,
        computeDispatcher = computeDispatcher,
        decimalSeparator = decimalSeparator,
        language = language,
        doneGroup = settings.filterNotNull().map { it.doneItemsLast }.distinctUntilChanged(),
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    /**
     * Downloads the items every interval while auto sync is on, until cancelled: the screen
     * runs it while it is visible. Failures stay quiet, the next interval tries again and the
     * items show their check offs still to send.
     */
    suspend fun syncWhileShown() {
        settings.filterNotNull()
            .map { if (it.autoSync) it.syncIntervalSeconds else null }
            .distinctUntilChanged()
            .collectLatest { interval ->
                if (interval == null) return@collectLatest
                while (true) {
                    delay(interval.seconds)
                    try {
                        shoppingLists.refreshItemsOnly()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: GrocyApiException) {
                        // Offline or the server is down, the next interval tries again
                    }
                }
            }
    }

    fun setAutoSync(autoSync: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(autoSync = autoSync) } }
    }

    /** Checks the item off or on again, offline too. */
    fun toggleDone(row: ShoppingListRow) {
        viewModelScope.launch { shoppingLists.setDone(row.id, !row.done) }
    }

    fun selectList(listId: Int) {
        shoppingLists.selectList(listId)
    }

    fun setStatus(status: ShoppingListStatus?) {
        optionsRepository.setStatus(status)
    }

    /** Like Grocy's "Clear filter": the status, not the table options. */
    fun clearFilter() {
        optionsRepository.setStatus(null)
    }

    fun setGrouping(grouping: ShoppingListGrouping) {
        viewModelScope.launch { optionsRepository.update { it.copy(grouping = grouping) } }
    }

    fun setFieldShown(field: ShoppingListField, shown: Boolean) {
        viewModelScope.launch {
            optionsRepository.update { options ->
                options.copy(
                    fields = if (shown) options.fields + field else options.fields - field,
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
