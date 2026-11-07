@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.batteries.impl

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
import com.patrickzedler.grocy.core.forms.label
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewField
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewGrouping
import com.patrickzedler.grocy.core.model.batteries.BatteriesOverviewSortField
import com.patrickzedler.grocy.core.model.batteries.BatteryDueStatus
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.batteries_journal_title
import com.patrickzedler.grocy.core.resources.batteries_last_charged
import com.patrickzedler.grocy.core.resources.batteries_next_charge
import com.patrickzedler.grocy.core.resources.batteries_overview_title
import com.patrickzedler.grocy.core.resources.batteries_status_due_soon
import com.patrickzedler.grocy.core.resources.batteries_status_due_today
import com.patrickzedler.grocy.core.resources.batteries_status_overdue
import com.patrickzedler.grocy.core.resources.battery_tracking_title
import com.patrickzedler.grocy.core.resources.battery_used_in
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.ic_rounded_battery_charging_90
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.masterdata_batteries
import com.patrickzedler.grocy.core.resources.masterdata_batteries_count
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.settings_batteries
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

/** Where the batteries overview leads. */
internal class BatteriesOverviewLinks(
    val onTrackingClick: () -> Unit,
    val onJournalClick: (batteryId: Int?) -> Unit,
    val onEditClick: (batteryId: Int) -> Unit,
    val onSettingsClick: () -> Unit,
    val onManageBatteriesClick: () -> Unit,
)

/**
 * Grocy's "Batteries overview": the active batteries as items with their fields as chips, colored
 * like Grocy's rows, how many below the title, the filter (with the numbers of Grocy's colored
 * messages) and table options as chips, the search. Battery tracking is the toolbar's main action,
 * the journal beside it; a tap opens the battery's overview as dialog.
 *
 * @param isShown whether the list is the screen the user sees, which downloads the batteries again
 *   after a page on top may have changed them.
 */
@Composable
internal fun BatteriesOverviewScreen(
    viewModel: BatteriesOverviewViewModel,
    overview: BatteryOverviewViewModel,
    overviewLinks: BatteryOverviewLinks,
    links: BatteriesOverviewLinks,
    menuItems: List<AppBarMenuItem>,
    selectedBatteryId: Int?,
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
        hasData = state?.hasBatteries == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    BatteryActionsHost(viewModel.actions, snackbarHostState)
    BatteryOverviewHost(overview, overviewLinks, snackbarHostState)
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
        title = stringResource(Res.string.batteries_overview_title),
        subtitle = state?.takeIf { it.hasBatteries }?.summary?.count?.let {
            pluralStringResource(Res.plurals.masterdata_batteries_count, it, it)
        },
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        actionMenuItems = listOf(
            AppBarMenuItem(
                stringResource(Res.string.masterdata_batteries),
                onClick = links.onManageBatteriesClick,
            ),
            AppBarMenuItem(
                stringResource(Res.string.settings_batteries),
                onClick = links.onSettingsClick,
            ),
        ),
        pane = ScreenPane.List.takeIf { isBesideDetail },
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && state?.hasBatteries == true,
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_battery_charging_90),
                fabDescription = stringResource(Res.string.battery_tracking_title),
                onFabClick = links.onTrackingClick,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_receipt_long),
                    contentDescription = stringResource(Res.string.batteries_journal_title),
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
            BatteriesFilterBar(current, viewModel, contentPadding)
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        batteryItems(
            state = current,
            refresh = refresh,
            isSearching = viewModel.searchState.text.isNotBlank(),
            rowActions = BatteryRowActions(
                onClick = { overview.show(it.id, it.name) },
                onTrack = { viewModel.actions.track(it.id, it.name) },
                onJournal = { links.onJournalClick(it.id) },
                onEdit = { links.onEditClick(it.id) },
                onDownloadGrocycode = { battery, twoDimensional ->
                    downloadGrocycode(Grocycode.battery(battery.id), twoDimensional)
                },
                onPrintLabel = { viewModel.actions.printLabel(it.id) },
            ),
            selectedBatteryId = selectedBatteryId.takeIf { isBesideDetail },
            isBesideDetail = isBesideDetail,
            onRetryClick = viewModel::refresh,
        )
    }
}

@Composable
private fun BatteriesFilterBar(
    state: BatteriesOverviewUiState,
    viewModel: BatteriesOverviewViewModel,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    val summary = state.summary
    val statuses = BatteryDueStatus.entries.filter {
        it != BatteryDueStatus.DUE_SOON || state.dueSoonDays > 0
    }
    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_status),
            optionGroups = listOf(listOf<BatteryDueStatus?>(null), statuses),
            selected = state.status,
            optionLabel = { status ->
                status?.let {
                    val count = when (it) {
                        BatteryDueStatus.OVERDUE -> summary.overdue
                        BatteryDueStatus.DUE_TODAY -> summary.dueToday
                        BatteryDueStatus.DUE_SOON -> summary.dueSoon
                    }
                    stringResource(
                        Res.string.shopping_list_list_with_count,
                        stringResource(it.label),
                        count,
                    )
                } ?: all
            },
            selectedLabel = { status -> status?.let { stringResource(it.label) } ?: all },
            onSelect = viewModel::setStatus,
        )
        SortChip(
            fields = availableBatterySortColumns(state.fields, state.userfieldColumns),
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
            isDefault = { it == TableColumn.Own(BatteriesOverviewGrouping.NONE) },
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
        if (state.status != null) {
            ClearFilterChip(onClick = viewModel::clearFilter)
        }
    }
}

private fun LazyListScope.batteryItems(
    state: BatteriesOverviewUiState,
    refresh: RefreshState,
    isSearching: Boolean,
    rowActions: BatteryRowActions,
    selectedBatteryId: Int?,
    isBesideDetail: Boolean,
    onRetryClick: () -> Unit,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means the server has no batteries
            hasData = state.hasBatteries && (state.status != null || isSearching),
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    val style = BatteryRowStyle(
        fields = state.fields,
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
            key = { it.value.battery.id },
            contentType = { "battery" },
        ) { (index, row) ->
            BatteryListItem(
                row = row,
                style = style,
                index = index,
                count = group.rows.size,
                actions = rowActions,
                selected = if (isBesideDetail) row.battery.id == selectedBatteryId else null,
            )
        }
    }
}

// The list puts the segmented gap between its items, the title adds the rest of a group gap
internal val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap

private val BatteryDueStatus.label: StringResource
    get() = when (this) {
        BatteryDueStatus.OVERDUE -> Res.string.batteries_status_overdue
        BatteryDueStatus.DUE_TODAY -> Res.string.batteries_status_due_today
        BatteryDueStatus.DUE_SOON -> Res.string.batteries_status_due_soon
    }

private val BatteriesOverviewField.label: StringResource
    get() = when (this) {
        BatteriesOverviewField.USED_IN -> Res.string.battery_used_in
        BatteriesOverviewField.LAST_CHARGED -> Res.string.batteries_last_charged
        BatteriesOverviewField.NEXT_CHARGE -> Res.string.batteries_next_charge
        BatteriesOverviewField.USERFIELDS -> Res.string.product_userfields
    }

private val BatteriesOverviewSortField.label: StringResource
    get() = when (this) {
        BatteriesOverviewSortField.NAME -> Res.string.products_sort_name
        BatteriesOverviewSortField.USED_IN -> Res.string.battery_used_in
        BatteriesOverviewSortField.LAST_CHARGED -> Res.string.batteries_last_charged
        BatteriesOverviewSortField.NEXT_CHARGE -> Res.string.batteries_next_charge
    }

private val BatteriesOverviewGrouping.label: StringResource
    get() = when (this) {
        BatteriesOverviewGrouping.NONE -> Res.string.data_none
        BatteriesOverviewGrouping.USED_IN -> Res.string.battery_used_in
    }
