package com.patrickzedler.grocy.feature.shoppinglist.impl

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.forms.htmlToPlainText
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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
    /** Whether the server has not taken the list's notes yet, null once it has. */
    val notesSync: ShoppingListItemSync? = null,
    /** The items whose check off the server has not taken yet, by id. */
    val syncStates: Map<Int, ShoppingListItemSync>,
    /** False until a download stored any list. */
    val hasData: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()

    /** Whether the user can choose another list, Grocy's list selector. */
    val canChooseList: Boolean
        get() = config.shoppingListMultipleLists && list != null

    /** Product items shown right now, which "Add all list items to stock" books like Grocy. */
    val purchases: List<ShoppingListPurchase>
        get() = groups.flatMap { group -> group.rows.mapNotNull { it.purchase } }
}

/**
 * The shown list as the shopping list and the shopping mode show it, from the database: the
 * chosen list, Grocy's table options and the status filter they share, searched by [query].
 * [doneLast] moves done items to the end of their group.
 */
internal fun shoppingListState(
    masterData: MasterDataRepository,
    stock: StockRepository,
    shoppingLists: ShoppingListRepository,
    optionsRepository: ShoppingListOptionsRepository,
    computeDispatcher: CoroutineDispatcher,
    decimalSeparator: Char,
    language: String,
    query: Flow<String> = flowOf(""),
    doneLast: Flow<Boolean> = flowOf(false),
): Flow<ShoppingListUiState> {
    val masterDataPart = combine(
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
    val data = combine(
        masterDataPart,
        shoppingLists.items,
        stock.productStockInfo,
    ) { part: ShoppingListData, items: List<ShoppingListItem>, info: List<ProductStockInfo> ->
        part.copy(items = items, info = info)
    }
    val listsPart = combine(
        shoppingLists.lists,
        shoppingLists.selectedListId,
        shoppingLists.syncStates,
        shoppingLists.notesSyncStates,
    ) { lists, selectedId, sync, notesSync -> ListsPart(lists, selectedId, sync, notesSync) }
    val options = combine(
        masterData.serverConfig,
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        optionsRepository.status,
        combine(query, doneLast, ::ListView),
    ) { config, settings, options, status, view ->
        ListOptions(config, settings, options, status, view)
    }
    return combine(data, listsPart, options) { data, lists, options ->
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
            query = options.view.query,
            grouping = grouping,
            config = config,
            settings = options.settings,
            decimalSeparator = decimalSeparator,
            language = language,
            doneLast = options.view.doneLast,
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
            notesSync = list?.let { lists.notesSyncStates[it.id] },
            syncStates = lists.syncStates,
            hasData = lists.lists.isNotEmpty() || data.items.isNotEmpty(),
        )
    }.flowOn(computeDispatcher)
}

/**
 * Downloads what the shopping list shows: master data, the lists and their items and what the
 * stock adds (below min. stock amount, last prices), in parallel.
 */
internal suspend fun refreshShoppingList(
    masterData: MasterDataRepository,
    stock: StockRepository,
    shoppingLists: ShoppingListRepository,
) {
    val config = masterData.serverConfig.first()
    coroutineScope {
        launch { masterData.refresh() }
        launch { shoppingLists.refresh() }
        // Below min. stock amount and the last prices come with the stock overview
        if (config.stock) launch { stock.refreshOverview(withShoppingLists = true) }
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

private data class ListsPart(
    val lists: List<ShoppingList>,
    val selectedId: Int?,
    val syncStates: Map<Int, ShoppingListItemSync>,
    val notesSyncStates: Map<Int, ShoppingListItemSync>,
)

private data class ListView(val query: String, val doneLast: Boolean)

private data class ListOptions(
    val config: ServerConfig,
    val settings: UserSettings,
    val options: ShoppingListOptions,
    val status: ShoppingListStatus?,
    val view: ListView,
)
