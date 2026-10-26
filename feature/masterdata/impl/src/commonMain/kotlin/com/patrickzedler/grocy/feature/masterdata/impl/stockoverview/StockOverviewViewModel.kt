package com.patrickzedler.grocy.feature.masterdata.impl.stockoverview

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.StockOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.ResolvedQuantityUnitConversion
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.stock.ProductStockInfo
import com.patrickzedler.grocy.core.model.stock.StockLocationAmount
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.feature.masterdata.impl.products.ignoringServerErrors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the stock overview shows; built from the database, never waits for the server. */
internal data class StockOverviewUiState(
    val groups: List<StockOverviewGroup>,
    val statusCounts: Map<StockStatus, Int>,
    val productCount: Int,
    /** Null without price tracking. */
    val totalValue: Double?,
    val currency: String,
    val priceDecimals: Int,
    val filter: StockOverviewFilter,
    /** The order the list has, the default one when the chosen field is hidden. */
    val sort: StockOverviewSort,
    val isDefaultSort: Boolean,
    val grouping: StockOverviewGrouping,
    /** The shown fields besides the name, in Grocy's order. */
    val fields: List<StockOverviewField>,
    val availableFields: List<StockOverviewField>,
    val availableGroupings: List<StockOverviewGrouping>,
    val availableStatuses: List<StockStatus>,
    /** Active locations, the location filter's choices; empty without location tracking. */
    val locations: List<Location>,
    /** Active product groups, the group filter's choices. */
    val productGroups: List<ProductGroup>,
    val config: ServerConfig,
    /** False until the first download stored any product. */
    val hasProducts: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()
}

/**
 * Grocy's stock overview: shows the stored stock at once and downloads the master data and
 * what the overview adds to it in the background. Fields, grouping and sort are kept like
 * Grocy's table options ([StockOverviewOptionsRepository]), filters and search only here.
 */
@OptIn(FlowPreview::class)
internal class StockOverviewViewModel(
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val optionsRepository: StockOverviewOptionsRepository,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    decimalSeparator: Char = platformDecimalSeparator(),
    /** The app language, whose rules name the units of amounts. */
    language: String = Locale.current.toLanguageTag(),
    today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val filter = MutableStateFlow(StockOverviewFilter())

    private val refreshController = RefreshController(viewModelScope) {
        val withShoppingLists = masterData.serverConfig.first().shoppingList
        coroutineScope {
            launch { masterData.refresh() }
            launch { stock.refreshOverview(withShoppingLists) }
        }
        // In the background, so the pictures show offline later
        viewModelScope.launch { ignoringServerErrors { masterData.downloadMissingThumbnails() } }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val masterDataPart = combine(
        masterData.products,
        masterData.productGroups,
        masterData.locations,
        masterData.quantityUnits,
        masterData.stores,
        masterData.productBarcodes,
        masterData.resolvedQuantityUnitConversions,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        StockOverviewData(
            products = values[0] as List<Product>,
            productGroups = values[1] as List<ProductGroup>,
            locations = values[2] as List<Location>,
            quantityUnits = values[3] as List<QuantityUnit>,
            stores = values[4] as List<Store>,
            barcodes = values[5] as List<ProductBarcode>,
            resolvedConversions = values[6] as List<ResolvedQuantityUnitConversion>,
        )
    }

    private val data = combine(
        masterDataPart,
        masterData.stock,
        stock.productStockInfo,
        stock.stockLocations,
    ) { part: StockOverviewData,
        amounts: List<StockAmount>,
        info: List<ProductStockInfo>,
        locations: List<StockLocationAmount>,
        ->
        part.copy(stock = amounts, info = info, stockLocations = locations)
    }

    // Typing stays smooth with thousands of products: the list follows after a short pause
    private val query = snapshotFlow { searchState.text.toString() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DELAY_MILLIS }

    private val options = combine(
        masterData.serverConfig,
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        filter,
        query,
    ) { config, settings, options, filter, query ->
        ListOptions(config, settings, options, filter, query)
    }

    val state: StateFlow<StockOverviewUiState?> = combine(data, options) { data, options ->
        val config = options.config
        val available = availableFields(config)
        val shown = available.filter { it in options.options.fields }
        val sort = effectiveSort(options.options.sortField, options.options.ascending, shown)
        val grouping = options.options.grouping.takeIf { it in availableGroupings(config) }
            ?: StockOverviewGrouping.NONE
        val list = buildStockOverview(
            data = data,
            filter = options.filter,
            sort = sort,
            grouping = grouping,
            query = options.query,
            config = config,
            settings = options.settings,
            today = today(),
            decimalSeparator = decimalSeparator,
            language = language,
        )
        StockOverviewUiState(
            groups = list.groups,
            statusCounts = list.statusCounts,
            productCount = list.productCount,
            totalValue = list.totalValue.takeIf { config.stockPriceTracking },
            currency = config.currency,
            priceDecimals = options.settings.stockDecimalPlacesPricesDisplay,
            filter = options.filter,
            sort = sort,
            isDefaultSort = sort == defaultSort(shown),
            grouping = grouping,
            fields = shown,
            availableFields = available,
            availableGroupings = availableGroupings(config),
            availableStatuses = availableStatuses(config),
            locations = data.locations.filter { it.active && config.stockLocationTracking },
            productGroups = data.productGroups.filter { it.active },
            config = config,
            hasProducts = data.products.isNotEmpty(),
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setFilter(update: (StockOverviewFilter) -> StockOverviewFilter) {
        filter.update(update)
    }

    /** Like Grocy's "Clear filter": resets every filter and the search, not the table options. */
    fun clearFilter() {
        filter.value = StockOverviewFilter()
        searchState.clearText()
        isSearchVisible = false
    }

    fun setSort(field: StockOverviewField, ascending: Boolean) {
        updateOptions { it.copy(sortField = field, ascending = ascending) }
    }

    fun setGrouping(grouping: StockOverviewGrouping) {
        updateOptions { it.copy(grouping = grouping) }
    }

    /**
     * Shows or hides [field]. Hiding the field the list is sorted by sorts it like Grocy by
     * default again, by the next due date or the name, instead of by something not shown.
     */
    fun setFieldShown(field: StockOverviewField, shown: Boolean) {
        viewModelScope.launch {
            val available = availableFields(masterData.serverConfig.first())
            optionsRepository.update { options ->
                val fields = if (shown) options.fields + field else options.fields - field
                val updated = options.copy(fields = fields)
                if (shown || options.sortField != field) {
                    updated
                } else {
                    val fallback = defaultSort(available.filter { it in fields })
                    updated.copy(sortField = fallback.field, ascending = fallback.ascending)
                }
            }
        }
    }

    /**
     * A product's picture for the list, stored for offline use; null when it is not stored and
     * cannot be downloaded, e.g. offline.
     */
    suspend fun thumbnail(fileName: String): ByteArray? =
        ignoringServerErrors { masterData.productThumbnail(fileName) }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun updateOptions(transform: (StockOverviewOptions) -> StockOverviewOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private data class ListOptions(
        val config: ServerConfig,
        val settings: UserSettings,
        val options: StockOverviewOptions,
        val filter: StockOverviewFilter,
        val query: String,
    )

    private companion object {
        const val SEARCH_DELAY_MILLIS = 150L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
