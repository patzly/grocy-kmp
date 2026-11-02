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
import com.patrickzedler.grocy.core.data.settings.ChoresJournalOptionsRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.changeErrorOf
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.chores.ChoreExecution
import com.patrickzedler.grocy.core.model.chores.ChoresJournalField
import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import com.patrickzedler.grocy.core.model.chores.ChoresJournalSortField
import com.patrickzedler.grocy.core.model.masterdata.Chore
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the chores journal shows. */
internal data class ChoresJournalUiState(
    val groups: List<ChoresJournalGroup>,
    /** The active chores and the chore filtered by, the chore filter's choices. */
    val chores: List<Chore>,
    val filter: ChoresJournalFilter,
    val hasExecutions: Boolean,
    val sortField: ChoresJournalSortField,
    val ascending: Boolean,
    val isDefaultSort: Boolean,
    val grouping: ChoresJournalGrouping,
    val fields: List<ChoresJournalField>,
    val availableFields: List<ChoresJournalField>,
    val availableGroupings: List<ChoresJournalGrouping>,
)

internal sealed interface ChoresJournalEvent {
    data object Undone : ChoresJournalEvent

    data class Failed(val error: ChangeError) : ChoresJournalEvent
}

/**
 * Grocy's "Chores journal" (of [choreId] if given) as stored, downloaded for the chosen date
 * range, with undoing executions. Fields, grouping and sort are kept like Grocy's table options,
 * filters and search only here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChoresJournalViewModel(
    choreId: Int?,
    masterData: MasterDataRepository,
    private val chores: ChoresRepository,
    private val optionsRepository: ChoresJournalOptionsRepository,
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    private val filter = MutableStateFlow(ChoresJournalFilter(choreId = choreId))

    private val since = filter.map { choresJournalSince(today(), it.months) }.distinctUntilChanged()

    private val refreshController = RefreshController(viewModelScope) {
        chores.refreshExecutions(choresJournalSince(today(), filter.value.months))
    }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    private val data = combine(
        masterData.chores,
        masterData.users,
        masterData.userfields(CHORES_LOG),
        masterData.serverConfig,
        ::JournalData,
    )

    val state: StateFlow<ChoresJournalUiState?> = combine(
        since.flatMapLatest { chores.executions(it) },
        data,
        filter,
        query,
        optionsRepository.options.filterNotNull(),
    ) { executions: List<ChoreExecution>, journal, current, search, options ->
        val available = availableChoresJournalFields(
            config = journal.config,
            hasUserfieldColumns = journal.userfields.any { it.showAsColumnInTables },
        )
        val shown = available.filter { it in options.fields }
        val sort = effectiveChoresJournalSort(options, shown)
        val groupings = availableChoresJournalGroupings(journal.config)
        val grouping = options.grouping.takeIf { it in groupings } ?: ChoresJournalGrouping.NONE
        ChoresJournalUiState(
            groups = buildChoresJournal(
                executions = executions,
                chores = journal.chores,
                users = journal.users,
                userfields = journal.userfields,
                filter = current,
                query = search,
                sort = sort,
                grouping = grouping,
            ),
            chores = journal.chores.filter { it.active || it.id == current.choreId }
                .sortedBy { it.name.lowercase() },
            filter = current,
            hasExecutions = executions.isNotEmpty(),
            sortField = sort.first,
            ascending = sort.second,
            isDefaultSort = sort == defaultChoresJournalSort(shown),
            grouping = grouping,
            fields = shown,
            availableFields = available,
            availableGroupings = groupings,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var isUndoing by mutableStateOf(false)
        private set

    private val eventChannel = Channel<ChoresJournalEvent>(Channel.BUFFERED)
    val events: Flow<ChoresJournalEvent> = eventChannel.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    /** A longer date range downloads the executions it adds. */
    fun setFilter(update: (ChoresJournalFilter) -> ChoresJournalFilter) {
        val months = filter.value.months
        filter.update(update)
        if (filter.value.months != months) refresh()
    }

    fun clearFilter() = setFilter { ChoresJournalFilter() }

    fun setSort(field: ChoresJournalSortField, ascending: Boolean) {
        updateOptions { it.copy(sortField = field, ascending = ascending) }
    }

    fun setGrouping(grouping: ChoresJournalGrouping) {
        updateOptions { it.copy(grouping = grouping) }
    }

    fun setFieldShown(field: ChoresJournalField, shown: Boolean) {
        updateOptions { it.copy(fields = if (shown) it.fields + field else it.fields - field) }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        isSearchVisible = false
        searchState.clearText()
    }

    /** Grocy's "Undo chore execution". */
    fun undo(execution: ChoreExecution) {
        if (isUndoing || execution.undone) return
        isUndoing = true
        viewModelScope.launch {
            val error = changeErrorOf { chores.undo(execution.id, execution.choreId) }
            isUndoing = false
            eventChannel.send(
                if (error == null) ChoresJournalEvent.Undone else ChoresJournalEvent.Failed(error),
            )
        }
    }

    private fun updateOptions(transform: (ChoresJournalOptions) -> ChoresJournalOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private class JournalData(
        val chores: List<Chore>,
        val users: List<User>,
        val userfields: List<Userfield>,
        val config: ServerConfig,
    )

    private companion object {
        const val CHORES_LOG = "chores_log"
    }
}
