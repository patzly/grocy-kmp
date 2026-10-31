@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.GroupTitle
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_note
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.objects_delete_failed
import com.patrickzedler.grocy.core.resources.shopping_list_add_all_to_stock
import com.patrickzedler.grocy.core.resources.shopping_list_add_item
import com.patrickzedler.grocy.core.resources.shopping_list_add_missing
import com.patrickzedler.grocy.core.resources.shopping_list_add_overdue_expired
import com.patrickzedler.grocy.core.resources.shopping_list_choose
import com.patrickzedler.grocy.core.resources.shopping_list_clear
import com.patrickzedler.grocy.core.resources.shopping_list_clear_done
import com.patrickzedler.grocy.core.resources.shopping_list_delete
import com.patrickzedler.grocy.core.resources.shopping_list_edit
import com.patrickzedler.grocy.core.resources.shopping_list_empty
import com.patrickzedler.grocy.core.resources.shopping_list_empty_filtered
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_new
import com.patrickzedler.grocy.core.resources.shopping_list_notes
import com.patrickzedler.grocy.core.resources.shopping_list_notes_empty
import com.patrickzedler.grocy.core.resources.shopping_list_print
import com.patrickzedler.grocy.core.resources.shopping_list_ungrouped
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_total_value
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where the shopping list leads: its forms and Grocy's purchase. */
internal class ShoppingListLinks(
    /** Grocy's "Add item" on the shown list. */
    val onAddItemClick: (listId: Int) -> Unit,
    val onItemClick: (itemId: Int) -> Unit,
    val onNewListClick: () -> Unit,
    val onEditListClick: (listId: Int) -> Unit,
    /**
     * Grocy's purchase of an item, which removes the item once booked; with its progress while
     * "Add all list items to stock" books one after the other.
     */
    val onPurchase: (ShoppingListPurchase, PurchaseProgress?) -> Unit,
)

/**
 * Grocy's "Shopping list": the items of the chosen list grouped by product group, checked off
 * with their checkbox (offline too), the status filter and table options as chips, the search,
 * the list's notes at the end (offline too). The title names the shown list, with several lists
 * it opens Grocy's list selector. The overflow menu holds Grocy's list and stock actions, the
 * floating toolbar "Add item" as its main action.
 *
 * @param selectedItemId the item shown beside the list, marked in it while [isBesideDetail].
 * @param isShown whether the list is the screen the user sees, not one that only shows
 *   during a back gesture.
 */
@Composable
internal fun ShoppingListScreen(
    viewModel: ShoppingListViewModel,
    actions: StockPageActions,
    links: ShoppingListLinks,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedItemId: Int? = null,
    isBesideDetail: Boolean = false,
    isShown: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LabelPrintEffect(actions.labels, snackbarHostState)

    // With saved data a failed download is only a note, without it the list explains itself
    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasData == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    ShoppingListEvents(viewModel, snackbarHostState, links.onPurchase)
    // Back from a purchase of "Add all list items to stock" goes on with the next item, only
    // once back is done: a back gesture shows the list before it may still be cancelled
    LaunchedEffect(viewModel, isShown) { if (isShown) viewModel.onShown() }
    // Back closes the search first and stays on the list
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.isSearchVisible,
        onBackCompleted = viewModel::hideSearch,
    )
    val current = state
    ShoppingListDialogs(viewModel, current)

    val chooseLabel = stringResource(Res.string.shopping_list_choose)
    SegmentedListScreen(
        title = current?.list?.name ?: stringResource(Res.string.shopping_list_list),
        subtitle = current?.let { subtitle(it) },
        onTitleClick = viewModel::chooseList.takeIf { current?.canChooseList == true },
        titleClickLabel = chooseLabel,
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        actionMenuGroups = current?.let { actionMenuGroups(it, viewModel, links) }.orEmpty(),
        pane = ScreenPane.List.takeIf { isBesideDetail },
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && current?.hasData == true,
        onRefresh = viewModel::refresh,
        // Every item is an item of its group, the group spacing follows the filters
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.shopping_list_add_item),
                onFabClick = {
                    links.onAddItemClick(current?.list?.id ?: ShoppingList.DEFAULT_ID)
                },
            ) {
                if (current?.config?.stock == true) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_inventory_2),
                        contentDescription =
                            stringResource(Res.string.shopping_list_add_all_to_stock),
                        onClick = viewModel::addAllToStock,
                        enabled = current.purchases.isNotEmpty(),
                    )
                }
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_edit_note),
                    contentDescription = stringResource(Res.string.shopping_list_notes),
                    onClick = viewModel::editNotes,
                    enabled = current?.list != null,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = { scope.showSearchAtTop(listState, viewModel::showSearch) },
                )
            }
        },
    ) {
        if (current == null) return@SegmentedListScreen
        // The chips lie on the background and scroll sideways up to the screen's edges
        edgeToEdgeItem(key = "filters") { contentPadding ->
            ShoppingListFilterBar(
                state = current,
                onStatusChange = viewModel::setStatus,
                onGroupingChange = viewModel::setGrouping,
                onFieldShownChange = viewModel::setFieldShown,
                onClearClick = viewModel::clearFilter,
                contentPadding = contentPadding,
            )
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        rows(
            state = current,
            refresh = refresh,
            isSearching = viewModel.searchState.text.isNotBlank(),
            selectedItemId = selectedItemId.takeIf { isBesideDetail },
            isBesideDetail = isBesideDetail,
            actions = ShoppingListRowActions(
                onClick = { links.onItemClick(it.id) },
                onToggleDone = viewModel::toggleDone,
                onDelete = viewModel::delete,
                onAddToStock = viewModel::addToStock,
                onProductOverview = actions.overview::show,
            ),
            onRetryClick = viewModel::refresh,
        )
        notes(current, onClick = viewModel::editNotes)
    }

    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        labels = actions.labels,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id ->
            state?.groups?.firstNotNullOfOrNull { group ->
                group.rows.firstOrNull { it.productId == id }
            }?.title.orEmpty()
        },
    )
}

/** Opens purchases and shows what failed. */
@Composable
private fun ShoppingListEvents(
    viewModel: ShoppingListViewModel,
    snackbarHostState: SnackbarHostState,
    onPurchase: (ShoppingListPurchase, PurchaseProgress?) -> Unit,
) {
    val currentOnPurchase by rememberUpdatedState(onPurchase)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ShoppingListEvent.OpenPurchase ->
                    currentOnPurchase(event.purchase, event.progress)
                is ShoppingListEvent.Failed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(
                        when (event.action) {
                            ShoppingListAction.DELETE_ITEM,
                            ShoppingListAction.DELETE_LIST,
                            -> Res.string.objects_delete_failed
                            ShoppingListAction.CLEAR,
                            ShoppingListAction.ADD_PRODUCTS,
                            -> Res.string.data_save_failed
                        },
                    ),
                )
            }
        }
    }
}

/** Grocy's text beside its title, the list's total value with prices. */
@Composable
private fun subtitle(state: ShoppingListUiState): String? =
    state.totalValue?.let { value ->
        stringResource(
            Res.string.stock_overview_total_value,
            formatCurrency(value, state.currency, state.priceDecimals),
        )
    }

/** Grocy's "List actions", the list's clearing, its "Stock actions" and printing. */
@Composable
private fun actionMenuGroups(
    state: ShoppingListUiState,
    viewModel: ShoppingListViewModel,
    links: ShoppingListLinks,
): List<List<AppBarMenuItem>> {
    val list = state.list
    val config = state.config
    val hasItems = state.itemCount > 0 && !viewModel.isChanging
    return listOf(
        if (config.shoppingListMultipleLists) {
            listOfNotNull(
                AppBarMenuItem(stringResource(Res.string.shopping_list_new)) {
                    links.onNewListClick()
                },
                list?.let {
                    AppBarMenuItem(stringResource(Res.string.shopping_list_edit)) {
                        links.onEditListClick(it.id)
                    }
                },
                // Grocy's first list cannot be deleted
                AppBarMenuItem(
                    stringResource(Res.string.shopping_list_delete),
                    enabled = list != null && list.id != ShoppingList.DEFAULT_ID,
                    onClick = viewModel::requestDeleteList,
                ),
            )
        } else {
            emptyList()
        },
        listOf(
            AppBarMenuItem(
                stringResource(Res.string.shopping_list_clear),
                enabled = hasItems,
                onClick = viewModel::requestClear,
            ),
            AppBarMenuItem(
                stringResource(Res.string.shopping_list_clear_done),
                enabled = hasItems,
                onClick = viewModel::clearDone,
            ),
        ),
        if (config.stock) {
            listOfNotNull(
                AppBarMenuItem(
                    stringResource(Res.string.shopping_list_add_all_to_stock),
                    enabled = state.purchases.isNotEmpty(),
                    onClick = viewModel::addAllToStock,
                ),
                // Grocy adds them by itself then
                AppBarMenuItem(
                    stringResource(Res.string.shopping_list_add_missing),
                    enabled = !viewModel.isChanging,
                    onClick = viewModel::addMissingProducts,
                ).takeIf { !state.settings.shoppingListAutoAddBelowMinStock },
                AppBarMenuItem(
                    stringResource(Res.string.shopping_list_add_overdue_expired),
                    enabled = !viewModel.isChanging,
                    onClick = viewModel::addOverdueAndExpiredProducts,
                ),
            )
        } else {
            emptyList()
        },
        listOfNotNull(
            AppBarMenuItem(
                stringResource(Res.string.shopping_list_print),
                onClick = viewModel::openPrint,
            ).takeIf { config.thermalPrinter && list != null },
        ),
    )
}

private fun LazyListScope.rows(
    state: ShoppingListUiState,
    refresh: RefreshState,
    isSearching: Boolean,
    selectedItemId: Int?,
    isBesideDetail: Boolean,
    actions: ShoppingListRowActions,
    onRetryClick: () -> Unit,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means nothing is on it
            hasData = state.hasData && (state.status != null || isSearching),
            refresh = refresh,
            emptyText = { stringResource(Res.string.shopping_list_empty) },
            filteredEmptyText = { stringResource(Res.string.shopping_list_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    val style = ShoppingListRowStyle(
        fields = state.fields,
        currency = state.currency,
        priceDecimals = state.priceDecimals,
        hasStock = state.config.stock,
    )
    state.groups.forEachIndexed { groupIndex, group ->
        group.title?.let { title ->
            insetItem(key = "group-$title", contentType = "groupTitle") {
                GroupTitle(
                    text = groupTitleText(title),
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
            key = { it.value.id },
            contentType = { "item" },
        ) { (index, row) ->
            ShoppingListRowItem(
                row = row,
                style = style,
                selected = if (isBesideDetail) row.id == selectedItemId else null,
                sync = state.syncStates[row.id],
                index = index,
                count = group.rows.size,
                actions = actions,
            )
        }
    }
}

/**
 * Grocy's "Notes" below its table, edited in a dialog; notes the server has not taken yet show
 * the state of sending them like a checked off item.
 */
private fun LazyListScope.notes(state: ShoppingListUiState, onClick: () -> Unit) {
    if (state.list == null) return
    insetItem(key = "notes-title") {
        GroupTitle(
            text = stringResource(Res.string.shopping_list_notes),
            modifier = Modifier.padding(top = GroupTitleTopPadding),
        )
    }
    insetItem(key = "notes") {
        val haptic = LocalHapticFeedback.current
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                onClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            colors = SegmentedListDefaults.colors(),
            modifier = Modifier.fillMaxWidth().clickCursor(),
            leadingContent = {
                LeadingTrailingContentWrapper {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_edit_note),
                        contentDescription = null,
                    )
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.notes.isBlank()) {
                    Text(
                        text = stringResource(Res.string.shopping_list_notes_empty),
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(state.notes, modifier = Modifier.weight(1f, fill = false))
                }
                SyncIndicator(state.notesSync)
            }
        }
    }
}

@Composable
private fun groupTitleText(title: ShoppingListGroupTitle): String = when (title) {
    is ShoppingListGroupTitle.Text -> title.text
    ShoppingListGroupTitle.Ungrouped -> stringResource(Res.string.shopping_list_ungrouped)
    ShoppingListGroupTitle.NotSet -> stringResource(Res.string.stock_overview_group_not_set)
}

// The list puts the segmented gap between its items, the title adds the rest of a group gap
private val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap
