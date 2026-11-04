@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.chores.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.design.component.AppBarAction
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.GroupTitle
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.design.component.rememberStatusColors
import com.patrickzedler.grocy.core.forms.ClearFilterChip
import com.patrickzedler.grocy.core.forms.dateTimeText
import com.patrickzedler.grocy.core.forms.DisabledAlpha
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.ItemButton
import com.patrickzedler.grocy.core.forms.ItemButtons
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.MultiSelectChip
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.model.chores.ChoresJournalField
import com.patrickzedler.grocy.core.model.chores.ChoresJournalGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresJournalSortField
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.chore_done_by
import com.patrickzedler.grocy.core.resources.chore_edit
import com.patrickzedler.grocy.core.resources.chore_field
import com.patrickzedler.grocy.core.resources.chore_overview
import com.patrickzedler.grocy.core.resources.chore_scheduled_time
import com.patrickzedler.grocy.core.resources.chore_skipped
import com.patrickzedler.grocy.core.resources.chore_time_of_tracking
import com.patrickzedler.grocy.core.resources.chore_tracked_late
import com.patrickzedler.grocy.core.resources.chore_tracked_time
import com.patrickzedler.grocy.core.resources.chore_undo
import com.patrickzedler.grocy.core.resources.chore_undo_failed
import com.patrickzedler.grocy.core.resources.chore_unknown
import com.patrickzedler.grocy.core.resources.chores_journal_title
import com.patrickzedler.grocy.core.resources.chores_undone
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_fast_forward
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import com.patrickzedler.grocy.core.resources.ic_rounded_person
import com.patrickzedler.grocy.core.resources.ic_rounded_schedule
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.ic_rounded_undo
import com.patrickzedler.grocy.core.resources.journal_date_range
import com.patrickzedler.grocy.core.resources.journal_months
import com.patrickzedler.grocy.core.resources.journal_undone_on
import com.patrickzedler.grocy.core.resources.journal_years
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Chores journal": its filters and table options as chips, each execution with Grocy's
 * "Undo chore execution"; skipped ones say so, late ones are red and undone ones muted like
 * Grocy's rows. A tap opens the chore's overview as dialog.
 */
@Composable
internal fun ChoresJournalScreen(
    viewModel: ChoresJournalViewModel,
    menuItems: List<AppBarMenuItem>,
    isBesideList: Boolean,
    onBackClick: () -> Unit,
    overview: ChoreOverviewViewModel,
    onEditClick: (choreId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasExecutions == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    ChoresJournalEvent.Undone -> getString(Res.string.chores_undone)
                    is ChoresJournalEvent.Failed ->
                        event.error.actionMessage(Res.string.chore_undo_failed)
                },
            )
        }
    }
    // Its journal is this page, filtered to the chore
    ChoreOverviewHost(
        overview = overview,
        links = ChoreOverviewLinks(
            onJournalClick = { id -> viewModel.setFilter { it.copy(choreId = id) } },
            onEditClick = onEditClick,
        ),
        snackbarHostState = snackbarHostState,
    )
    // Back closes the search first and stays on the page
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.isSearchVisible,
        onBackCompleted = viewModel::hideSearch,
    )

    SegmentedListScreen(
        title = stringResource(Res.string.chores_journal_title),
        modifier = modifier,
        navigationIcon = { if (!isBesideList) NavigateBackButton(onClick = onBackClick) },
        actions = listOf(
            AppBarAction { actionModifier, interactionSource ->
                AppBarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = {
                        if (viewModel.isSearchVisible) {
                            viewModel.hideSearch()
                        } else {
                            scope.showSearchAtTop(listState, viewModel::showSearch)
                        }
                    },
                    modifier = actionModifier,
                    interactionSource = interactionSource,
                )
            },
        ),
        menuItems = menuItems,
        pane = ScreenPane.Detail.takeIf { isBesideList },
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing && state?.hasExecutions == true,
        onRefresh = viewModel::refresh,
        listState = listState,
        itemSpacing = ListItemDefaults.SegmentedGap,
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            ChoresJournalFilterBar(current, viewModel, contentPadding)
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        if (current.groups.isEmpty()) {
            emptyListItem(
                hasData = current.hasExecutions,
                refresh = refresh,
                emptyText = { stringResource(Res.string.objects_empty) },
                filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
                onRetryClick = viewModel::refresh,
            )
        } else {
            current.groups.forEachIndexed { groupIndex, group ->
                if (group.title != null || group.isUnknownUser) {
                    insetItem(key = "group-${group.title}", contentType = "groupTitle") {
                        GroupTitle(
                            text = group.title ?: stringResource(Res.string.chore_unknown),
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
                    key = { it.value.execution.id },
                    contentType = { "execution" },
                ) { (index, item) ->
                    ChoresJournalListItem(
                        item = item,
                        fields = current.fields,
                        index = index,
                        count = group.rows.size,
                        canUndo = !viewModel.isUndoing,
                        onUndoClick = { viewModel.undo(item.execution) },
                        onChoreClick = { overview.show(item.execution.choreId, item.choreName) },
                        onEditClick = { onEditClick(item.execution.choreId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoresJournalFilterBar(
    state: ChoresJournalUiState,
    viewModel: ChoresJournalViewModel,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.chore_field),
            options = listOf<Int?>(null) + state.chores.map { it.id },
            selected = state.filter.choreId,
            optionLabel = { id ->
                state.chores.firstOrNull { it.id == id }?.name ?: all
            },
            onSelect = { id -> viewModel.setFilter { it.copy(choreId = id) } },
        )
        DropdownFilterChip(
            label = stringResource(Res.string.journal_date_range),
            options = ChoresJournalRanges,
            selected = state.filter.months,
            optionLabel = { months -> rangeLabel(months, all) },
            onSelect = { months -> viewModel.setFilter { it.copy(months = months) } },
            isDefault = { it == CHORES_JOURNAL_DEFAULT_MONTHS },
        )
        SortChip(
            fields = availableChoresJournalSortFields(state.fields),
            field = state.sortField,
            ascending = state.ascending,
            isDefault = state.isDefaultSort,
            fieldLabel = { stringResource(it.label) },
            onSortChange = viewModel::setSort,
        )
        DropdownFilterChip(
            label = stringResource(Res.string.stock_overview_group_by),
            optionGroups = listOf(state.availableGroupings),
            selected = state.grouping,
            isDefault = { it == ChoresJournalGrouping.NONE },
            optionLabel = { stringResource(it.label) },
            selectedLabel = {
                stringResource(Res.string.stock_overview_grouped_by, stringResource(it.label))
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
        if (state.filter.isActive) {
            ClearFilterChip(onClick = viewModel::clearFilter)
        }
    }
}

private val ChoresJournalField.label: StringResource
    get() = when (this) {
        ChoresJournalField.TRACKED_TIME -> Res.string.chore_tracked_time
        ChoresJournalField.SCHEDULED_TRACKING_TIME -> Res.string.chore_scheduled_time
        ChoresJournalField.TIME_OF_TRACKING -> Res.string.chore_time_of_tracking
        ChoresJournalField.DONE_BY -> Res.string.chore_done_by
        ChoresJournalField.USERFIELDS -> Res.string.product_userfields
    }

private val ChoresJournalSortField.label: StringResource
    get() = when (this) {
        ChoresJournalSortField.CHORE -> Res.string.chore_field
        ChoresJournalSortField.TRACKED_TIME -> Res.string.chore_tracked_time
        ChoresJournalSortField.SCHEDULED_TRACKING_TIME -> Res.string.chore_scheduled_time
        ChoresJournalSortField.TIME_OF_TRACKING -> Res.string.chore_time_of_tracking
        ChoresJournalSortField.DONE_BY -> Res.string.chore_done_by
    }

private val ChoresJournalGrouping.label: StringResource
    get() = when (this) {
        ChoresJournalGrouping.NONE -> Res.string.data_none
        ChoresJournalGrouping.CHORE -> Res.string.chore_field
        ChoresJournalGrouping.DONE_BY -> Res.string.chore_done_by
    }

/** Grocy's ranges: "1 month", "6 months", "1 year", "2 years", "All". */
@Composable
private fun rangeLabel(months: Int, all: String): String = when {
    months >= CHORES_JOURNAL_ALL_MONTHS -> all
    months % 12 == 0 -> pluralStringResource(Res.plurals.journal_years, months / 12, months / 12)
    else -> pluralStringResource(Res.plurals.journal_months, months, months)
}

@Composable
private fun ChoresJournalListItem(
    item: ChoresJournalItem,
    fields: List<ChoresJournalField>,
    index: Int,
    count: Int,
    canUndo: Boolean,
    onUndoClick: () -> Unit,
    onChoreClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val execution = item.execution
    // Undone executions are muted and struck through like Grocy's rows
    val contentAlpha = if (execution.undone) DisabledAlpha else 1f
    val chips = journalChips(item, fields)

    ListDetailItem(
        selected = null,
        onClick = onChoreClick,
        index = index,
        count = count,
        modifier = Modifier.fillMaxWidth(),
        supportingContent = {
            key(chips.map { it.text }) {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp).alpha(contentAlpha),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { FieldChipView(it) }
                }
            }
        },
        trailingContent = {
            ItemButtons(
                buttons = listOf(
                    ItemButton(
                        painter = painterResource(Res.drawable.ic_rounded_undo),
                        description = stringResource(Res.string.chore_undo),
                        enabled = !execution.undone && canUndo,
                        onClick = onUndoClick,
                    ),
                ),
                menuGroups = listOf(
                    listOf(
                        AppBarMenuItem(
                            stringResource(Res.string.chore_overview),
                            onClick = onChoreClick,
                        ),
                        AppBarMenuItem(
                            stringResource(Res.string.chore_edit),
                            onClick = onEditClick,
                        ),
                    ),
                ),
            )
        },
    ) {
        Text(
            text = item.choreName,
            modifier = Modifier.alpha(contentAlpha),
            textDecoration = if (execution.undone) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun journalChips(
    item: ChoresJournalItem,
    fields: List<ChoresJournalField>,
): List<FieldChip> {
    val execution = item.execution
    val dateOnly = item.trackDateOnly
    val critical = rememberStatusColors().critical
    val trackedLabel = stringResource(Res.string.chore_tracked_time)
    val tracked = dateTimeText(execution.trackedTime, dateOnly)
    val late = stringResource(Res.string.chore_tracked_late)
    return buildList {
        if (ChoresJournalField.TRACKED_TIME in fields) {
            add(
                FieldChip(
                    text = tracked,
                    description = labelled(
                        trackedLabel,
                        if (execution.isLate) "$tracked, $late" else tracked,
                    ),
                    icon = Res.drawable.ic_rounded_history,
                    status = critical.takeIf { execution.isLate },
                ),
            )
            if (execution.skipped) {
                val skipped = stringResource(Res.string.chore_skipped)
                add(FieldChip(skipped, skipped, Res.drawable.ic_rounded_fast_forward))
            }
        }
        execution.scheduledExecutionTime
            ?.takeIf { ChoresJournalField.SCHEDULED_TRACKING_TIME in fields }
            ?.let { scheduled ->
                val text = dateTimeText(scheduled, dateOnly)
                add(
                    FieldChip(
                        text = text,
                        description = labelled(
                            stringResource(Res.string.chore_scheduled_time),
                            text,
                        ),
                        icon = Res.drawable.ic_rounded_event,
                    ),
                )
            }
        execution.createdTimestamp
            ?.takeIf { ChoresJournalField.TIME_OF_TRACKING in fields }
            ?.let { created ->
                // When it was entered is always a moment, also for chores tracked by date
                val text = dateTimeText(created, dateOnly = false)
                add(
                    FieldChip(
                        text = text,
                        description = labelled(
                            stringResource(Res.string.chore_time_of_tracking),
                            text,
                        ),
                        icon = Res.drawable.ic_rounded_schedule,
                    ),
                )
            }
        if (ChoresJournalField.DONE_BY in fields) {
            val name = item.userName ?: stringResource(Res.string.chore_unknown)
            add(
                FieldChip(
                    text = name,
                    description = labelled(stringResource(Res.string.chore_done_by), name),
                    icon = Res.drawable.ic_rounded_person,
                ),
            )
        }
        if (ChoresJournalField.USERFIELDS in fields) {
            item.userfields.forEach { (caption, value) ->
                add(FieldChip(value, labelled(caption, value), Res.drawable.ic_rounded_dataset))
            }
        }
        execution.undoneTimestamp?.takeIf { execution.undone }?.let { time ->
            val text = stringResource(
                Res.string.journal_undone_on,
                dateTimeText(time, dateOnly = false),
            )
            add(FieldChip(text, text, Res.drawable.ic_rounded_undo))
        }
    }
}
