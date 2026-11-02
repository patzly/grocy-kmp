package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.ChoresOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewField
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewOptions
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewSortField
import com.patrickzedler.grocy.core.model.chores.ChoresSummary
import com.patrickzedler.grocy.core.model.chores.choresSummaryOf
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the chores overview shows; built from the database, never waits for the server. */
internal data class ChoresOverviewUiState(
    val groups: List<ChoreGroup>,
    val summary: ChoresSummary,
    val filter: ChoresFilter,
    val sortField: ChoresOverviewSortField,
    val ascending: Boolean,
    val isDefaultSort: Boolean,
    val grouping: ChoresOverviewGrouping,
    /** The shown fields besides the name, in Grocy's order. */
    val fields: List<ChoresOverviewField>,
    val availableFields: List<ChoresOverviewField>,
    val availableGroupings: List<ChoresOverviewGrouping>,
    val users: List<User>,
    val currentUserId: Int?,
    val dueSoonDays: Int,
    val swapsTrackingButtons: Boolean,
    val config: ServerConfig,
    /** False until the first download stored any chore. */
    val hasChores: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()
}

/**
 * Grocy's chores overview: shows the stored chores at once and downloads them in the background.
 * Fields, grouping and sort are kept like Grocy's table options, filters and search only here.
 */
internal class ChoresOverviewViewModel(
    private val masterData: MasterDataRepository,
    private val chores: ChoresRepository,
    private val optionsRepository: ChoresOverviewOptionsRepository,
    private val now: () -> String = { grocyDateTimeOf(localNowMillis()) },
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    val actions = ChoreActions(viewModelScope, chores, now)

    private val filter = MutableStateFlow(ChoresFilter())

    private val refreshController = RefreshController(viewModelScope) { chores.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    private val data = combine(
        chores.current,
        masterData.chores,
        masterData.users,
        masterData.userfields(MasterDataRepository.CHORES),
    ) { current, all, users, userfields ->
        ChoresOverviewData(current, all, users, userfields)
    }

    private val listOptions = combine(
        masterData.serverConfig,
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        filter,
        query,
    ) { config, settings, options, filter, query ->
        ListOptions(config, settings, options, filter, query)
    }

    val state: StateFlow<ChoresOverviewUiState?> = combine(
        data,
        listOptions,
        chores.currentUserId,
    ) { data, list, userId ->
        val config = list.config
        val dueSoonDays = list.settings.choresDueSoonDays
        val available = availableChoreFields(
            config = config,
            hasUserfieldColumns = data.userfields.any { it.showAsColumnInTables },
        )
        val shown = available.filter { it in list.options.fields }
        val sort = effectiveChoreSort(list.options, shown)
        val groupings = availableChoreGroupings(config)
        val grouping = list.options.grouping.takeIf { it in groupings }
            ?: ChoresOverviewGrouping.NONE
        val time = now()
        ChoresOverviewUiState(
            groups = buildChoresOverview(
                data = data,
                filter = list.filter,
                sort = sort,
                grouping = grouping,
                query = list.query,
                now = time,
                dueSoonDays = dueSoonDays,
            ),
            summary = choresSummaryOf(data.current, time, dueSoonDays, userId),
            filter = list.filter,
            sortField = sort.first,
            ascending = sort.second,
            isDefaultSort = sort == (defaultChoreSort(shown) to true),
            grouping = grouping,
            fields = shown,
            availableFields = available,
            availableGroupings = groupings,
            users = data.users.takeIf { config.choresAssignments }.orEmpty(),
            currentUserId = userId.takeIf { config.choresAssignments },
            dueSoonDays = dueSoonDays,
            swapsTrackingButtons = list.settings.choresOverviewSwapTrackingButtons,
            config = config,
            hasChores = data.current.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setFilter(update: (ChoresFilter) -> ChoresFilter) {
        filter.update(update)
    }

    /** Like Grocy's "Clear filter": resets every filter and the search, not the table options. */
    fun clearFilter() {
        filter.value = ChoresFilter()
        hideSearch()
    }

    fun setSort(field: ChoresOverviewSortField, ascending: Boolean) {
        updateOptions { it.copy(sortField = field, ascending = ascending) }
    }

    fun setGrouping(grouping: ChoresOverviewGrouping) {
        updateOptions { it.copy(grouping = grouping) }
    }

    fun setFieldShown(field: ChoresOverviewField, shown: Boolean) {
        updateOptions { it.copy(fields = if (shown) it.fields + field else it.fields - field) }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun updateOptions(transform: (ChoresOverviewOptions) -> ChoresOverviewOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private data class ListOptions(
        val config: ServerConfig,
        val settings: UserSettings,
        val options: ChoresOverviewOptions,
        val filter: ChoresFilter,
        val query: String,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
