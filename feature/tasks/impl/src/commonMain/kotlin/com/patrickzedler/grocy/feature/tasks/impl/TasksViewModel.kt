package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.settings.TasksOverviewOptionsRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.data.util.RefreshController
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.forms.sortableUserfieldColumns
import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.masterdata.TaskCategory
import com.patrickzedler.grocy.core.model.masterdata.Userfield
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewField
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewOptions
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewSortField
import com.patrickzedler.grocy.core.model.tasks.TasksSummary
import com.patrickzedler.grocy.core.model.tasks.tasksSummaryOf
import com.patrickzedler.grocy.core.model.text.grocyDateTimeOf
import com.patrickzedler.grocy.core.model.text.localNowMillis
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the tasks page shows; built from the database, never waits for the server. */
internal data class TasksUiState(
    val groups: List<TaskGroup>,
    val summary: TasksSummary,
    val filter: TasksFilter,
    val sortField: TableColumn<TasksOverviewSortField>,
    val ascending: Boolean,
    val isDefaultSort: Boolean,
    val grouping: TableColumn<TasksOverviewGrouping>,
    val availableGroupings: List<TableColumn<TasksOverviewGrouping>>,
    /** The userfield columns the list can be sorted and grouped by. */
    val userfieldColumns: List<Userfield>,
    /** The shown fields besides the name, in Grocy's order. */
    val fields: List<TasksOverviewField>,
    val availableFields: List<TasksOverviewField>,
    /** The categories Grocy's filter offers, the active ones. */
    val categories: List<TaskCategory>,
    val users: List<User>,
    val dueSoonDays: Int,
    /** False until the first download stored any task. */
    val hasTasks: Boolean,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()
}

/**
 * Grocy's tasks page: shows the stored tasks at once and downloads them in the background. Fields,
 * grouping and sort are kept like Grocy's table options, filters and search only here.
 */
internal class TasksViewModel(
    private val masterData: MasterDataRepository,
    private val tasks: TasksRepository,
    private val optionsRepository: TasksOverviewOptionsRepository,
    now: () -> String = { grocyDateTimeOf(localNowMillis()) },
    private val today: () -> Long = ::todayEpochDay,
) : ViewModel() {

    val searchState = TextFieldState()

    var isSearchVisible by mutableStateOf(false)
        private set

    val actions = TaskActions(viewModelScope, tasks, now)

    private val filter = MutableStateFlow(TasksFilter())

    private val refreshController = RefreshController(viewModelScope) { tasks.refresh() }
    val refresh: StateFlow<RefreshState> = refreshController.state

    private val query = snapshotFlow { searchState.text.toString() }.distinctUntilChanged()

    private val data = combine(
        tasks.tasks,
        masterData.taskCategories,
        masterData.users,
        masterData.userfields(MasterDataRepository.TASKS),
    ) { all, categories, users, userfields -> TasksData(all, categories, users, userfields) }

    private val listOptions = combine(
        masterData.userSettings,
        optionsRepository.options.filterNotNull(),
        filter,
        query,
    ) { settings, options, filter, query -> ListOptions(settings, options, filter, query) }

    val state: StateFlow<TasksUiState?> = combine(data, listOptions) { data, list ->
        val dueSoonDays = list.settings.tasksDueSoonDays
        val available = availableTaskFields(data.userfields.any { it.showAsColumnInTables })
        val shown = available.filter { it in list.options.fields }
        val userfieldColumns = sortableUserfieldColumns(data.userfields)
        val sort = effectiveTaskSort(list.options, shown, userfieldColumns)
        val groupings = availableTaskGroupings(userfieldColumns)
        val grouping = effectiveTaskGrouping(list.options, groupings)
        val day = today()
        TasksUiState(
            groups = buildTasksOverview(
                data = data,
                filter = list.filter,
                sort = sort,
                grouping = grouping,
                query = list.query,
                today = day,
                dueSoonDays = dueSoonDays,
            ),
            summary = tasksSummaryOf(data.tasks, day, dueSoonDays),
            filter = list.filter,
            sortField = sort.first,
            ascending = sort.second,
            isDefaultSort = sort == (TableColumn.Own(defaultTaskSort(shown)) to true),
            grouping = grouping,
            availableGroupings = groupings,
            userfieldColumns = userfieldColumns,
            fields = shown,
            availableFields = available,
            categories = data.categories.filter { it.active },
            users = data.users,
            dueSoonDays = dueSoonDays,
            hasTasks = data.tasks.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        refresh()
    }

    fun refresh() = refreshController.refresh()

    fun onErrorShown() = refreshController.onErrorShown()

    fun setFilter(update: (TasksFilter) -> TasksFilter) {
        filter.update(update)
    }

    /** Like Grocy's "Clear filter": resets every filter and the search, not the table options. */
    fun clearFilter() {
        filter.value = TasksFilter()
        hideSearch()
    }

    fun setSort(column: TableColumn<TasksOverviewSortField>, ascending: Boolean) {
        updateOptions { it.withSort(column, ascending) }
    }

    fun setGrouping(column: TableColumn<TasksOverviewGrouping>) {
        updateOptions { it.withGrouping(column) }
    }

    fun setFieldShown(field: TasksOverviewField, shown: Boolean) {
        updateOptions { it.copy(fields = if (shown) it.fields + field else it.fields - field) }
    }

    fun showSearch() {
        isSearchVisible = true
    }

    fun hideSearch() {
        searchState.clearText()
        isSearchVisible = false
    }

    private fun updateOptions(transform: (TasksOverviewOptions) -> TasksOverviewOptions) {
        viewModelScope.launch { optionsRepository.update(transform) }
    }

    private data class ListOptions(
        val settings: UserSettings,
        val options: TasksOverviewOptions,
        val filter: TasksFilter,
        val query: String,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
