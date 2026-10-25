@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.objects_column_entity
import com.patrickzedler.grocy.core.resources.objects_configure_fields
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.resources.products_disabled
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One of Grocy's master data lists like its table: an item per object, filters, sort order and
 * search above them, the actions of the page in the floating toolbar.
 *
 * @param selectedId the object shown beside the list, marked in it while [isBesideDetail].
 * @param objectActions the actions of each item's overflow menu; [onActionDone] gets the ones
 *   that succeeded.
 * @param onConfigureUserfieldsClick opens the userfields of [entity], the objects' own or those
 *   of a userentity.
 */
@Composable
internal fun ObjectListScreen(
    menuItems: List<AppBarMenuItem>,
    viewModel: ObjectListViewModel,
    objectActions: ObjectActionsViewModel,
    selectedId: Int?,
    isBesideDetail: Boolean,
    onBackClick: () -> Unit,
    onObjectClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onConfigureUserfieldsClick: (entity: String) -> Unit,
    onActionDone: (ObjectActionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // With saved data a failed download is only a note, without it the list explains itself
    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasObjects == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    // Back closes the search first and stays on the list
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.isSearchVisible,
        onBackCompleted = viewModel::hideSearch,
    )
    ObjectActionsHost(objectActions, snackbarHostState, onActionDone)

    ObjectListContent(
        kind = viewModel.kind,
        state = state,
        refresh = refresh,
        defaultSort = viewModel.defaultSort,
        selectedId = selectedId.takeIf { isBesideDetail },
        isBesideDetail = isBesideDetail,
        searchState = viewModel.searchState,
        isSearchVisible = viewModel.isSearchVisible,
        snackbarHostState = snackbarHostState,
        menuItems = menuItems,
        actions = ObjectListActions(
            onBackClick = onBackClick,
            onObjectClick = onObjectClick,
            onAddClick = onAddClick,
            onConfigureUserfieldsClick = onConfigureUserfieldsClick,
            onSearchClick = viewModel::showSearch,
            onCloseSearchClick = viewModel::hideSearch,
            onFilterChange = viewModel::setFilter,
            onSortChange = viewModel::setSort,
            onClearFilterClick = viewModel::clearFilter,
            onRefresh = viewModel::refresh,
            objectActions = objectActions,
        ),
        modifier = modifier,
    )
}

private class ObjectListActions(
    val onBackClick: () -> Unit,
    val onObjectClick: (Int) -> Unit,
    val onAddClick: () -> Unit,
    val onConfigureUserfieldsClick: (entity: String) -> Unit,
    val onSearchClick: () -> Unit,
    val onCloseSearchClick: () -> Unit,
    val onFilterChange: ((ObjectFilter) -> ObjectFilter) -> Unit,
    val onSortChange: (ObjectSort) -> Unit,
    val onClearFilterClick: () -> Unit,
    val onRefresh: () -> Unit,
    val objectActions: ObjectActionsViewModel,
)

@Composable
private fun ObjectListContent(
    kind: ObjectListKind,
    state: ObjectListUiState?,
    refresh: RefreshState,
    defaultSort: ObjectSort,
    selectedId: Int?,
    isBesideDetail: Boolean,
    searchState: TextFieldState,
    isSearchVisible: Boolean,
    snackbarHostState: SnackbarHostState,
    menuItems: List<AppBarMenuItem>,
    actions: ObjectListActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val userfieldEntity = kind.userfieldEntity

    SegmentedListScreen(
        title = stringResource(kind.category.title),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = actions.onBackClick) },
        pane = ScreenPane.List.takeIf { isBesideDetail },
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && state?.hasObjects == true,
        onRefresh = actions.onRefresh,
        // Every object is an item of one large group, the group spacing follows the filters
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.action_add),
                onFabClick = actions.onAddClick,
            ) {
                if (userfieldEntity != null) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                        contentDescription =
                            stringResource(Res.string.products_configure_userfields),
                        onClick = { actions.onConfigureUserfieldsClick(userfieldEntity) },
                    )
                }
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = {
                        actions.onSearchClick()
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
            }
        },
    ) {
        if (state == null) return@SegmentedListScreen
        // The chips lie on the background and scroll sideways up to the screen's edges
        edgeToEdgeItem(key = "filters") { contentPadding ->
            ObjectFilterBar(
                kind = kind,
                state = state,
                defaultSort = defaultSort,
                actions = actions,
                contentPadding = contentPadding,
            )
        }
        searchItem(
            isVisible = isSearchVisible,
            state = searchState,
            onCloseClick = actions.onCloseSearchClick,
        )
        objectItems(kind, state, refresh, selectedId, isBesideDetail, actions)
    }
}

@Composable
private fun ObjectFilterBar(
    kind: ObjectListKind,
    state: ObjectListUiState,
    defaultSort: ObjectSort,
    actions: ObjectListActions,
    contentPadding: PaddingValues,
) {
    val filter = state.filter
    FilterChipRow(contentPadding = contentPadding) {
        if (kind == ObjectListKind.USERFIELDS) {
            val all = stringResource(Res.string.data_all)
            DropdownFilterChip(
                label = stringResource(Res.string.objects_column_entity),
                // A filter given by a link stays choosable even before the download
                options = listOf<String?>(null) + (state.entities + listOfNotNull(filter.entity))
                    .distinct().sorted(),
                selected = filter.entity,
                optionLabel = { it ?: all },
                onSelect = { entity -> actions.onFilterChange { it.copy(entity = entity) } },
            )
        }
        if (kind.hasActive) {
            ShowDisabledChip(
                selected = filter.showDisabled,
                onSelectedChange = { show ->
                    actions.onFilterChange { it.copy(showDisabled = show) }
                },
            )
        }
        SortChip(
            fields = kind.columns,
            field = state.sort.column,
            ascending = state.sort.ascending,
            isDefault = state.sort == defaultSort,
            fieldLabel = { stringResource(it.label) },
            onSortChange = { column, ascending ->
                actions.onSortChange(ObjectSort(column, ascending))
            },
        )
        if (filter.isActive) {
            ClearFilterChip(onClick = actions.onClearFilterClick)
        }
    }
}

private fun LazyListScope.objectItems(
    kind: ObjectListKind,
    state: ObjectListUiState,
    refresh: RefreshState,
    selectedId: Int?,
    isBesideDetail: Boolean,
    actions: ObjectListActions,
) {
    if (state.rows.isEmpty()) {
        emptyListItem(
            hasData = state.hasObjects,
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = actions.onRefresh,
        )
        return
    }
    insetItems(
        items = state.rows.withIndex().toList(),
        key = { it.value.id },
        contentType = { "object" },
    ) { (index, row) ->
        ObjectItem(
            kind = kind,
            row = row,
            selected = if (isBesideDetail) row.id == selectedId else null,
            index = index,
            count = state.rows.size,
            onClick = { actions.onObjectClick(row.id) },
            menuItems = objectActionItems(actions.objectActions, row.id),
            onConfigureFieldsClick = row.entity
                ?.takeIf { kind == ObjectListKind.USERENTITIES }
                ?.let { entity -> { actions.onConfigureUserfieldsClick(entity) } },
        )
    }
}

@Composable
private fun ObjectItem(
    kind: ObjectListKind,
    row: ObjectRow,
    selected: Boolean?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    menuItems: List<AppBarMenuItem>,
    onConfigureFieldsClick: (() -> Unit)?,
) {
    val contentAlpha = if (row.active) 1f else DisabledAlpha
    val supporting = kind.supporting.mapNotNull { columns ->
        columns.mapNotNull { row.texts[it] }.joinToString(" · ").ifEmpty { null }
    }

    ListDetailItem(
        selected = selected,
        onClick = onClick,
        index = index,
        count = count,
        modifier = Modifier.fillMaxWidth(),
        overlineContent = if (!row.active) {
            { Text(stringResource(Res.string.products_disabled)) }
        } else {
            null
        },
        supportingContent = if (supporting.isNotEmpty()) {
            { SupportingLines(lines = supporting, modifier = Modifier.alpha(contentAlpha)) }
        } else {
            null
        },
        // Buttons bring enough space of their own, unlike icons and switches
        trailingContent = {
            if (onConfigureFieldsClick != null) {
                UserentityButtons(onConfigureFieldsClick, menuItems)
            } else {
                ListItemOverflowMenu(groups = listOf(menuItems))
            }
        },
    ) {
        Text(
            text = row.texts[kind.headline] ?: row.texts[ObjectColumn.NAME].orEmpty(),
            modifier = Modifier.alpha(contentAlpha),
        )
    }
}

/**
 * Grocy's "Configure fields" of a userentity and the item's menu as a standard button group,
 * narrow buttons, so the item keeps room for its name.
 */
@Composable
private fun UserentityButtons(
    onConfigureFieldsClick: () -> Unit,
    menuItems: List<AppBarMenuItem>,
) {
    val haptic = LocalHapticFeedback.current
    val configureSource = remember { MutableInteractionSource() }
    val menuSource = remember { MutableInteractionSource() }
    val configureText = stringResource(Res.string.objects_configure_fields)
    val onConfigure = {
        haptic.confirm()
        onConfigureFieldsClick()
    }

    ButtonGroup(
        // Only where the item is too narrow for both buttons
        overflowIndicator = { menuState ->
            NarrowIconButton(
                painter = painterResource(Res.drawable.ic_rounded_more_vert),
                description = stringResource(Res.string.action_more_options),
                onClick = { if (menuState.isShowing) menuState.dismiss() else menuState.show() },
            )
        },
        horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement,
    ) {
        customItem(
            buttonGroupContent = {
                NarrowIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                    description = configureText,
                    onClick = onConfigureFieldsClick,
                    modifier = Modifier.animateWidth(configureSource),
                    interactionSource = configureSource,
                )
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(configureText) },
                    modifier = Modifier.clickCursor(),
                    onClick = {
                        menuState.dismiss()
                        onConfigure()
                    },
                )
            },
        )
        customItem(
            buttonGroupContent = {
                ListItemOverflowMenu(
                    groups = listOf(menuItems),
                    modifier = Modifier.animateWidth(menuSource),
                    interactionSource = menuSource,
                )
            },
            menuContent = { menuState ->
                menuItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.text) },
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            menuState.dismiss()
                            item.onClick()
                        },
                    )
                }
            },
        )
    }
}

/** A standard narrow icon button with its tooltip. */
@Composable
private fun NarrowIconButton(
    painter: Painter,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(text = description, modifier = modifier) {
        IconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(
                    IconButtonDefaults.smallContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Narrow,
                    ),
                )
                .clickCursor(),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painter, contentDescription = description)
        }
    }
}
