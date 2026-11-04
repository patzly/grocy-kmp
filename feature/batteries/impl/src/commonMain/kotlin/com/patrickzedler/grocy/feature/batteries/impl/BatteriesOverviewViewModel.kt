package com.patrickzedler.grocy.feature.batteries.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.BatteriesOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewField
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewOptions
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewSortField
import com.patrickzedler.grocy.core.model.batteries.BatteriesSummary
import com.patrickzedler.grocy.core.model.batteries.BatteryDueStatus
import com.patrickzedler.grocy.core.model.batteries.batteriesSummaryOf
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the batteries overview shows; built from the database, never waits for the server. */
internal data class BatteriesOverviewUiState(
    val groups: List<BatteryGroup>,
    val summary: BatteriesSummary,
    val status: BatteryDueStatus?,
    val sortField: TableColumn<BatteriesOverviewSortField>,
    val ascending: Boolean,
    val isDefaultSort: Boolean,
    val grouping: TableColumn<BatteriesOverviewGrouping>,
    /** The shown fields besides the name, in Grocy's order. */
    val fields: List<BatteriesOverviewField>,
    val availableFields: List<BatteriesOverviewField>,
    val availableGroupings: List<TableColumn<BatteriesOverviewGrouping>>,
    /** The userfield columns the list can be sorted and grouped by. */
    val userfieldColumns: List<Userfield>,
    val dueSoonDays: Int,
    val config: ServerConfig,
    /** False until the first download stored any battery. */
    val hasBatteries: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()
}

/**
 * Grocy's batteries overview: shows the stored batteries at once and downloads them in the
 * background. Fields, grouping and sort are kept like Grocy's table options, filter and search
 * only here.
 */
internal class BatteriesOverviewViewModel(
    private val masterData: MasterDataRepository,
    private val batteries: BatteriesRepository,
    private val optionsRepository: BatteriesOverviewOptionsRepository,
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    val actions = BatteryActions(viewModelScope, batteries, now)

    private val status = MutableStateFlow<BatteryDueStatus?>(null)

    private val refreshController = RefreshController(viewModelScope) { batteries.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    private val data = combine(
        batteries.current,
        masterData.batteries,
        masterData.userfields(MasterDataRepository.BATTERIES),
        ::BatteriesOverviewData,
    )

    private val listOptions = combine(
        masterData.serverConfig,
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        status,
        query,
    ) { config, settings, options, status, query ->
        ListOptions(config, settings, options, status, query)
    }

    val state: StateFlow<BatteriesOverviewUiState?> = combine(
        data,
        listOptions,
    ) { data, list ->
        val dueSoonDays = list.settings.batteriesDueSoonDays
        val available = availableBatteryFields(
            hasUserfieldColumns = data.userfields.any { it.showAsColumnInTables },
        )
        val shown = available.filter { it in list.options.fields }
        val userfieldColumns = sortableUserfieldColumns(data.userfields)
        val sort = effectiveBatterySort(list.options, shown, userfieldColumns)
        val groupings = availableBatteryGroupings(userfieldColumns)
        val grouping = effectiveBatteryGrouping(list.options, groupings)
        val time = now()
        BatteriesOverviewUiState(
            groups = buildBatteriesOverview(
                data = data,
                status = list.status,
                sort = sort,
                grouping = grouping,
                query = list.query,
                now = time,
                dueSoonDays = dueSoonDays,
            ),
            summary = batteriesSummaryOf(data.current, time, dueSoonDays),
            status = list.status,
            sortField = sort.first,
            ascending = sort.second,
            isDefaultSort = sort == (TableColumn.Own(defaultBatterySort(shown)) to true),
            grouping = grouping,
            fields = shown,
            availableFields = available,
            availableGroupings = groupings,
            userfieldColumns = userfieldColumns,
            dueSoonDays = dueSoonDays,
            config = list.config,
            hasBatteries = data.current.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setStatus(value: BatteryDueStatus?) {
        status.value = value
    }

    /** Like Grocy's "Clear filter": resets the filter and the search, not the table options. */
    fun clearFilter() {
        status.value = null
        hideSearch()
    }

    fun setSort(column: TableColumn<BatteriesOverviewSortField>, ascending: Boolean) {
        updateOptions { it.withSort(column, ascending) }
    }

    fun setGrouping(column: TableColumn<BatteriesOverviewGrouping>) {
        updateOptions { it.withGrouping(column) }
    }

    fun setFieldShown(field: BatteriesOverviewField, shown: Boolean) {
        updateOptions { it.copy(fields = if (shown) it.fields + field else it.fields - field) }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun updateOptions(
        transform: (BatteriesOverviewOptions) -> BatteriesOverviewOptions,
    ) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private data class ListOptions(
        val config: ServerConfig,
        val settings: UserSettings,
        val options: BatteriesOverviewOptions,
        val status: BatteryDueStatus?,
        val query: String,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
