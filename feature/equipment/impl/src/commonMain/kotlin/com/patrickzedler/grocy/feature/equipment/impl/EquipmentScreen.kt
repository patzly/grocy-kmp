@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.patrickzedler.grocy.feature.equipment.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import com.patrickzedler.grocy.core.design.component.SwipeAction
import com.patrickzedler.grocy.core.design.component.SwipeActionBox
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FieldChip
import com.patrickzedler.grocy.core.forms.FieldChipView
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.ItemButton
import com.patrickzedler.grocy.core.forms.ItemButtons
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.MultiSelectChip
import com.patrickzedler.grocy.core.forms.ObjectAction
import com.patrickzedler.grocy.core.forms.ObjectActionEvent
import com.patrickzedler.grocy.core.forms.ObjectActionsHost
import com.patrickzedler.grocy.core.forms.ObjectActionsViewModel
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.label
import com.patrickzedler.grocy.core.forms.labelled
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.forms.showsPullIndicator
import com.patrickzedler.grocy.core.model.equipment.Equipment
import com.patrickzedler.grocy.core.model.equipment.EquipmentField
import com.patrickzedler.grocy.core.model.equipment.EquipmentGrouping
import com.patrickzedler.grocy.core.model.equipment.EquipmentSortField
import com.patrickzedler.grocy.core.model.masterdata.TableColumn
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.equipment_count
import com.patrickzedler.grocy.core.resources.equipment_create
import com.patrickzedler.grocy.core.resources.equipment_edit
import com.patrickzedler.grocy.core.resources.equipment_title
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_dataset
import com.patrickzedler.grocy.core.resources.ic_rounded_delete
import com.patrickzedler.grocy.core.resources.ic_rounded_edit
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where the equipment list leads. */
internal class EquipmentLinks(
    val onCreateClick: () -> Unit,
    val onEquipmentClick: (id: Int) -> Unit,
    val onEditClick: (id: Int) -> Unit,
    val onEquipmentDeleted: (id: Int) -> Unit,
)

/**
 * Grocy's equipment table: every piece as item with its userfields as chips, how many below the
 * title, the table options as chips, the search. "Add" is the toolbar's main action; a tap shows
 * manual and notes of the piece, beside the list where the window is wide enough.
 *
 * @param isShown whether the list is the screen the user sees, which downloads the equipment again
 *   after a page on top may have changed it.
 */
@Composable
internal fun EquipmentScreen(
    viewModel: EquipmentViewModel,
    objectActions: ObjectActionsViewModel,
    links: EquipmentLinks,
    menuItems: List<AppBarMenuItem>,
    selectedId: Int?,
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
        hasData = state?.hasEquipment == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    ObjectActionsHost(objectActions, snackbarHostState) { event ->
        if (event is ObjectActionEvent.Deleted) links.onEquipmentDeleted(event.id)
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
        title = stringResource(Res.string.equipment_title),
        subtitle = state?.takeIf { it.hasEquipment }?.count?.let {
            pluralStringResource(Res.plurals.equipment_count, it, it)
        },
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        pane = ScreenPane.List.takeIf { isBesideDetail },
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.showsPullIndicator(
            state?.hasEquipment == true,
            storedByStart = true,
        ),
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.equipment_create),
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
        // Without userfield columns Grocy's table has nothing to set up but the name
        if (current.userfieldColumns.isNotEmpty()) {
            edgeToEdgeItem(key = "filters") { contentPadding ->
                EquipmentFilterBar(current, viewModel, contentPadding)
            }
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        equipmentItems(
            state = current,
            refresh = refresh,
            isSearching = viewModel.searchState.text.isNotBlank(),
            links = links,
            onDelete = { objectActions.request(ObjectAction.DELETE, it.id) },
            selectedId = selectedId.takeIf { isBesideDetail },
            isBesideDetail = isBesideDetail,
            onRetryClick = viewModel::refresh,
        )
    }
}

@Composable
private fun EquipmentFilterBar(
    state: EquipmentUiState,
    viewModel: EquipmentViewModel,
    contentPadding: PaddingValues,
) {
    FilterChipRow(contentPadding = contentPadding) {
        SortChip(
            fields = availableEquipmentSortColumns(state.fields, state.userfieldColumns),
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
            optionGroups = listOf(availableEquipmentGroupings(state.userfieldColumns)),
            selected = state.grouping,
            isDefault = { it == TableColumn.Own(EquipmentGrouping.NONE) },
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
            options = EquipmentField.entries.toList(),
            checked = state.fields.toSet(),
            optionLabel = { stringResource(Res.string.product_userfields) },
            onCheckedChange = viewModel::setFieldShown,
        )
    }
}

private fun LazyListScope.equipmentItems(
    state: EquipmentUiState,
    refresh: RefreshState,
    isSearching: Boolean,
    links: EquipmentLinks,
    onDelete: (Equipment) -> Unit,
    selectedId: Int?,
    isBesideDetail: Boolean,
    onRetryClick: () -> Unit,
) {
    if (state.groups.isEmpty()) {
        emptyListItem(
            hasData = state.hasEquipment && isSearching,
            storedByStart = true,
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
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
            key = { it.value.equipment.id },
            contentType = { "equipment" },
        ) { (index, row) ->
            EquipmentListItem(
                row = row,
                showsUserfields = EquipmentField.USERFIELDS in state.fields,
                index = index,
                count = group.rows.size,
                links = links,
                onDelete = onDelete,
                selected = if (isBesideDetail) row.equipment.id == selectedId else null,
            )
        }
    }
}

/**
 * A piece of equipment: its name, its userfields as chips. Grocy's "Edit this item" stays on the
 * row, "Delete this item" in the menu; swiping towards the start deletes as well.
 */
@Composable
private fun EquipmentListItem(
    row: EquipmentRow,
    showsUserfields: Boolean,
    index: Int,
    count: Int,
    links: EquipmentLinks,
    onDelete: (Equipment) -> Unit,
    selected: Boolean?,
) {
    val equipment = row.equipment
    val deleteText = stringResource(Res.string.action_delete)
    val colors = MaterialTheme.colorScheme
    val chips = if (showsUserfields) {
        row.userfields.map { (caption, value) ->
            FieldChip(value, labelled(caption, value), Res.drawable.ic_rounded_dataset)
        }
    } else {
        emptyList()
    }

    SwipeActionBox(
        startAction = null,
        endAction = SwipeAction(
            icon = painterResource(Res.drawable.ic_rounded_delete),
            containerColor = colors.error,
            contentColor = colors.onError,
        ) { onDelete(equipment) },
    ) {
        ListDetailItem(
            selected = selected,
            onClick = { links.onEquipmentClick(equipment.id) },
            index = index,
            count = count,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(deleteText) {
                            onDelete(equipment)
                            true
                        },
                    )
                },
            supportingContent = if (chips.isEmpty()) {
                null
            } else {
                {
                    key(chips.map { it.text }) {
                        FlowRow(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            chips.forEach { FieldChipView(it) }
                        }
                    }
                }
            },
            trailingContent = {
                ItemButtons(
                    buttons = listOf(
                        ItemButton(
                            painter = painterResource(Res.drawable.ic_rounded_edit),
                            description = stringResource(Res.string.equipment_edit),
                        ) { links.onEditClick(equipment.id) },
                    ),
                    menuGroups = listOf(
                        listOf(AppBarMenuItem(deleteText) { onDelete(equipment) }),
                    ),
                )
            },
        ) {
            Text(equipment.name)
        }
    }
}

// The list puts the segmented gap between its items, the title adds the rest of a group gap
private val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap

private val EquipmentSortField.label: StringResource
    get() = when (this) {
        EquipmentSortField.NAME -> Res.string.products_sort_name
    }

private val EquipmentGrouping.label: StringResource
    get() = when (this) {
        EquipmentGrouping.NONE -> Res.string.data_none
    }
