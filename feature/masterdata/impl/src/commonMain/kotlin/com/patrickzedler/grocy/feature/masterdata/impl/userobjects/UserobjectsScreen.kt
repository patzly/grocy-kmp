@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.userobjects

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.ObjectActionEvent
import com.patrickzedler.grocy.core.forms.ObjectActionsHost
import com.patrickzedler.grocy.core.forms.ObjectActionsViewModel
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.objectActionItems
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.objects_configure_fields
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's page of a userentity: its caption and description, an item per object with Grocy's
 * columns, the fields to sort by, the search; adding and "Configure fields" in the toolbar.
 */
@Composable
internal fun UserobjectsScreen(
    viewModel: UserobjectsViewModel,
    objectActions: ObjectActionsViewModel,
    menuItems: List<AppBarMenuItem>,
    selectedId: Int?,
    isBesideDetail: Boolean,
    onBackClick: () -> Unit,
    onObjectClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onConfigureFieldsClick: () -> Unit,
    onActionDone: (ObjectActionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

    SegmentedListScreen(
        title = state?.userentity?.caption.orEmpty(),
        subtitle = state?.userentity?.description?.ifBlank { null },
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        pane = ScreenPane.List.takeIf { isBesideDetail },
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && state?.hasObjects == true,
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.action_add),
                onFabClick = onAddClick,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                    contentDescription = stringResource(Res.string.objects_configure_fields),
                    onClick = onConfigureFieldsClick,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
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
        val sortField = current.sort.field
        if (sortField != null) {
            edgeToEdgeItem(key = "filters") { contentPadding ->
                FilterChipRow(contentPadding = contentPadding) {
                    SortChip(
                        fields = current.fields,
                        field = sortField,
                        ascending = current.sort.ascending,
                        isDefault = sortField == current.fields.first() && current.sort.ascending,
                        fieldLabel = { it.caption },
                        onSortChange = viewModel::setSort,
                    )
                }
            }
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        if (current.rows.isEmpty()) {
            emptyListItem(
                hasData = current.hasObjects,
                refresh = refresh,
                emptyText = { stringResource(Res.string.objects_empty) },
                filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
                onRetryClick = viewModel::refresh,
            )
            return@SegmentedListScreen
        }
        insetItems(
            items = current.rows.withIndex().toList(),
            key = { it.value.id },
            contentType = { "userobject" },
        ) { (index, row) ->
            ListDetailItem(
                selected = if (isBesideDetail) row.id == selectedId else null,
                onClick = { onObjectClick(row.id) },
                index = index,
                count = current.rows.size,
                modifier = Modifier.fillMaxWidth(),
                supportingContent = row.lines.takeIf { it.isNotEmpty() }?.let { lines ->
                    { SupportingLines(lines = lines) }
                },
                trailingContent = {
                    ListItemOverflowMenu(groups = listOf(objectActionItems(objectActions, row.id)))
                },
            ) {
                // Nothing filled in yet still has to be an item that can be told apart
                Text(row.title ?: "#${row.id}")
            }
        }
    }
}
