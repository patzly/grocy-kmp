package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.ProductBarcode
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.masterdata.Store
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the product list shows; built from the database, never waits for the server. */
internal data class ProductsUiState(
    val items: List<ProductListItem>,
    /** Active product groups, the choices of the group filter. */
    val productGroups: List<ProductGroup>,
    val filter: ProductFilter,
    val sort: ProductSort,
    /** False until the first download stored any product. */
    val hasProducts: Boolean,
    val showLocation: Boolean,
    val showStore: Boolean,
)

/**
 * Grocy's product list: shows the products of the database at once and downloads the server's
 * state into it in the background.
 */
@OptIn(FlowPreview::class)
internal class ProductsViewModel(
    private val repository: MasterDataRepository,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    decimalSeparator: Char = platformDecimalSeparator(),
    /** The app language, whose rules name the units of amounts. */
    language: String,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val filter = MutableStateFlow(ProductFilter())
    private val sort = MutableStateFlow(ProductSort())

    private val refreshController = RefreshController(viewModelScope) {
        repository.refresh()
        // In the background, so the list also shows its pictures offline later
        viewModelScope.launch { ignoringServerErrors { repository.downloadMissingThumbnails() } }
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val data = combine(
        repository.products,
        repository.productGroups,
        repository.locations,
        repository.quantityUnits,
        repository.stores,
        repository.productBarcodes,
        repository.stock,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ProductListData(
            products = values[0] as List<Product>,
            productGroups = values[1] as List<ProductGroup>,
            locations = values[2] as List<Location>,
            quantityUnits = values[3] as List<QuantityUnit>,
            stores = values[4] as List<Store>,
            barcodes = values[5] as List<ProductBarcode>,
            stock = values[6] as List<StockAmount>,
        )
    }

    // Typing stays smooth with thousands of products: the list follows after a short pause
    private val query = snapshotFlow { searchState.text.toString() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DELAY_MILLIS }

    private val options = combine(
        repository.serverConfig,
        repository.userSettings,
        filter,
        sort,
        query,
    ) { config, settings, filter, sort, query ->
        ListOptions(
            filter = filter,
            sort = sort,
            query = query,
            decimalPlaces = settings.stockDecimalPlacesAmounts,
            showLocation = config.stockLocationTracking,
            showStore = config.stockPriceTracking,
        )
    }

    val state: StateFlow<ProductsUiState?> = combine(data, options) { data, options ->
        ProductsUiState(
            items = buildProductList(
                data = data,
                filter = options.filter,
                sort = options.sort,
                query = options.query,
                decimalPlaces = options.decimalPlaces,
                decimalSeparator = decimalSeparator,
                showLocation = options.showLocation,
                showStore = options.showStore,
                language = language,
            ),
            productGroups = data.productGroups.filter { it.active },
            filter = options.filter,
            sort = options.sort,
            hasProducts = data.products.isNotEmpty(),
            showLocation = options.showLocation,
            showStore = options.showStore,
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setFilter(update: (ProductFilter) -> ProductFilter) {
        filter.update(update)
    }

    fun setSort(value: ProductSort) {
        sort.value = value
    }

    /** Like Grocy's "Clear filter": resets every filter and the search. */
    fun clearFilter() {
        filter.value = ProductFilter()
        searchState.clearText()
        isSearchVisible = false
    }

    /**
     * A product's picture for the list, stored for offline use; null when it is not stored and
     * cannot be downloaded, e.g. offline.
     */
    suspend fun thumbnail(fileName: String): ByteArray? =
        ignoringServerErrors { repository.productThumbnail(fileName) }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private data class ListOptions(
        val filter: ProductFilter,
        val sort: ProductSort,
        val query: String,
        val decimalPlaces: Int,
        val showLocation: Boolean,
        val showStore: Boolean,
    )

    private companion object {
        const val SEARCH_DELAY_MILLIS = 150L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** [block]'s result, null when the server could not answer it (offline, errors). */
internal suspend fun <T> ignoringServerErrors(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: GrocyApiException) {
    null
}
