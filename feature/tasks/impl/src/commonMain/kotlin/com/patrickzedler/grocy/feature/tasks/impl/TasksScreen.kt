@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.tasks.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.GroupTitle
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.ClearFilterChip
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.MultiSelectChip
import com.patrickzedler.grocy.core.forms.ObjectAction
import com.patrickzedler.grocy.core.forms.ObjectActionEvent
import com.patrickzedler.grocy.core.forms.ObjectActionsHost
import com.patrickzedler.grocy.core.forms.ObjectActionsViewModel
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.forms.ToggleFilterChip
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.label
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.forms.showsPullIndicator
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.model.tasks.TaskDueStatus
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewField
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewGrouping
import com.patrickzedler.grocy.core.model.tasks.TasksOverviewSortField
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.chores_assignment
import com.patrickzedler.grocy.core.resources.chores_status_due_soon
import com.patrickzedler.grocy.core.resources.chores_status_due_today
import com.patrickzedler.grocy.core.resources.chores_status_overdue
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.masterdata_task_categories
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.settings_tasks
import com.patrickzedler.grocy.core.resources.shopping_list_list_with_count
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.resources.task_category
import com.patrickzedler.grocy.core.resources.task_create
import com.patrickzedler.grocy.core.resources.task_due
import com.patrickzedler.grocy.core.resources.task_uncategorized
import com.patrickzedler.grocy.core.resources.tasks_count
import com.patrickzedler.grocy.core.resources.tasks_show_done
import com.patrickzedler.grocy.core.resources.tasks_title
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where the tasks page leads. */
internal class TasksLinks(
    val onCreateClick: () -> Unit,
    val onTaskClick: (id: Int) -> Unit,
    val onCategoriesClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onTaskDeleted: (id: Int) -> Unit,
)

/**
 * Grocy's "Tasks": the tasks as items with their fields as chips, colored like Grocy's rows, how
 * many below the title, the filters (with the numbers of Grocy's colored messages), "Show done
 * tasks" and the table options as chips, the search. Creating a task is the toolbar's main action,
 * a tap opens the task's form.
 *
 * @param isShown whether the list is the screen the user sees, which downloads the tasks again
 *   after a page on top may have changed them.
 */
@Composable
internal fun TasksScreen(
    viewModel: TasksViewModel,
    objectActions: ObjectActionsViewModel,
    links: TasksLinks,
    menuItems: List<AppBarMenuItem>,
    selectedTaskId: Int?,
    isBesideDetail: Boolean,
    isShown: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasTasks == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    TaskActionsHost(viewModel.actions, snackbarHostState)
    ObjectActionsHost(objectActions, snackbarHostState) { event ->
        if (event is ObjectActionEvent.Deleted) links.onTaskDeleted(event.id)
    }
    // The first time the ViewModel downloaded them already
    var wasHidden by remember { mutableStateOf(false) }
    LaunchedEffect(isShown) {
        if (isShown && wasHidden) viewModel.refresh()
        wasHidden = !isShown
    }
    // Back closes the search first and stays on the list
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.isSearchVisible,
        onBackCompleted = viewModel::hideSearch,
    )

    SegmentedListScreen(
        title = stringResource(Res.string.tasks_title),
        subtitle = state?.takeIf { it.hasTasks }?.summary?.count?.let {
            pluralStringResource(Res.plurals.tasks_count, it, it)
        },
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        actionMenuItems = listOf(
            AppBarMenuItem(
                stringResource(Res.string.masterdata_task_categories),
                onClick = links.onCategoriesClick,
            ),
            AppBarMenuItem(
                stringResource(Res.string.settings_tasks),
                onClick = links.onSettingsClick,
            ),
        ),
        pane = ScreenPane.List.takeIf { isBesideDetail },
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.showsPullIndicator(state?.hasTasks == true, storedByStart = true),
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.task_create),
                onFabClick = links.onCreateClick,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = { scope.showSearchAtTop(listState, viewModel::showSearch) },
                )
            }
        },
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            TasksFilterBar(current, viewModel, contentPadding)
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        taskItems(
            state = current,
            refresh = refresh,
            isSearching = viewModel.searchState.text.isNotBlank(),
            rowActions = TaskRowActions(
                onClick = { links.onTaskClick(it.id) },
                onComplete = viewModel.actions::complete,
                onUndo = { viewModel.actions.undo(it.id) },
                onDelete = { objectActions.request(ObjectAction.DELETE, it.id) },
            ),
            selectedTaskId = selectedTaskId.takeIf { isBesideDetail },
            isBesideDetail = isBesideDetail,
            onRetryClick = viewModel::refresh,
        )
    }
}

@Composable
private fun TasksFilterBar(
    state: TasksUiState,
    viewModel: TasksViewModel,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    val uncategorized = stringResource(Res.string.task_uncategorized)
    val filter = state.filter
    val summary = state.summary
    val statuses = TaskDueStatus.entries.filter {
        it != TaskDueStatus.DUE_SOON || state.dueSoonDays > 0
    }
    val categoryName = { id: Int? ->
        when (id) {
            null -> all
            UNCATEGORIZED -> uncategorized
            else -> state.categories.firstOrNull { it.id == id }?.name.orEmpty()
        }
    }
    val userName = { id: Int? ->
        if (id == null) all else state.users.firstOrNull { it.id == id }?.displayName.orEmpty()
    }
    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_status),
            optionGroups = listOf(listOf<TaskDueStatus?>(null), statuses),
            selected = filter.status,
            optionLabel = { status ->
                status?.let {
                    val count = when (it) {
                        TaskDueStatus.OVERDUE -> summary.overdue
                        TaskDueStatus.DUE_TODAY -> summary.dueToday
                        TaskDueStatus.DUE_SOON -> summary.dueSoon
                    }
                    stringResource(
                        Res.string.shopping_list_list_with_count,
                        stringResource(it.label),
                        count,
                    )
                } ?: all
            },
            selectedLabel = { status -> status?.let { stringResource(it.label) } ?: all },
            onSelect = { status -> viewModel.setFilter { it.copy(status = status) } },
        )
        DropdownFilterChip(
            label = stringResource(Res.string.task_category),
            optionGroups = listOf(
                listOf<Int?>(null),
                state.categories.map { it.id },
                listOf(UNCATEGORIZED),
            ),
            selected = filter.categoryId,
            optionLabel = { categoryName(it) },
            selectedLabel = { categoryName(it) },
            onSelect = { id -> viewModel.setFilter { it.copy(categoryId = id) } },
        )
        if (state.users.isNotEmpty()) {
            DropdownFilterChip(
                label = stringResource(Res.string.chores_assignment),
                optionGroups = listOf(listOf<Int?>(null), state.users.map { it.id }),
                selected = filter.userId,
                optionLabel = { userName(it) },
                selectedLabel = { userName(it) },
                onSelect = { id -> viewModel.setFilter { it.copy(userId = id) } },
            )
        }
        ToggleFilterChip(
            label = stringResource(Res.string.tasks_show_done),
            selected = filter.showDone,
            onSelectedChange = { show -> viewModel.setFilter { it.copy(showDone = show) } },
        )
        SortChip(
            fields = availableTaskSortColumns(state.fields, state.userfieldColumns),
            field = state.sortField,
            ascending = state.ascending,
            isDefault = state.isDefaultSort,
            fieldLabel = { column ->
                column.label(state.userfieldColumns) { stringResource(it.label) }
            },
            onSortChange = viewModel::setSort,
        )
        DropdownFilterChip(
            label = stringResource(Res.string.stock_overview_group_by),
            optionGroups = listOf(state.availableGroupings),
            selected = state.grouping,
            isDefault = { it == TableColumn.Own(TasksOverviewGrouping.NONE) },
            optionLabel = { column ->
                column.label(state.userfieldColumns) { stringResource(it.label) }
            },
            selectedLabel = { column ->
                stringResource(
                    Res.string.stock_overview_grouped_by,
                    column.label(state.userfieldColumns) { stringResource(it.label) },
                )
            },
            onSelect = viewModel::setGrouping,
        )
        MultiSelectChip(
            label = stringResource(Res.string.stock_overview_fields),
            options = state.availableFields,
            checked = state.fields.toSet(),
            optionLabel = { stringResource(it.label) },
            onCheckedChange = viewModel::setFieldShown,
        )
        if (filter.isActive) {
            ClearFilterChip(onClick = viewModel::clearFilter)
        }
    }
}

private fun LazyListScope.taskItems(
    state: TasksUiState,
    refresh: RefreshState,
    isSearching: Boolean,
    rowActions: TaskRowActions,
    selectedTaskId: Int?,
    isBesideDetail: Boolean,
    onRetryClick: () -> Unit,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means there is nothing to do
            hasData = state.hasTasks && (state.filter.isActive || isSearching),
            storedByStart = true,
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    state.groups.forEachIndexed { groupIndex, group ->
        if (group.title != null || group.isUnset) {
            insetItem(key = "group-${group.title}", contentType = "groupTitle") {
                GroupTitle(
                    text = groupTitle(group, state.grouping),
                    // The first group follows the filters' group gap, the others the items
                    modifier = if (groupIndex > 0) {
                        Modifier.padding(top = GroupTitleTopPadding)
                    } else {
                        Modifier
                    },
                )
            }
        }
        insetItems(
            items = group.rows.withIndex().toList(),
            key = { it.value.task.id },
            contentType = { "task" },
        ) { (index, row) ->
            TaskListItem(
                row = row,
                fields = state.fields,
                index = index,
                count = group.rows.size,
                actions = rowActions,
                selected = if (isBesideDetail) row.task.id == selectedTaskId else null,
            )
        }
    }
}

@Composable
private fun groupTitle(group: TaskGroup, grouping: TableColumn<TasksOverviewGrouping>): String {
    val title = group.title
    return when {
        title == null && grouping == TableColumn.Own(TasksOverviewGrouping.CATEGORY) ->
            stringResource(Res.string.task_uncategorized)
        title == null -> stringResource(Res.string.stock_overview_group_not_set)
        grouping == TableColumn.Own(TasksOverviewGrouping.DUE) -> taskDateText(title)
        else -> title
    }
}

// The list puts the segmented gap between its items, the title adds the rest of a group gap
private val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap

private val TaskDueStatus.label: StringResource
    get() = when (this) {
        TaskDueStatus.OVERDUE -> Res.string.chores_status_overdue
        TaskDueStatus.DUE_TODAY -> Res.string.chores_status_due_today
        TaskDueStatus.DUE_SOON -> Res.string.chores_status_due_soon
    }

private val TasksOverviewField.label: StringResource
    get() = when (this) {
        TasksOverviewField.DUE -> Res.string.task_due
        TasksOverviewField.CATEGORY -> Res.string.task_category
        TasksOverviewField.ASSIGNED_TO -> Res.string.chores_assigned_to
        TasksOverviewField.USERFIELDS -> Res.string.product_userfields
    }

private val TasksOverviewSortField.label: StringResource
    get() = when (this) {
        TasksOverviewSortField.NAME -> Res.string.products_sort_name
        TasksOverviewSortField.DUE -> Res.string.task_due
        TasksOverviewSortField.CATEGORY -> Res.string.task_category
        TasksOverviewSortField.ASSIGNED_TO -> Res.string.chores_assigned_to
    }

private val TasksOverviewGrouping.label: StringResource
    get() = when (this) {
        TasksOverviewGrouping.NONE -> Res.string.data_none
        TasksOverviewGrouping.DUE -> Res.string.task_due
        TasksOverviewGrouping.CATEGORY -> Res.string.task_category
        TasksOverviewGrouping.ASSIGNED_TO -> Res.string.chores_assigned_to
    }
