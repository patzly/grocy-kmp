@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.stockoverview

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_never_overdue
import com.patrickzedler.grocy.core.resources.ic_rounded_analytics
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode_scanner
import com.patrickzedler.grocy.core.resources.ic_rounded_inventory_2
import com.patrickzedler.grocy.core.resources.ic_rounded_receipt_long
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.product_stock_entries
import com.patrickzedler.grocy.core.resources.products_empty_filtered
import com.patrickzedler.grocy.core.resources.stock_overview_empty
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_journal
import com.patrickzedler.grocy.core.resources.stock_overview_products
import com.patrickzedler.grocy.core.resources.stock_overview_reports
import com.patrickzedler.grocy.core.resources.stock_overview_scan
import com.patrickzedler.grocy.core.resources.stock_overview_title
import com.patrickzedler.grocy.core.resources.stock_overview_total_value
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.feature.masterdata.impl.objects.emptyListItem
import com.patrickzedler.grocy.feature.masterdata.impl.objects.searchItem
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductLinks
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewHost
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist.AddToShoppingListViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where the stock overview's own buttons lead, Grocy's links beside its title. */
internal class StockOverviewLinks(
    val onJournalClick: () -> Unit,
    val onStockEntriesClick: () -> Unit,
    val onReportsClick: () -> Unit,
    val onScanClick: () -> Unit,
)

/**
 * Grocy's "Stock overview": the products in stock or below their min. stock amount as items
 * with their fields as chips, how many and what they are worth below the title, the filters
 * and table options as chips, the search, and Grocy's links in the floating toolbar with
 * scanning as its main action. A tap opens the product overview.
 *
 * [pane]: beside the start screen, whose bar has back and the app menu.
 */
@Composable
internal fun StockOverviewScreen(
    viewModel: StockOverviewViewModel,
    overviewViewModel: ProductOverviewViewModel,
    shoppingList: AddToShoppingListViewModel,
    productLinks: ProductLinks,
    links: StockOverviewLinks,
    itemActions: StockOverviewItemActions,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

    SegmentedListScreen(
        title = stringResource(Res.string.stock_overview_title),
        subtitle = state?.takeIf { it.hasProducts }?.let { summary(it) },
        modifier = modifier,
        navigationIcon = { if (pane == null) NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        pane = pane,
        listState = listState,
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && state?.hasProducts == true,
        onRefresh = viewModel::refresh,
        // Every product is an item of its group, the group spacing follows the filters
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior, toolbarModifier ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                modifier = toolbarModifier,
                fabPainter = painterResource(Res.drawable.ic_rounded_barcode_scanner),
                fabDescription = stringResource(Res.string.stock_overview_scan),
                onFabClick = links.onScanClick,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_receipt_long),
                    contentDescription = stringResource(Res.string.stock_overview_journal),
                    onClick = links.onJournalClick,
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_inventory_2),
                    contentDescription = stringResource(Res.string.product_stock_entries),
                    onClick = links.onStockEntriesClick,
                )
                // Grocy's reports need location or price tracking
                val config = state?.config
                if (config != null &&
                    (config.stockLocationTracking || config.stockPriceTracking)
                ) {
                    FloatingToolbarIconButton(
                        painter = painterResource(Res.drawable.ic_rounded_analytics),
                        contentDescription = stringResource(Res.string.stock_overview_reports),
                        onClick = links.onReportsClick,
                    )
                }
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = {
                        viewModel.showSearch()
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
            }
        },
    ) {
        val current = state ?: return@SegmentedListScreen
        // The chips lie on the background and scroll sideways up to the screen's edges
        edgeToEdgeItem(key = "filters") { contentPadding ->
            StockOverviewFilterBar(
                state = current,
                energyUnit = current.config.energyUnit,
                onFilterChange = viewModel::setFilter,
                onSortChange = viewModel::setSort,
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
        stockItems(
            state = current,
            refresh = refresh,
            isSearching = viewModel.searchState.text.isNotBlank(),
            actions = itemActions,
            onRetryClick = viewModel::refresh,
            loadThumbnail = viewModel::thumbnail,
        )
    }

    ProductOverviewHost(
        overview = overviewViewModel,
        shoppingList = shoppingList,
        snackbarHostState = snackbarHostState,
        links = productLinks,
        nameOf = { id ->
            state?.groups?.firstNotNullOfOrNull { group ->
                group.items.firstOrNull { it.productId == id }
            }?.name.orEmpty()
        },
    )
}

/** Grocy's text beside its title: the products in stock and, with prices, what they cost. */
@Composable
private fun summary(state: StockOverviewUiState): String {
    val products = pluralStringResource(
        Res.plurals.stock_overview_products,
        state.productCount,
        state.productCount,
    )
    val value = state.totalValue ?: return products
    val total = formatCurrency(value, state.currency, state.priceDecimals)
    return "$products, ${stringResource(Res.string.stock_overview_total_value, total)}"
}

private fun LazyListScope.stockItems(
    state: StockOverviewUiState,
    refresh: RefreshState,
    isSearching: Boolean,
    actions: StockOverviewItemActions,
    onRetryClick: () -> Unit,
    loadThumbnail: suspend (fileName: String) -> ByteArray?,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means nothing is in stock
            hasData = state.hasProducts && (state.filter.isActive || isSearching),
            refresh = refresh,
            emptyText = { stringResource(Res.string.stock_overview_empty) },
            filteredEmptyText = { stringResource(Res.string.products_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    val style = StockOverviewItemStyle(
        fields = state.fields,
        currency = state.currency,
        priceDecimals = state.priceDecimals,
        canOpen = state.config.stockProductOpenedTracking,
        hasShoppingList = state.config.shoppingList,
        hasRecipes = state.config.recipes,
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
            items = group.items.withIndex().toList(),
            key = { it.value.productId },
            contentType = { "product" },
        ) { (index, item) ->
            StockOverviewListItem(
                item = item,
                style = style,
                index = index,
                count = group.items.size,
                actions = actions,
                loadThumbnail = loadThumbnail,
            )
        }
    }
}

@Composable
private fun groupTitleText(title: StockOverviewGroupTitle): String = when (title) {
    is StockOverviewGroupTitle.Text -> title.text
    is StockOverviewGroupTitle.Day -> formatDate(title.epochDay)
    StockOverviewGroupTitle.NeverOverdue -> stringResource(Res.string.data_never_overdue)
    StockOverviewGroupTitle.None -> stringResource(Res.string.stock_overview_group_not_set)
}

// The list puts the segmented gap between its items, the title adds the rest of a group gap
private val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap
