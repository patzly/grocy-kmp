package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.feature.masterdata.impl.product.htmlToPlainText
import com.patrickzedler.grocy.feature.masterdata.impl.product.plainTextToHtml
import com.patrickzedler.grocy.feature.masterdata.impl.related.ChangeError
import com.patrickzedler.grocy.feature.masterdata.impl.related.changeErrorOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A list of the list selector: Grocy's "Name (count)". */
internal data class ShoppingListChoice(val id: Int, val name: String, val itemCount: Int)

/** What the shopping list shows; built from the database, never waits for the server. */
internal data class ShoppingListUiState(
    /** Every list by name, the selector's choices; one without multiple lists. */
    val lists: List<ShoppingListChoice>,
    /** The shown list, null until the lists were downloaded. */
    val list: ShoppingList?,
    val groups: List<ShoppingListGroup>,
    val statusCounts: Map<ShoppingListStatus, Int>,
    val itemCount: Int,
    /** Null without price tracking. */
    val totalValue: Double?,
    val currency: String,
    val priceDecimals: Int,
    val status: ShoppingListStatus?,
    /** The shown fields, in Grocy's order. */
    val fields: List<ShoppingListField>,
    val availableFields: List<ShoppingListField>,
    val grouping: ShoppingListGrouping,
    val availableGroupings: List<ShoppingListGrouping>,
    val availableStatuses: List<ShoppingListStatus>,
    val config: ServerConfig,
    val settings: UserSettings,
    /** The list's notes as plain text. */
    val notes: String,
    /** Check offs the server has not taken yet. */
    val pendingCount: Int,
    /** False until a download stored any list. */
    val hasData: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()

    /** Product items shown right now, which "Add all list items to stock" books like Grocy. */
    val purchases: List<ShoppingListPurchase>
        get() = groups.flatMap { group -> group.rows.mapNotNull { it.purchase } }
}

/** What failed, for Grocy's messages. */
internal enum class ShoppingListAction {
    DELETE_ITEM,
    CLEAR,
    ADD_PRODUCTS,
    DELETE_LIST,
}

internal sealed interface ShoppingListEvent {
    /** Opens Grocy's purchase of [purchase], whose booking removes the item. */
    data class OpenPurchase(val purchase: ShoppingListPurchase) : ShoppingListEvent

    data class Failed(val action: ShoppingListAction, val error: ChangeError) : ShoppingListEvent
}

/** The questions and forms the shopping list opens over itself. */
internal sealed interface ShoppingListDialog {
    /** "Are you sure you want to empty shopping list …?", [name] null without several lists. */
    data class ClearList(val listId: Int, val name: String?) : ShoppingListDialog

    data class DeleteList(val listId: Int, val name: String) : ShoppingListDialog

    /** A purchase of "Add all list items to stock" was left without booking. */
    data class StockItemSkipped(val position: Int, val count: Int, val name: String) :
        ShoppingListDialog
}

/** Grocy's "Notes" of the shown list, edited as plain text. */
@Stable
internal class NotesEditor(val listId: Int, text: String) {
    val text = TextFieldState(text)
    var isSaving by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)
}

/** Grocy's print options of the thermal printer. */
@Stable
internal class PrintOptions(val listId: Int, showHeader: Boolean) {
    var showHeader by mutableStateOf(showHeader)
    var isPrinting by mutableStateOf(false)
    var error by mutableStateOf<ChangeError?>(null)
}

/**
 * Grocy's shopping list page: shows the stored list at once and downloads the master data,
 * what the stock adds (below min. stock amount, last prices) and the lists in the background.
 * Checking items off works offline ([ShoppingListRepository.setDone]), everything else is
 * online only. Fields and grouping are kept like Grocy's table options, the status filter and
 * the search only here.
 *
 * @param listId the list to show (Grocy's `list`), the last shown or first one without.
 */
@OptIn(FlowPreview::class)
internal class ShoppingListViewModel(
    listId: Int?,
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val shoppingLists: ShoppingListRepository,
    private val optionsRepository: ShoppingListOptionsRepository,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    decimalSeparator: Char = platformDecimalSeparator(),
    /** The app language, whose rules name the units of amounts. */
    language: String,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    var dialog by mutableStateOf<ShoppingListDialog?>(null)
        private set

    var notes by mutableStateOf<NotesEditor?>(null)
        private set

    var print by mutableStateOf<PrintOptions?>(null)
        private set

    /** True while a change of the whole list runs, so it is not started twice. */
    var isChanging by mutableStateOf(false)
        private set

    private val status = MutableStateFlow<ShoppingListStatus?>(null)

    private val eventChannel = Channel<ShoppingListEvent>(Channel.BUFFERED)
    val events: Flow<ShoppingListEvent> = eventChannel.receiveAsFlow()

    /** "Add all list items to stock": the items still to book and where it is. */
    private var stockWorkflow: StockWorkflow? = null

    private val refreshController = RefreshController(viewModelScope) {
        val config = masterData.serverConfig.first()
        coroutineScope {
            launch { masterData.refresh() }
            launch { shoppingLists.refresh() }
            // Below min. stock amount and the last prices come with the stock overview
            if (config.stock) launch { stock.refreshOverview(withShoppingLists = true) }
        }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val masterDataPart = combine(
        masterData.products,
        masterData.productGroups,
        masterData.quantityUnits,
        masterData.stores,
        masterData.productBarcodes,
        masterData.resolvedQuantityUnitConversions,
        masterData.allUserfields,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ShoppingListData(
            products = values[0] as List<Product>,
            productGroups = values[1] as List<ProductGroup>,
            quantityUnits = values[2] as List<QuantityUnit>,
            stores = values[3] as List<Store>,
            barcodes = values[4] as List<ProductBarcode>,
            resolvedConversions = values[5] as List<ResolvedQuantityUnitConversion>,
            userfields = values[6] as List<Userfield>,
        )
    }

    private val data = combine(
        masterDataPart,
        shoppingLists.items,
        stock.productStockInfo,
    ) { part: ShoppingListData, items: List<ShoppingListItem>, info: List<ProductStockInfo> ->
        part.copy(items = items, info = info)
    }

    // Typing stays smooth with long lists: the list follows after a short pause
    private val query = snapshotFlow { searchState.text.toString() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DELAY_MILLIS }

    private val listsPart = combine(
        shoppingLists.lists,
        shoppingLists.selectedListId,
        shoppingLists.pendingCount,
    ) { lists, selectedId, pending -> ListsPart(lists, selectedId, pending) }

    private val options = combine(
        masterData.serverConfig,
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        status,
        query,
    ) { config, settings, options, status, query ->
        ListOptions(config, settings, options, status, query)
    }

    val state: StateFlow<ShoppingListUiState?> =
        combine(data, listsPart, options) { data, lists, options ->
            val config = options.config
            val list = shownList(lists.lists, lists.selectedId, config)
            val shownId = list?.id ?: ShoppingList.DEFAULT_ID
            val grouping = options.options.grouping
                .takeIf { it in availableShoppingListGroupings(config) }
                ?: ShoppingListGrouping.NONE
            val content = buildShoppingList(
                data = data,
                listId = shownId,
                status = options.status,
                query = options.query,
                grouping = grouping,
                config = config,
                settings = options.settings,
                decimalSeparator = decimalSeparator,
                language = language,
            )
            val available = availableShoppingListFields(config)
            val counts = data.items.groupingBy { it.listId }.eachCount()
            ShoppingListUiState(
                lists = lists.lists
                    .filter { config.shoppingListMultipleLists || it.id == list?.id }
                    .map { ShoppingListChoice(it.id, it.name, counts[it.id] ?: 0) },
                list = list,
                groups = content.groups,
                statusCounts = content.statusCounts,
                itemCount = content.itemCount,
                totalValue = content.totalValue.takeIf { config.stockPriceTracking },
                currency = config.currency,
                priceDecimals = options.settings.stockDecimalPlacesPricesDisplay,
                status = options.status,
                fields = available.filter { it in options.options.fields },
                availableFields = available,
                grouping = grouping,
                availableGroupings = availableShoppingListGroupings(config),
                availableStatuses = availableShoppingListStatuses(config),
                config = config,
                settings = options.settings,
                notes = list?.description?.let(::htmlToPlainText).orEmpty(),
                pendingCount = lists.pendingCount,
                hasData = lists.lists.isNotEmpty() || data.items.isNotEmpty(),
            )
        }
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        listId?.let(shoppingLists::selectList)
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun selectList(listId: Int) {
        shoppingLists.selectList(listId)
    }

    fun setStatus(status: ShoppingListStatus?) {
        this.status.value = status
    }

    /** Like Grocy's "Clear filter": the status and the search, not the table options. */
    fun clearFilter() {
        status.value = null
        searchState.clearText()
        isSearchVisible = false
    }

    fun setGrouping(grouping: ShoppingListGrouping) {
        updateOptions { it.copy(grouping = grouping) }
    }

    fun setFieldShown(field: ShoppingListField, shown: Boolean) {
        updateOptions { options ->
            options.copy(fields = if (shown) options.fields + field else options.fields - field)
        }
    }

    /** Grocy's done button: checks the item off or on again, offline too. */
    fun toggleDone(row: ShoppingListRow) {
        viewModelScope.launch { shoppingLists.setDone(row.id, !row.done) }
    }

    /** Grocy's delete button, without question like Grocy. */
    fun delete(row: ShoppingListRow) {
        viewModelScope.launch {
            val error = changeErrorOf { shoppingLists.deleteItem(row.id) } ?: return@launch
            eventChannel.send(ShoppingListEvent.Failed(ShoppingListAction.DELETE_ITEM, error))
        }
    }

    /** Grocy's "Clear list" asks first, naming the list where there are several. */
    fun requestClear() {
        val current = state.value ?: return
        val list = current.list ?: return
        dialog = ShoppingListDialog.ClearList(
            listId = list.id,
            name = list.name.takeIf { current.config.shoppingListMultipleLists },
        )
    }

    fun clear(listId: Int) = change(ShoppingListAction.CLEAR) {
        shoppingLists.clear(listId, doneOnly = false)
    }

    /** Grocy's "Clear done items", without question. */
    fun clearDone() {
        val listId = state.value?.list?.id ?: return
        change(ShoppingListAction.CLEAR) { shoppingLists.clear(listId, doneOnly = true) }
    }

    fun addMissingProducts() {
        val listId = state.value?.list?.id ?: return
        change(ShoppingListAction.ADD_PRODUCTS) { shoppingLists.addMissingProducts(listId) }
    }

    fun addOverdueAndExpiredProducts() {
        val listId = state.value?.list?.id ?: return
        change(ShoppingListAction.ADD_PRODUCTS) {
            shoppingLists.addOverdueAndExpiredProducts(listId)
        }
    }

    /** Grocy's "Delete shopping list" asks first; its first list cannot be deleted. */
    fun requestDeleteList() {
        val list = state.value?.list?.takeIf { it.id != ShoppingList.DEFAULT_ID } ?: return
        dialog = ShoppingListDialog.DeleteList(list.id, list.name)
    }

    /** Deletes the list and shows the first one, like Grocy. */
    fun deleteList(listId: Int) = change(ShoppingListAction.DELETE_LIST) {
        shoppingLists.deleteList(listId)
        shoppingLists.selectList(ShoppingList.DEFAULT_ID)
    }

    fun dismissDialog() {
        dialog = null
    }

    fun editNotes() {
        val current = state.value ?: return
        val list = current.list ?: return
        notes = NotesEditor(list.id, current.notes)
    }

    /** Saves the notes, Grocy's "Clear" saves them empty. Online only. */
    fun saveNotes(clear: Boolean = false) {
        val editor = notes ?: return
        if (editor.isSaving) return
        if (clear) editor.text.clearText()
        editor.isSaving = true
        editor.error = null
        viewModelScope.launch {
            val html = plainTextToHtml(editor.text.text.toString())
            val error = changeErrorOf { shoppingLists.saveNotes(editor.listId, html) }
            editor.isSaving = false
            if (error == null) {
                if (notes === editor) notes = null
            } else {
                editor.error = error
            }
        }
    }

    fun dismissNotes() {
        if (notes?.isSaving != true) notes = null
    }

    fun openPrint() {
        val current = state.value ?: return
        val list = current.list ?: return
        print = PrintOptions(list.id, current.settings.shoppingListPrintShowHeader)
    }

    /** Grocy's option is a user setting, saved when changed; the dialog keeps it anyway. */
    fun setPrintHeader(showHeader: Boolean) {
        val options = print ?: return
        options.showHeader = showHeader
        viewModelScope.launch {
            changeErrorOf {
                masterData.saveUserSetting(
                    PRINT_SHOW_HEADER,
                    UserSettingValue.Flag(showHeader),
                )
            }
        }
    }

    /** Grocy's "Thermal printer": the server prints the list. */
    fun printThermal() {
        val options = print ?: return
        if (options.isPrinting) return
        options.isPrinting = true
        options.error = null
        viewModelScope.launch {
            val error = changeErrorOf { shoppingLists.print(options.listId, options.showHeader) }
            options.isPrinting = false
            if (error == null) {
                if (print === options) print = null
            } else {
                options.error = error
            }
        }
    }

    fun dismissPrint() {
        if (print?.isPrinting != true) print = null
    }

    /** Grocy's "Add this item to stock": its purchase, whose booking removes the item. */
    fun addToStock(row: ShoppingListRow) {
        val purchase = row.purchase ?: return
        stockWorkflow = null
        viewModelScope.launch { eventChannel.send(ShoppingListEvent.OpenPurchase(purchase)) }
    }

    /**
     * Grocy's "Add all list items to stock": the purchase of every product item shown, one
     * after the other. The next opens once the previous was booked (its item is gone).
     */
    fun addAllToStock() {
        val current = state.value ?: return
        val purchases = current.purchases
        if (purchases.isEmpty()) return
        val names = current.groups.flatMap { it.rows }.associate { it.id to it.title }
        stockWorkflow = StockWorkflow(purchases, names)
        openStockItem()
    }

    /** The list is shown again, e.g. back from a purchase of "Add all list items to stock". */
    fun onShown() {
        val workflow = stockWorkflow?.takeIf { it.isAwaitingPurchase } ?: return
        workflow.isAwaitingPurchase = false
        viewModelScope.launch {
            val item = workflow.current
            val wasBooked = shoppingLists.items.first().none { it.id == item.itemId }
            if (wasBooked) {
                nextStockItem()
            } else {
                // Left without booking: skip it like Grocy's "Skip", or stop
                dialog = ShoppingListDialog.StockItemSkipped(
                    position = workflow.index + 1,
                    count = workflow.purchases.size,
                    name = workflow.names[item.itemId].orEmpty(),
                )
            }
        }
    }

    /** Grocy's "Skip": the item stays on the list, the next one's purchase opens. */
    fun skipStockItem() {
        dialog = null
        nextStockItem()
    }

    /** Stops "Add all list items to stock", like closing Grocy's dialog. */
    fun stopStockWorkflow() {
        dialog = null
        stockWorkflow = null
    }

    private fun nextStockItem() {
        val workflow = stockWorkflow ?: return
        workflow.index++
        if (workflow.index < workflow.purchases.size) openStockItem() else stockWorkflow = null
    }

    private fun openStockItem() {
        val workflow = stockWorkflow ?: return
        workflow.isAwaitingPurchase = true
        viewModelScope.launch {
            eventChannel.send(ShoppingListEvent.OpenPurchase(workflow.current))
        }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun change(action: ShoppingListAction, block: suspend () -> Unit) {
        if (isChanging) return
        dialog = null
        isChanging = true
        viewModelScope.launch {
            val error = changeErrorOf(block)
            isChanging = false
            error?.let { eventChannel.send(ShoppingListEvent.Failed(action, it)) }
        }
    }

    private fun updateOptions(transform: (ShoppingListOptions) -> ShoppingListOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private class StockWorkflow(
        val purchases: List<ShoppingListPurchase>,
        val names: Map<Int, String>,
    ) {
        var index = 0
        var isAwaitingPurchase = false

        val current: ShoppingListPurchase
            get() = purchases[index]
    }

    private data class ListsPart(
        val lists: List<ShoppingList>,
        val selectedId: Int?,
        val pendingCount: Int,
    )

    private data class ListOptions(
        val config: ServerConfig,
        val settings: UserSettings,
        val options: ShoppingListOptions,
        val status: ShoppingListStatus?,
        val query: String,
    )

    private companion object {
        const val SEARCH_DELAY_MILLIS = 150L
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val PRINT_SHOW_HEADER = "shopping_list_print_show_header"
    }
}

/**
 * The list the page shows: the chosen one while it exists, otherwise Grocy's first list (or
 * the first by name); always the first one without multiple lists.
 */
internal fun shownList(
    lists: List<ShoppingList>,
    selectedId: Int?,
    config: ServerConfig,
): ShoppingList? {
    val first = lists.firstOrNull { it.id == ShoppingList.DEFAULT_ID } ?: lists.firstOrNull()
    if (!config.shoppingListMultipleLists) return first
    return lists.firstOrNull { it.id == selectedId } ?: first
}
