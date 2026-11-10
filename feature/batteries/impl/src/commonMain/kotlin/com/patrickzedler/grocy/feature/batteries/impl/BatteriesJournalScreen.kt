@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.batteries.impl

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
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
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
import com.patrickzedler.grocy.core.forms.showsPullIndicator
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalField
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesJournalSortField
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.batteries_journal_title
import com.patrickzedler.grocy.core.resources.batteries_undone
import com.patrickzedler.grocy.core.resources.battery_edit
import com.patrickzedler.grocy.core.resources.battery_field
import com.patrickzedler.grocy.core.resources.battery_overview
import com.patrickzedler.grocy.core.resources.battery_undo
import com.patrickzedler.grocy.core.resources.battery_undo_failed
import com.patrickzedler.grocy.core.resources.chore_tracked_time
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_history
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
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Batteries journal": its filters and table options as chips, each charge cycle with
 * Grocy's "Undo charge cycle"; undone ones muted like Grocy's rows. A tap opens the battery's
 * overview as dialog.
 */
@Composable
internal fun BatteriesJournalScreen(
    viewModel: BatteriesJournalViewModel,
    menuItems: List<AppBarMenuItem>,
    isBesideList: Boolean,
    onBackClick: () -> Unit,
    overview: BatteryOverviewViewModel,
    onEditClick: (batteryId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasCycles == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            snackbarHostState.showNewestSnackbar(
                when (event) {
                    BatteriesJournalEvent.Undone -> getString(Res.string.batteries_undone)
                    is BatteriesJournalEvent.Failed ->
                        event.error.actionMessage(Res.string.battery_undo_failed)
                },
            )
        }
    }
    // Its journal is this page, filtered to the battery
    BatteryOverviewHost(
        overview = overview,
        links = BatteryOverviewLinks(
            onJournalClick = { id -> viewModel.setFilter { it.copy(batteryId = id) } },
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
        title = stringResource(Res.string.batteries_journal_title),
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
        isRefreshing = refresh.showsPullIndicator(state?.hasCycles == true),
        onRefresh = viewModel::refresh,
        listState = listState,
        itemSpacing = ListItemDefaults.SegmentedGap,
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            BatteriesJournalFilterBar(current, viewModel, contentPadding)
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        if (current.groups.isEmpty()) {
            emptyListItem(
                hasData = current.hasCycles,
                refresh = refresh,
                emptyText = { stringResource(Res.string.objects_empty) },
                filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
                onRetryClick = viewModel::refresh,
            )
        } else {
            current.groups.forEachIndexed { groupIndex, group ->
                group.title?.let { title ->
                    insetItem(key = "group-$title", contentType = "groupTitle") {
                        GroupTitle(
                            text = title,
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
                    key = { it.value.cycle.id },
                    contentType = { "cycle" },
                ) { (index, item) ->
                    BatteriesJournalListItem(
                        item = item,
                        fields = current.fields,
                        index = index,
                        count = group.rows.size,
                        canUndo = !viewModel.isUndoing,
                        onUndoClick = { viewModel.undo(item.cycle) },
                        onBatteryClick = { overview.show(item.cycle.batteryId, item.batteryName) },
                        onEditClick = { onEditClick(item.cycle.batteryId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteriesJournalFilterBar(
    state: BatteriesJournalUiState,
    viewModel: BatteriesJournalViewModel,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.battery_field),
            options = listOf<Int?>(null) + state.batteries.map { it.id },
            selected = state.filter.batteryId,
            optionLabel = { id ->
                state.batteries.firstOrNull { it.id == id }?.name ?: all
            },
            onSelect = { id -> viewModel.setFilter { it.copy(batteryId = id) } },
        )
        DropdownFilterChip(
            label = stringResource(Res.string.journal_date_range),
            options = BatteriesJournalRanges,
            selected = state.filter.months,
            optionLabel = { months -> rangeLabel(months, all) },
            onSelect = { months -> viewModel.setFilter { it.copy(months = months) } },
            isDefault = { it == BATTERIES_JOURNAL_DEFAULT_MONTHS },
        )
        SortChip(
            fields = availableBatteriesJournalSortFields(state.fields),
            field = state.sortField,
            ascending = state.ascending,
            isDefault = state.isDefaultSort,
            fieldLabel = { stringResource(it.label) },
            onSortChange = viewModel::setSort,
        )
        DropdownFilterChip(
            label = stringResource(Res.string.stock_overview_group_by),
            optionGroups = listOf(BatteriesJournalGrouping.entries),
            selected = state.grouping,
            isDefault = { it == BatteriesJournalGrouping.NONE },
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

private val BatteriesJournalField.label: StringResource
    get() = when (this) {
        BatteriesJournalField.TRACKED_TIME -> Res.string.chore_tracked_time
        BatteriesJournalField.USERFIELDS -> Res.string.product_userfields
    }

private val BatteriesJournalSortField.label: StringResource
    get() = when (this) {
        BatteriesJournalSortField.BATTERY -> Res.string.battery_field
        BatteriesJournalSortField.TRACKED_TIME -> Res.string.chore_tracked_time
    }

private val BatteriesJournalGrouping.label: StringResource
    get() = when (this) {
        BatteriesJournalGrouping.NONE -> Res.string.data_none
        BatteriesJournalGrouping.BATTERY -> Res.string.battery_field
    }

/** Grocy's ranges: "1 month", "6 months", "1 year", "2 years", "All". */
@Composable
private fun rangeLabel(months: Int, all: String): String = when {
    months >= BATTERIES_JOURNAL_ALL_MONTHS -> all
    months % 12 == 0 -> pluralStringResource(Res.plurals.journal_years, months / 12, months / 12)
    else -> pluralStringResource(Res.plurals.journal_months, months, months)
}

@Composable
private fun BatteriesJournalListItem(
    item: BatteriesJournalItem,
    fields: List<BatteriesJournalField>,
    index: Int,
    count: Int,
    canUndo: Boolean,
    onUndoClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val cycle = item.cycle
    // Undone charge cycles are muted and struck through like Grocy's rows
    val contentAlpha = if (cycle.undone) DisabledAlpha else 1f
    val chips = journalChips(item, fields)

    ListDetailItem(
        selected = null,
        onClick = onBatteryClick,
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
                        description = stringResource(Res.string.battery_undo),
                        enabled = !cycle.undone && canUndo,
                        onClick = onUndoClick,
                    ),
                ),
                menuGroups = listOf(
                    listOf(
                        AppBarMenuItem(
                            stringResource(Res.string.battery_overview),
                            onClick = onBatteryClick,
                        ),
                        AppBarMenuItem(
                            stringResource(Res.string.battery_edit),
                            onClick = onEditClick,
                        ),
                    ),
                ),
            )
        },
    ) {
        Text(
            text = item.batteryName,
            modifier = Modifier.alpha(contentAlpha),
            textDecoration = if (cycle.undone) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun journalChips(
    item: BatteriesJournalItem,
    fields: List<BatteriesJournalField>,
): List<FieldChip> {
    val cycle = item.cycle
    return buildList {
        if (BatteriesJournalField.TRACKED_TIME in fields) {
            val tracked = dateTimeText(cycle.trackedTime)
            add(
                FieldChip(
                    text = tracked,
                    description = labelled(stringResource(Res.string.chore_tracked_time), tracked),
                    icon = Res.drawable.ic_rounded_history,
                ),
            )
        }
        if (BatteriesJournalField.USERFIELDS in fields) {
            item.userfields.forEach { (caption, value) ->
                add(FieldChip(value, labelled(caption, value), Res.drawable.ic_rounded_dataset))
            }
        }
        cycle.undoneTimestamp?.takeIf { cycle.undone }?.let { time ->
            val text = stringResource(Res.string.journal_undone_on, dateTimeText(time))
            add(FieldChip(text, text, Res.drawable.ic_rounded_undo))
        }
    }
}
