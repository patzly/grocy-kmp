package com.patrickzedler.grocy.core.wear

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingListOptionsRepository
import com.patrickzedler.grocy.core.data.settings.ShoppingModeSettingsRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingItemAmounts
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.model.text.normalizedForSearch
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One row of the watch's list. */
data class WearShoppingItem(
    val id: Int,
    val title: String,
    /** Amount, unit and note, null if there is nothing to add to the title. */
    val details: String?,
    val done: Boolean,
    val sync: ShoppingListItemSync?,
)

/** What a group of rows is named by. */
sealed interface WearShoppingGroupTitle {
    data class Text(val text: String) : WearShoppingGroupTitle

    /** Items without product group. */
    data object Ungrouped : WearShoppingGroupTitle

    /** Items without the default store they are grouped by. */
    data object NotSet : WearShoppingGroupTitle

    /** The done items, last of all. */
    data object Done : WearShoppingGroupTitle
}

/** Rows below a heading; without [title] the list has no groups. */
data class WearShoppingGroup(
    val title: WearShoppingGroupTitle?,
    val items: List<WearShoppingItem>,
)

/** The list the watch shows and how the user set it up. */
data class WearShoppingState(
    val lists: List<ShoppingList> = emptyList(),
    val listId: Int? = null,
    val groups: List<WearShoppingGroup> = emptyList(),
    /** Of the whole list, whatever the status filter shows. */
    val doneCount: Int = 0,
    val undoneCount: Int = 0,
    /** Null for all items. */
    val status: ShoppingListStatus? = null,
    val grouping: ShoppingListGrouping = ShoppingListGrouping.NONE,
    val availableGroupings: List<ShoppingListGrouping> = emptyList(),
    val settings: ShoppingModeSettings = ShoppingModeSettings(),
)

/** What the tile says about the list. */
data class WearTileSummary(
    val listName: String,
    val total: Int,
    val left: Int,
    /** The first items that are not done, in the list's order. */
    val nextItems: List<String>,
)

/** Everything a shown list is built from, as the database has it. */
internal data class WearShoppingData(
    val lists: List<ShoppingList> = emptyList(),
    val selectedListId: Int? = null,
    val items: List<ShoppingListItem> = emptyList(),
    val syncStates: Map<Int, ShoppingListItemSync> = emptyMap(),
    val names: WearShoppingNames = WearShoppingNames(),
    val config: ServerConfig = ServerConfig(),
    val userSettings: UserSettings = UserSettings(),
)

/** The master data that names the items of a list. */
internal data class WearShoppingNames(
    val products: List<Product> = emptyList(),
    val productGroups: List<ProductGroup> = emptyList(),
    val quantityUnits: List<QuantityUnit> = emptyList(),
    val stores: List<Store> = emptyList(),
    val conversions: List<ResolvedQuantityUnitConversion> = emptyList(),
)

/**
 * The shopping list of the watch, for its app and its tile: the phone's shopping mode on the
 * small screen, with its groups and options, kept on the watch. Reads from the database, so it
 * works offline; [refresh] and [syncItems] download what changed.
 */
class WearShoppingList(
    private val shopping: ShoppingListRepository,
    private val masterData: MasterDataRepository,
    private val options: ShoppingListOptionsRepository,
    private val modeSettings: ShoppingModeSettingsRepository,
    /** The app language, whose rules name the units of amounts. */
    private val language: String,
    private val decimalSeparator: Char = platformDecimalSeparator(),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val names = combine(
        masterData.products,
        masterData.productGroups,
        masterData.quantityUnits,
        masterData.stores,
        masterData.resolvedQuantityUnitConversions,
        ::WearShoppingNames,
    )

    private val listData = combine(
        shopping.lists,
        shopping.selectedListId,
        shopping.items,
        shopping.syncStates,
        names,
    ) { lists, selectedId, items, syncStates, names ->
        WearShoppingData(lists, selectedId, items, syncStates, names)
    }

    private val data = combine(
        listData,
        masterData.serverConfig,
        masterData.userSettings,
    ) { data, config, userSettings -> data.copy(config = config, userSettings = userSettings) }

    private val view = combine(
        options.options.filterNotNull(),
        modeSettings.settings.filterNotNull(),
        ::Pair,
    )

    private fun state(status: Flow<ShoppingListStatus?>): Flow<WearShoppingState> =
        combine(data, view, status) { data, (options, settings), shownStatus ->
            wearShoppingState(data, options, settings, shownStatus, language, decimalSeparator)
        }

    /** What the app shows. */
    val state: Flow<WearShoppingState> = state(options.status)

    /** What the tile shows: the whole list, the app's status filter is only for looking. */
    val tileSummary: Flow<WearTileSummary> =
        state(flowOf(null)).map(::wearTileSummary).distinctUntilChanged()

    /** The seconds between downloads while the app is shown, null with auto sync off. */
    val syncIntervalSeconds: Flow<Int?> = modeSettings.settings.filterNotNull()
        .map { if (it.autoSync) it.syncIntervalSeconds else null }
        .distinctUntilChanged()

    private val namesMutex = Mutex()

    // Master data changes rarely: once per process, and when an item names something new
    private var namesDownloaded = false
    private var checkedReferences: Set<Pair<Int?, Int?>>? = null

    private var lastDownload: TimeMark? = null

    /**
     * Sends what is queued and downloads the lists.
     *
     * @throws GrocyApiException e.g. when offline; the stored list stays.
     */
    suspend fun refresh() {
        lastDownload = timeSource.markNow()
        shopping.refresh()
        refreshNames()
    }

    /**
     * Downloads only the items, for the regular sync.
     *
     * @throws GrocyApiException e.g. when offline; the stored list stays.
     */
    suspend fun syncItems() {
        lastDownload = timeSource.markNow()
        shopping.refreshItemsOnly()
        refreshNames()
    }

    /** Keeps the tile current without costing battery: at most one download a minute. */
    suspend fun refreshForTile(minimumAge: Duration = TILE_REFRESH_INTERVAL) {
        val last = lastDownload
        if (last != null && last.elapsedNow() < minimumAge) return
        try {
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // Offline the tile shows the stored list
        }
    }

    private suspend fun refreshNames() = namesMutex.withLock {
        val items = shopping.items.first()
        val references = items.map { it.productId to it.quId }.toSet()
        // Reading all products every few seconds would cost more than the sync itself
        if (namesDownloaded && references == checkedReferences) return@withLock
        if (!namesDownloaded || isAnyNameMissing(items)) {
            masterData.refreshShoppingListNames()
            namesDownloaded = true
        }
        checkedReferences = references
    }

    private suspend fun isAnyNameMissing(items: List<ShoppingListItem>): Boolean {
        val products = masterData.products.first().associateBy { it.id }
        val units = masterData.quantityUnits.first().map { it.id }.toSet()
        val groups = masterData.productGroups.first().map { it.id }.toSet()
        val stores = masterData.stores.first().map { it.id }.toSet()
        return items.any { item ->
            val product = item.productId?.let { products[it] }
            (item.productId != null && product == null) ||
                (item.quId != null && item.quId !in units) ||
                product?.productGroupId?.let { it !in groups } == true ||
                product?.storeId?.let { it !in stores } == true
        }
    }

    fun selectList(listId: Int) = shopping.selectList(listId)

    suspend fun setDone(itemId: Int, done: Boolean) = shopping.setDone(itemId, done)

    fun setStatus(status: ShoppingListStatus?) = options.setStatus(status)

    suspend fun setGrouping(grouping: ShoppingListGrouping) {
        options.update { it.copy(grouping = grouping) }
    }

    suspend fun setAutoSync(autoSync: Boolean) {
        modeSettings.update { it.copy(autoSync = autoSync) }
    }

    suspend fun setDoneItemsLast(doneItemsLast: Boolean) {
        modeSettings.update { it.copy(doneItemsLast = doneItemsLast) }
    }

    suspend fun setSyncInterval(seconds: Int) {
        modeSettings.update { it.copy(syncIntervalSeconds = seconds) }
    }

    private companion object {
        val TILE_REFRESH_INTERVAL = 1.minutes
    }
}

/** The shown list in the groups the user chose, like the phone's shopping mode shows it. */
internal fun wearShoppingState(
    data: WearShoppingData,
    options: ShoppingListOptions,
    settings: ShoppingModeSettings,
    status: ShoppingListStatus?,
    language: String,
    decimalSeparator: Char,
): WearShoppingState {
    val list = shownList(data.lists, data.selectedListId, data.config)
    val builder = RowBuilder(data, language, decimalSeparator)
    val rows = data.items.filter { it.listId == list?.id }.map(builder::row)
        .sortedWith(compareBy({ it.item.title.normalizedForSearch() }, { it.item.id }))
    val available = ShoppingListGrouping.entries.filter {
        it != ShoppingListGrouping.DEFAULT_STORE || data.config.stockPriceTracking
    }
    val grouping = options.grouping.takeIf { it in available } ?: ShoppingListGrouping.NONE
    val shown = rows.filter { row ->
        when (status) {
            ShoppingListStatus.DONE -> row.item.done
            ShoppingListStatus.UNDONE -> !row.item.done
            // Below the min. stock amount would need the whole stock on the watch
            ShoppingListStatus.BELOW_MIN_STOCK, null -> true
        }
    }
    return WearShoppingState(
        lists = data.lists.filter {
            data.config.shoppingListMultipleLists || it.id == list?.id
        },
        listId = list?.id,
        groups = groups(shown, grouping, settings.doneItemsLast),
        doneCount = rows.count { it.item.done },
        undoneCount = rows.count { !it.item.done },
        status = status,
        grouping = grouping,
        availableGroupings = available,
        settings = settings,
    )
}

/** What the sync interval can be set to; an interval set otherwise stays selectable. */
fun wearSyncIntervals(current: Int): List<Int> =
    (SYNC_INTERVALS + current).distinct().sorted()

private val SYNC_INTERVALS = listOf(5, 10, 15, 30, 60)

/** The tile's numbers of a list shown with all its items. */
internal fun wearTileSummary(state: WearShoppingState): WearTileSummary = WearTileSummary(
    listName = state.lists.firstOrNull { it.id == state.listId }?.name.orEmpty(),
    total = state.doneCount + state.undoneCount,
    left = state.undoneCount,
    nextItems = state.groups.flatMap { it.items }
        .filter { !it.done && it.title.isNotBlank() }
        .take(TILE_ITEMS)
        .map { it.title },
)

private const val TILE_ITEMS = 3

/** The chosen list while it exists, otherwise Grocy's first one, like the phone. */
private fun shownList(
    lists: List<ShoppingList>,
    selectedId: Int?,
    config: ServerConfig,
): ShoppingList? {
    val first = lists.firstOrNull { it.id == ShoppingList.DEFAULT_ID } ?: lists.firstOrNull()
    if (!config.shoppingListMultipleLists) return first
    return lists.firstOrNull { it.id == selectedId } ?: first
}

private data class WearRow(val item: WearShoppingItem, val product: RowGroups)

private data class RowGroups(val productGroup: String?, val store: String?)

/** Builds the rows; what all of them need, looked up once. */
private class RowBuilder(
    private val data: WearShoppingData,
    language: String,
    decimalSeparator: Char,
) {
    private val products = data.names.products.associateBy { it.id }
    private val groups = data.names.productGroups.associateBy { it.id }
    private val stores = data.names.stores.associateBy { it.id }
    private val amounts = ShoppingItemAmounts(
        products = data.names.products,
        quantityUnits = data.names.quantityUnits,
        conversions = data.names.conversions,
        settings = data.userSettings,
        language = language,
        decimalSeparator = decimalSeparator,
    )

    fun row(item: ShoppingListItem): WearRow {
        val product = item.productId?.let { products[it] }
        val note = item.note?.trim()?.ifEmpty { null }
        val amount = amounts.of(item)
        return WearRow(
            item = WearShoppingItem(
                id = item.id,
                title = product?.name ?: note.orEmpty(),
                details = listOfNotNull(amount, note.takeIf { product != null })
                    .joinToString(" · ")
                    .ifEmpty { null },
                done = item.done,
                sync = data.syncStates[item.id],
            ),
            product = RowGroups(
                productGroup = product?.productGroupId?.let { groups[it]?.name },
                store = product?.storeId?.let { stores[it]?.name },
            ),
        )
    }
}

/** The rows in groups, with [doneLast] the done ones in a last group of their own. */
private fun groups(
    rows: List<WearRow>,
    grouping: ShoppingListGrouping,
    doneLast: Boolean,
): List<WearShoppingGroup> {
    if (!doneLast) return group(rows, grouping)
    val (done, undone) = rows.partition { it.item.done }
    if (done.isEmpty()) return group(undone, grouping)
    return group(undone, grouping) +
        WearShoppingGroup(WearShoppingGroupTitle.Done, done.map { it.item })
}

private fun group(rows: List<WearRow>, grouping: ShoppingListGrouping): List<WearShoppingGroup> {
    if (rows.isEmpty()) return emptyList()
    if (grouping == ShoppingListGrouping.NONE) {
        return listOf(WearShoppingGroup(null, rows.map { it.item }))
    }
    return rows.groupBy { row ->
        val value = when (grouping) {
            ShoppingListGrouping.PRODUCT_GROUP -> row.product.productGroup
            ShoppingListGrouping.DEFAULT_STORE -> row.product.store
            ShoppingListGrouping.NONE -> null
        }
        when {
            !value.isNullOrBlank() -> WearShoppingGroupTitle.Text(value)
            grouping == ShoppingListGrouping.PRODUCT_GROUP -> WearShoppingGroupTitle.Ungrouped
            else -> WearShoppingGroupTitle.NotSet
        }
    }
        .map { (title, groupRows) -> WearShoppingGroup(title, groupRows.map { it.item }) }
        // Groups without a name come last, like on the phone
        .sortedWith(
            compareBy<WearShoppingGroup> { it.title !is WearShoppingGroupTitle.Text }
                .thenBy { (it.title as? WearShoppingGroupTitle.Text)?.text?.normalizedForSearch() },
        )
}
