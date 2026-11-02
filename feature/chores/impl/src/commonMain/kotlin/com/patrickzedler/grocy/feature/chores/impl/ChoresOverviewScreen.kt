@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.chores.impl

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
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.model.chores.ChoreDueStatus
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewField
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewGrouping
import com.patrickzedler.grocy.core.model.chores.ChoresOverviewSortField
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.chore_tracking_title
import com.patrickzedler.grocy.core.resources.chores_assigned_to
import com.patrickzedler.grocy.core.resources.chores_assigned_to_me
import com.patrickzedler.grocy.core.resources.chores_assignment
import com.patrickzedler.grocy.core.resources.chores_journal_title
import com.patrickzedler.grocy.core.resources.chores_last_tracked
import com.patrickzedler.grocy.core.resources.chores_next_tracking
import com.patrickzedler.grocy.core.resources.chores_overview_title
import com.patrickzedler.grocy.core.resources.chores_status_due_soon
import com.patrickzedler.grocy.core.resources.chores_status_due_today
import com.patrickzedler.grocy.core.resources.chores_status_overdue
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.ic_rounded_play_arrow
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.masterdata_chores
import com.patrickzedler.grocy.core.resources.masterdata_chores_count
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.settings_chores
import com.patrickzedler.grocy.core.resources.shopping_list_list_with_count
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where the chores overview leads. */
internal class ChoresOverviewLinks(
    val onTrackingClick: () -> Unit,
    val onJournalClick: (choreId: Int?) -> Unit,
    val onEditClick: (choreId: Int) -> Unit,
    val onSettingsClick: () -> Unit,
    val onManageChoresClick: () -> Unit,
)

/**
 * Grocy's "Chores overview": the active chores as items with their fields as chips, colored like
 * Grocy's rows, how many below the title, the filters (with the numbers of Grocy's colored
 * messages) and table options as chips, the search. Chore tracking is the toolbar's main action,
 * the journal beside it; a tap opens the chore's overview as dialog.
 *
 * @param isShown whether the list is the screen the user sees, which downloads the chores again
 *   after a page on top may have changed them.
 */
@Composable
internal fun ChoresOverviewScreen(
    viewModel: ChoresOverviewViewModel,
    overview: ChoreOverviewViewModel,
    overviewLinks: ChoreOverviewLinks,
    links: ChoresOverviewLinks,
    menuItems: List<AppBarMenuItem>,
    selectedChoreId: Int?,
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
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasChores == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    ChoreActionsHost(viewModel.actions, state?.users.orEmpty(), snackbarHostState)
    ChoreOverviewHost(overview, overviewLinks, snackbarHostState)
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
        title = stringResource(Res.string.chores_overview_title),
        subtitle = state?.takeIf { it.hasChores }?.summary?.count?.let {
            pluralStringResource(Res.plurals.masterdata_chores_count, it, it)
        },
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        actionMenuItems = listOf(
            AppBarMenuItem(
                stringResource(Res.string.masterdata_chores),
                onClick = links.onManageChoresClick,
            ),
            AppBarMenuItem(
                stringResource(Res.string.settings_chores),
                onClick = links.onSettingsClick,
            ),
        ),
        pane = ScreenPane.List.takeIf { isBesideDetail },
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && state?.hasChores == true,
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_play_arrow),
                fabDescription = stringResource(Res.string.chore_tracking_title),
                onFabClick = links.onTrackingClick,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_receipt_long),
                    contentDescription = stringResource(Res.string.chores_journal_title),
                    onClick = { links.onJournalClick(null) },
                )
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
            ChoresFilterBar(current, viewModel, contentPadding)
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        choreItems(
            state = current,
            refresh = refresh,
            isSearching = viewModel.searchState.text.isNotBlank(),
            rowActions = ChoreRowActions(
                onClick = { overview.show(it.choreId, it.name) },
                onTrack = viewModel.actions::track,
                onSkip = viewModel.actions::skip,
                onReschedule = viewModel.actions::openReschedule,
                onJournal = { links.onJournalClick(it.choreId) },
                onEdit = { links.onEditClick(it.choreId) },
                onDownloadGrocycode = { chore, twoDimensional ->
                    downloadGrocycode(
                        Grocycode.chore(chore.choreId),
                        twoDimensional,
                    )
                },
                onPrintLabel = { viewModel.actions.printLabel(it.choreId) },
            ),
            selectedChoreId = selectedChoreId.takeIf { isBesideDetail },
            isBesideDetail = isBesideDetail,
            onRetryClick = viewModel::refresh,
        )
    }
}

@Composable
private fun ChoresFilterBar(
    state: ChoresOverviewUiState,
    viewModel: ChoresOverviewViewModel,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    val filter = state.filter
    val summary = state.summary
    val me = state.currentUserId
    val statuses = ChoreDueStatus.entries.filter {
        it != ChoreDueStatus.DUE_SOON || state.dueSoonDays > 0
    }
    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_status),
            optionGroups = listOf(listOf<ChoreDueStatus?>(null), statuses),
            selected = filter.status,
            optionLabel = { status ->
                status?.let {
                    val count = when (it) {
                        ChoreDueStatus.OVERDUE -> summary.overdue
                        ChoreDueStatus.DUE_TODAY -> summary.dueToday
                        ChoreDueStatus.DUE_SOON -> summary.dueSoon
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
        if (state.config.choresAssignments && state.users.isNotEmpty()) {
            val others = state.users.filter { it.id != me }
            DropdownFilterChip(
                label = stringResource(Res.string.chores_assignment),
                optionGroups = listOf(
                    listOf<Int?>(null),
                    listOfNotNull(me),
                    others.map { it.id },
                ),
                selected = filter.userId,
                optionLabel = { id ->
                    when (id) {
                        null -> all
                        me -> stringResource(
                            Res.string.shopping_list_list_with_count,
                            stringResource(Res.string.chores_assigned_to_me),
                            summary.assignedToMe,
                        )
                        else -> state.users.firstOrNull { it.id == id }?.displayName.orEmpty()
                    }
                },
                selectedLabel = { id ->
                    when (id) {
                        null -> all
                        me -> stringResource(Res.string.chores_assigned_to_me)
                        else -> state.users.firstOrNull { it.id == id }?.displayName.orEmpty()
                    }
                },
                onSelect = { id -> viewModel.setFilter { it.copy(userId = id) } },
            )
        }
        SortChip(
            fields = availableChoreSortFields(state.fields),
            field = state.sortField,
            ascending = state.ascending,
            isDefault = state.isDefaultSort,
            fieldLabel = { stringResource(it.label) },
            onSortChange = viewModel::setSort,
        )
        if (state.availableGroupings.size > 1) {
            DropdownFilterChip(
                label = stringResource(Res.string.stock_overview_group_by),
                optionGroups = listOf(state.availableGroupings),
                selected = state.grouping,
                isDefault = { it == ChoresOverviewGrouping.NONE },
                optionLabel = { stringResource(it.label) },
                selectedLabel = {
                    stringResource(Res.string.stock_overview_grouped_by, stringResource(it.label))
                },
                onSelect = viewModel::setGrouping,
            )
        }
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

private fun LazyListScope.choreItems(
    state: ChoresOverviewUiState,
    refresh: RefreshState,
    isSearching: Boolean,
    rowActions: ChoreRowActions,
    selectedChoreId: Int?,
    isBesideDetail: Boolean,
    onRetryClick: () -> Unit,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means the server has no chores
            hasData = state.hasChores && (state.filter.isActive || isSearching),
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    val style = ChoreRowStyle(
        fields = state.fields,
        swapsTrackingButtons = state.swapsTrackingButtons,
        grocycode2D = state.config.grocycode2D,
        printsLabels = state.config.labelPrinter && state.config.labelPrinterRunsOnServer,
    )
    state.groups.forEachIndexed { groupIndex, group ->
        if (group.title != null || group.isUnassigned) {
            insetItem(key = "group-${group.title}", contentType = "groupTitle") {
                GroupTitle(
                    text = group.title ?: stringResource(Res.string.stock_overview_group_not_set),
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
            key = { it.value.chore.choreId },
            contentType = { "chore" },
        ) { (index, row) ->
            ChoreListItem(
                row = row,
                style = style,
                index = index,
                count = group.rows.size,
                actions = rowActions,
                selected = if (isBesideDetail) row.chore.choreId == selectedChoreId else null,
            )
        }
    }
}

// The list puts the segmented gap between its items, the title adds the rest of a group gap
internal val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap

private val ChoreDueStatus.label: StringResource
    get() = when (this) {
        ChoreDueStatus.OVERDUE -> Res.string.chores_status_overdue
        ChoreDueStatus.DUE_TODAY -> Res.string.chores_status_due_today
        ChoreDueStatus.DUE_SOON -> Res.string.chores_status_due_soon
    }

private val ChoresOverviewField.label: StringResource
    get() = when (this) {
        ChoresOverviewField.NEXT_ESTIMATED_TRACKING -> Res.string.chores_next_tracking
        ChoresOverviewField.LAST_TRACKED -> Res.string.chores_last_tracked
        ChoresOverviewField.ASSIGNED_TO -> Res.string.chores_assigned_to
        ChoresOverviewField.USERFIELDS -> Res.string.product_userfields
    }

private val ChoresOverviewSortField.label: StringResource
    get() = when (this) {
        ChoresOverviewSortField.NAME -> Res.string.products_sort_name
        ChoresOverviewSortField.NEXT_ESTIMATED_TRACKING -> Res.string.chores_next_tracking
        ChoresOverviewSortField.LAST_TRACKED -> Res.string.chores_last_tracked
        ChoresOverviewSortField.ASSIGNED_TO -> Res.string.chores_assigned_to
    }

private val ChoresOverviewGrouping.label: StringResource
    get() = when (this) {
        ChoresOverviewGrouping.NONE -> Res.string.data_none
        ChoresOverviewGrouping.ASSIGNED_TO -> Res.string.chores_assigned_to
    }
