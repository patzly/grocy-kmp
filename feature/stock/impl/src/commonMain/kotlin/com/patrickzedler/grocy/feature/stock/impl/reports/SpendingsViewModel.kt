package com.patrickzedler.grocy.feature.stock.impl.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.MINIMUM_LOADING_DURATION
import com.patrickzedler.grocy.core.data.util.withMinimumDuration
import com.patrickzedler.grocy.core.design.component.CHART_COLOR_COUNT
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.stock.PricedPurchase
import com.patrickzedler.grocy.core.model.stock.ProductGroupFilter
import com.patrickzedler.grocy.core.model.stock.SpendingsGrouping
import com.patrickzedler.grocy.core.model.stock.SpendingsRange
import com.patrickzedler.grocy.core.model.stock.SpendingsRangePreset
import com.patrickzedler.grocy.core.model.stock.SpendingsRow
import com.patrickzedler.grocy.core.model.stock.spendingsOf
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlin.time.TimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Grocy's choices of its spendings page; the range defaults to this month. */
internal data class SpendingsFilter(
    val range: SpendingsRange,
    val grouping: SpendingsGrouping = SpendingsGrouping.PRODUCT,
    /** Only when grouped by product, like Grocy shows it. */
    val productGroup: ProductGroupFilter = ProductGroupFilter.All,
)

/** The purchases of the chosen range, read from the server. */
internal sealed interface SpendingsLoad {
    data object Loading : SpendingsLoad

    data class Loaded(val purchases: List<PricedPurchase>) : SpendingsLoad

    data class Failed(val error: ChangeError) : SpendingsLoad
}

/**
 * The spendings' table as rows by name and the pie chart's slices: the largest rows get the
 * palette's colors, the rest share one slice "Other", so no color repeats. [colorOf] tells a
 * row's color slot, null for "Other".
 */
internal data class SpendingsChart(
    val slices: List<SpendingsSlice>,
    val colorOf: List<Int?>,
) {
    val total: Double get() = slices.sumOf { it.total }
}

/** A slice of the chart: one row ([row]) in color [slot], or "Other" without either. */
internal data class SpendingsSlice(val row: SpendingsRow?, val slot: Int?, val total: Double)

/** The theme's chart colors; more rows than that fold into "Other". */
internal const val SPENDINGS_COLORS = CHART_COLOR_COUNT

/** The chart of [rows]: slices from the largest down, as many as the palette has colors. */
internal fun spendingsChartOf(rows: List<SpendingsRow>): SpendingsChart {
    val counted = rows.withIndex().filter { it.value.total > 0.0 }
    val ranked = counted.sortedByDescending { it.value.total }
    // One color left over would be "Other" for a single row, which can as well keep its own
    val colored = if (ranked.size <= SPENDINGS_COLORS) {
        ranked
    } else {
        ranked.take(SPENDINGS_COLORS - 1)
    }
    val slots = colored.withIndex().associate { (slot, row) -> row.index to slot }
    val rest = ranked.drop(colored.size)
    val slices = colored.mapIndexed { slot, row ->
        SpendingsSlice(row.value, slot, row.value.total)
    } +
        listOfNotNull(
            rest.takeIf { it.isNotEmpty() }
                ?.let { list -> SpendingsSlice(null, null, list.sumOf { it.value.total }) },
        )
    return SpendingsChart(slices, rows.indices.map { slots[it] })
}

internal data class SpendingsUiState(
    val filter: SpendingsFilter,
    val load: SpendingsLoad,
    val rows: List<SpendingsRow>,
    val chart: SpendingsChart,
    /** The active groups for the product group filter. */
    val productGroups: List<ProductGroup>,
    val currency: String,
    val priceDecimals: Int,
) {
    val isDefaultFilter: Boolean
        get() = filter.range.preset == SpendingsRangePreset.THIS_MONTH &&
            filter.grouping == SpendingsGrouping.PRODUCT &&
            filter.productGroup == ProductGroupFilter.All
}

/**
 * Grocy's stock report "Spendings": what the purchases of a date range cost, grouped by
 * product, product group or store, as pie chart and table. Online only: the purchases are read
 * from the server for each range; grouping and filtering need no new request.
 */
internal class SpendingsViewModel(
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val today: () -> Long = ::todayEpochDay,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    private val filter = MutableStateFlow(SpendingsFilter(SpendingsRange.default(today())))
    private val load = MutableStateFlow<SpendingsLoad>(SpendingsLoad.Loading)
    private var loadJob: Job? = null

    /** True while a pull to refresh reads the range again, the list stays meanwhile. */
    val isRefreshing = MutableStateFlow(false)

    val state: StateFlow<SpendingsUiState?> = combine(
        combine(filter, load, ::Pair),
        masterData.products,
        masterData.productGroups,
        masterData.stores,
        combine(masterData.serverConfig, masterData.userSettings, ::Pair),
    ) { (currentFilter, currentLoad), products, groups, stores, (config, settings) ->
        val purchases = (currentLoad as? SpendingsLoad.Loaded)?.purchases.orEmpty()
        val productGroup = currentFilter.productGroup.takeIf {
            currentFilter.grouping == SpendingsGrouping.PRODUCT
        } ?: ProductGroupFilter.All
        val rows = spendingsOf(
            purchases = purchases,
            from = currentFilter.range.from,
            to = currentFilter.range.to,
            grouping = currentFilter.grouping,
            groupFilter = productGroup,
            products = products,
            productGroups = groups,
            stores = stores,
        )
        SpendingsUiState(
            filter = currentFilter,
            load = currentLoad,
            rows = rows,
            chart = spendingsChartOf(rows),
            productGroups = groups.filter { it.active }.sortedBy { it.name.lowercase() },
            currency = config.currency,
            priceDecimals = settings.stockDecimalPlacesPricesDisplay,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        load(showLoading = true)
    }

    fun setGrouping(grouping: SpendingsGrouping) {
        filter.update { it.copy(grouping = grouping) }
    }

    fun setProductGroup(productGroup: ProductGroupFilter) {
        filter.update { it.copy(productGroup = productGroup) }
    }

    /** A preset of Grocy's picker; [SpendingsRangePreset.CUSTOM] comes with [setCustomRange]. */
    fun setRange(preset: SpendingsRangePreset) {
        setRange(SpendingsRange.of(preset, today()))
    }

    fun setCustomRange(start: Long, end: Long) {
        setRange(SpendingsRange.custom(start, end))
    }

    /** Grocy's "Clear filter": this month, by product, every group. */
    fun clearFilter() {
        val cleared = SpendingsFilter(SpendingsRange.default(today()))
        val rangeChanged = cleared.range != filter.value.range
        filter.value = cleared
        if (rangeChanged) load(showLoading = true)
    }

    fun refresh() {
        isRefreshing.value = true
        load(showLoading = false)
    }

    private fun setRange(range: SpendingsRange) {
        if (range == filter.value.range) return
        filter.update { it.copy(range = range) }
        load(showLoading = true)
    }

    private fun load(showLoading: Boolean) {
        loadJob?.cancel()
        val range = filter.value.range
        if (showLoading) load.value = SpendingsLoad.Loading
        loadJob = viewModelScope.launch {
            var purchases: List<PricedPurchase> = emptyList()
            val error = withMinimumDuration(MINIMUM_LOADING_DURATION, timeSource) {
                changeErrorOf { purchases = stock.pricedPurchases(range.from, range.to) }
            }
            load.value = if (error == null) {
                SpendingsLoad.Loaded(purchases)
            } else {
                SpendingsLoad.Failed(error)
            }
            isRefreshing.value = false
        }
    }
}
