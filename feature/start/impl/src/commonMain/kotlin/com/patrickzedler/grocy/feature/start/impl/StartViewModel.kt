package com.patrickzedler.grocy.feature.start.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListSummary
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockSummary
import com.patrickzedler.grocy.core.model.stock.stockSummaryOf
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.masterdata_batteries_count
import com.patrickzedler.grocy.core.resources.masterdata_chores_count
import com.patrickzedler.grocy.core.resources.masterdata_locations_count
import com.patrickzedler.grocy.core.resources.masterdata_product_groups_count
import com.patrickzedler.grocy.core.resources.masterdata_products_count
import com.patrickzedler.grocy.core.resources.masterdata_quantity_units_count
import com.patrickzedler.grocy.core.resources.masterdata_stores_count
import com.patrickzedler.grocy.core.resources.masterdata_task_categories_count
import com.patrickzedler.grocy.core.resources.masterdata_userentities_count
import com.patrickzedler.grocy.core.resources.masterdata_userfields_count
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.PluralStringResource

/**
 * The objects the master data item counts, in the order of Grocy's "Manage master data" menu
 * and hidden like its entries when the server turned the feature off.
 */
internal enum class MasterDataCount(
    val text: PluralStringResource,
    val isAvailable: (ServerConfig) -> Boolean = { true },
) {
    PRODUCTS(Res.plurals.masterdata_products_count),
    LOCATIONS(
        Res.plurals.masterdata_locations_count,
        { it.stock && it.stockLocationTracking },
    ),
    STORES(Res.plurals.masterdata_stores_count, { it.stock && it.stockPriceTracking }),
    QUANTITY_UNITS(Res.plurals.masterdata_quantity_units_count),
    PRODUCT_GROUPS(Res.plurals.masterdata_product_groups_count),
    CHORES(Res.plurals.masterdata_chores_count, { it.chores }),
    BATTERIES(Res.plurals.masterdata_batteries_count, { it.batteries }),
    TASK_CATEGORIES(Res.plurals.masterdata_task_categories_count, { it.tasks }),
    USERFIELDS(Res.plurals.masterdata_userfields_count),
    USERENTITIES(Res.plurals.masterdata_userentities_count),
}

/** What the start screen's sections tell about the stored data. */
internal data class StartUiState(
    /** Null without the stock feature or before the first download. */
    val stock: StockSummary?,
    val currency: String,
    val priceDecimals: Int,
    val dueSoonDays: Int,
    /** Null without the shopping list feature or before the first download. */
    val shoppingLists: ShoppingListSummary?,
    /** The objects of each available category, those without any left out. */
    val masterDataCounts: List<Pair<MasterDataCount, Int>>,
)

/**
 * The start screen: what its sections tell about the stock and the master data, from the
 * database, downloaded again when it opens and by pull to refresh. [serverConfig] (read at app
 * start, null until then) hides the sections of features the server turned off.
 */
internal class StartViewModel(
    private val repository: MasterDataRepository,
    private val stock: StockRepository,
    private val shoppingLists: ShoppingListRepository,
    val serverConfig: StateFlow<ServerConfig?>,
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    private val refreshController = RefreshController(viewModelScope) {
        val config = serverConfig.filterNotNull().first()
        coroutineScope {
            launch { repository.refresh() }
            if (config.stock) launch { stock.refreshOverview(config.shoppingList) }
            if (config.stock && config.shoppingList) launch { shoppingLists.refresh() }
        }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val counts: Flow<Map<MasterDataCount, Int>> = combine(
        MasterDataCount.entries.map { count ->
            repository.objectsOf(count).map { count to it.size }
        },
    ) { it.toMap() }

    private val stockData = combine(
        repository.products,
        repository.stock,
        stock.productStockInfo,
        shoppingLists.summary,
    ) { products, amounts, info, lists -> StockData(products, amounts, info, lists) }

    val state: StateFlow<StartUiState?> = combine(
        serverConfig.filterNotNull(),
        repository.userSettings,
        stockData,
        counts,
    ) { config, settings, stockData, counts ->
        stateOf(config, settings, stockData, counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    /**
     * The list a quick add puts its item on: the one the shopping list shows (chosen there in this
     * process), otherwise Grocy's default list; null before the lists are known.
     */
    fun quickAddListId(): Int? {
        val lists = state.value?.shoppingLists ?: return null
        val selected = shoppingLists.selectedListId.value
        return selected?.takeIf { id -> lists.lists.any { it.listId == id } }
            ?: lists.defaultList?.listId
    }

    private fun stateOf(
        config: ServerConfig,
        settings: UserSettings,
        stockData: StockData,
        counts: Map<MasterDataCount, Int>,
    ) = StartUiState(
        // Nothing to tell until the first download stored the products
        stock = if (config.stock && stockData.products.isNotEmpty()) {
            stockSummaryOf(
                products = stockData.products,
                stock = stockData.amounts,
                info = stockData.info,
                config = config,
                dueSoonDays = settings.stockDueSoonDays,
                today = today(),
            )
        } else {
            null
        },
        // No lists before the first download, Grocy always has its first one
        shoppingLists = stockData.shoppingLists.takeIf {
            config.stock && config.shoppingList && it.lists.isNotEmpty()
        },
        currency = config.currency,
        priceDecimals = settings.stockDecimalPlacesPricesDisplay,
        dueSoonDays = settings.stockDueSoonDays,
        masterDataCounts = MasterDataCount.entries
            .filter { it.isAvailable(config) }
            .mapNotNull { count -> counts[count]?.takeIf { it > 0 }?.let { count to it } },
    )

    private class StockData(
        val products: List<Product>,
        val amounts: List<StockAmount>,
        val info: List<ProductStockInfo>,
        val shoppingLists: ShoppingListSummary,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** All objects counted as [count], disabled ones included like Grocy's tables. */
private fun MasterDataRepository.objectsOf(count: MasterDataCount): Flow<List<Any>> =
    when (count) {
        MasterDataCount.PRODUCTS -> products
        MasterDataCount.LOCATIONS -> locations
        MasterDataCount.STORES -> stores
        MasterDataCount.QUANTITY_UNITS -> quantityUnits
        MasterDataCount.PRODUCT_GROUPS -> productGroups
        MasterDataCount.CHORES -> chores
        MasterDataCount.BATTERIES -> batteries
        MasterDataCount.TASK_CATEGORIES -> taskCategories
        MasterDataCount.USERFIELDS -> allUserfields
        MasterDataCount.USERENTITIES -> userentities
    }
