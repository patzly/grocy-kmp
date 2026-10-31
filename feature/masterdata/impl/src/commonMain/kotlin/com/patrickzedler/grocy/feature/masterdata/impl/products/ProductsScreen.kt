@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.util.RefreshState
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
import com.patrickzedler.grocy.core.forms.DisabledAlpha
import com.patrickzedler.grocy.core.forms.ListDetailItem
import com.patrickzedler.grocy.core.forms.ObjectActionEvent
import com.patrickzedler.grocy.core.forms.ObjectActionsHost
import com.patrickzedler.grocy.core.forms.ObjectActionsViewModel
import com.patrickzedler.grocy.core.forms.ProductThumbnail
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
import com.patrickzedler.grocy.core.resources.ic_rounded_settings_suggest
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.masterdata_products
import com.patrickzedler.grocy.core.resources.product_overview
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.resources.products_disabled
import com.patrickzedler.grocy.core.resources.products_empty
import com.patrickzedler.grocy.core.resources.products_empty_filtered
import com.patrickzedler.grocy.core.resources.products_has_picture
import com.patrickzedler.grocy.core.resources.products_min_stock_amount
import com.patrickzedler.grocy.core.resources.products_presets
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductLinks
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewHost
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.related.LabelPrintEffect
import com.patrickzedler.grocy.feature.masterdata.impl.related.LabelPrintViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist.AddToShoppingListViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProductsScreen(
    menuItems: List<AppBarMenuItem>,
    viewModel: ProductsViewModel,
    overviewViewModel: ProductOverviewViewModel,
    shoppingList: AddToShoppingListViewModel,
    labels: LabelPrintViewModel,
    objectActions: ObjectActionsViewModel,
    /** Where the product overview leads, its form is [onProductClick]. */
    links: ProductLinks,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    onConfigureUserfieldsClick: () -> Unit,
    onPresetsClick: () -> Unit,
    /** Gets the actions of the items' overflow menus that succeeded. */
    onActionDone: (ObjectActionEvent) -> Unit,
    modifier: Modifier = Modifier,
    /** The product shown beside the list, marked in it while [isBesideDetail]. */
    selectedId: Int? = null,
    isBesideDetail: Boolean = false,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LabelPrintEffect(labels, snackbarHostState)

    // With saved data a failed download is only a note, without it the list explains itself
    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasProducts == true,
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

    ProductsContent(
        state = state,
        refresh = refresh,
        searchState = viewModel.searchState,
        isSearchVisible = viewModel.isSearchVisible,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onOverviewClick = overviewViewModel::show,
        objectActions = objectActions,
        onAddClick = onAddClick,
        onConfigureUserfieldsClick = onConfigureUserfieldsClick,
        onPresetsClick = onPresetsClick,
        selectedId = selectedId,
        isBesideDetail = isBesideDetail,
        onSearchClick = viewModel::showSearch,
        onCloseSearchClick = viewModel::hideSearch,
        onFilterChange = viewModel::setFilter,
        onSortChange = viewModel::setSort,
        onClearFilterClick = viewModel::clearFilter,
        onRetryClick = viewModel::refresh,
        loadThumbnail = viewModel::thumbnail,
        menuItems = menuItems,
        modifier = modifier,
    )

    ProductOverviewHost(
        overview = overviewViewModel,
        shoppingList = shoppingList,
        labels = labels,
        snackbarHostState = snackbarHostState,
        links = links,
        nameOf = { id -> state?.items?.find { it.id == id }?.name.orEmpty() },
    )
}

@Composable
private fun ProductsContent(
    state: ProductsUiState?,
    refresh: RefreshState,
    searchState: TextFieldState,
    isSearchVisible: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onOverviewClick: (Int) -> Unit,
    objectActions: ObjectActionsViewModel,
    onAddClick: () -> Unit,
    onConfigureUserfieldsClick: () -> Unit,
    onPresetsClick: () -> Unit,
    selectedId: Int?,
    isBesideDetail: Boolean,
    onSearchClick: () -> Unit,
    onCloseSearchClick: () -> Unit,
    onFilterChange: ((ProductFilter) -> ProductFilter) -> Unit,
    onSortChange: (ProductSort) -> Unit,
    onClearFilterClick: () -> Unit,
    onRetryClick: () -> Unit,
    loadThumbnail: suspend (fileName: String) -> ByteArray?,
    menuItems: List<AppBarMenuItem>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    SegmentedListScreen(
        title = stringResource(Res.string.masterdata_products),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        pane = ScreenPane.List.takeIf { isBesideDetail },
        menuItems = menuItems,
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && state?.hasProducts == true,
        onRefresh = onRetryClick,
        // Every product is an item of one large group, the group spacing follows the filters
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
                    contentDescription = stringResource(Res.string.products_configure_userfields),
                    onClick = onConfigureUserfieldsClick,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_settings_suggest),
                    contentDescription = stringResource(Res.string.products_presets),
                    onClick = onPresetsClick,
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
                    onClick = {
                        scope.showSearchAtTop(listState, onSearchClick)
                    },
                )
            }
        },
    ) {
        if (state == null) return@SegmentedListScreen
        // The chips lie on the background and scroll sideways up to the screen's edges
        edgeToEdgeItem(key = "filters") { contentPadding ->
            ProductFilterBar(
                filter = state.filter,
                sort = state.sort,
                productGroups = state.productGroups,
                showClear = state.filter.isActive,
                showLocationSort = state.showLocation,
                showStoreSort = state.showStore,
                onFilterChange = onFilterChange,
                onSortChange = onSortChange,
                onClearClick = onClearFilterClick,
                contentPadding = contentPadding,
            )
        }
        searchItem(
            isVisible = isSearchVisible,
            state = searchState,
            onCloseClick = onCloseSearchClick,
        )
        productItems(
            state = state,
            refresh = refresh,
            selectedId = selectedId,
            isBesideDetail = isBesideDetail,
            onProductClick = onProductClick,
            onOverviewClick = onOverviewClick,
            objectActions = objectActions,
            onRetryClick = onRetryClick,
            loadThumbnail = loadThumbnail,
        )
    }
}

private fun LazyListScope.productItems(
    state: ProductsUiState,
    refresh: RefreshState,
    selectedId: Int?,
    isBesideDetail: Boolean,
    onProductClick: (Int) -> Unit,
    onOverviewClick: (Int) -> Unit,
    objectActions: ObjectActionsViewModel,
    onRetryClick: () -> Unit,
    loadThumbnail: suspend (fileName: String) -> ByteArray?,
) {
    when {
        state.items.isNotEmpty() -> insetItems(
            items = state.items.withIndex().toList(),
            key = { it.value.id },
            contentType = { "product" },
        ) { (index, item) ->
            ProductItem(
                item = item,
                selected = if (isBesideDetail) item.id == selectedId else null,
                index = index,
                count = state.items.size,
                onClick = { onProductClick(item.id) },
                menuGroups = listOf(
                    // Grocy computes no overview for disabled products
                    if (item.active) {
                        listOf(
                            AppBarMenuItem(stringResource(Res.string.product_overview)) {
                                onOverviewClick(item.id)
                            },
                        )
                    } else {
                        emptyList()
                    },
                    objectActionItems(objectActions, item.id),
                ),
                loadThumbnail = loadThumbnail,
            )
        }

        else -> emptyListItem(
            hasData = state.hasProducts,
            refresh = refresh,
            emptyText = { stringResource(Res.string.products_empty) },
            filteredEmptyText = { stringResource(Res.string.products_empty_filtered) },
            onRetryClick = onRetryClick,
        )
    }
}

@Composable
private fun ProductItem(
    item: ProductListItem,
    selected: Boolean?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    menuGroups: List<List<AppBarMenuItem>>,
    loadThumbnail: suspend (fileName: String) -> ByteArray?,
) {
    // Disabled products are greyed out like Grocy's muted rows
    val contentAlpha = if (item.active) 1f else DisabledAlpha
    val details = listOfNotNull(item.locationName, item.productGroupName).joinToString(" · ")
    val minStock = item.minStockAmount?.let { amount ->
        stringResource(Res.string.products_min_stock_amount, amount, item.minStockUnit.orEmpty())
            .trim()
    }
    val supporting = listOfNotNull(details.ifEmpty { null }, minStock)
    val pictureDescription = stringResource(Res.string.products_has_picture)

    ListDetailItem(
        selected = selected,
        onClick = onClick,
        index = index,
        count = count,
        // The picture hint belongs to the item, not to a node of its own
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (item.pictureFileName != null) stateDescription = pictureDescription },
        overlineContent = if (!item.active) {
            { Text(stringResource(Res.string.products_disabled)) }
        } else {
            null
        },
        supportingContent = if (supporting.isNotEmpty()) {
            { SupportingLines(lines = supporting, modifier = Modifier.alpha(contentAlpha)) }
        } else {
            null
        },
        // Button and picture bring enough space of their own, unlike icons and switches
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.pictureFileName?.let { fileName ->
                    ProductThumbnail(fileName = fileName, load = loadThumbnail)
                }
                ListItemOverflowMenu(groups = menuGroups)
            }
        },
    ) {
        Text(item.name, modifier = Modifier.alpha(contentAlpha))
    }
}

