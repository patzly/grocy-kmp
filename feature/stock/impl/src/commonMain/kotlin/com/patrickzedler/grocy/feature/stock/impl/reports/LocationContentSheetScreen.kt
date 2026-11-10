package com.patrickzedler.grocy.feature.stock.impl.reports

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.ToggleFilterChip
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.showsPullIndicator
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.location_sheet_only_in_stock
import com.patrickzedler.grocy.core.resources.product_overview_opened
import com.patrickzedler.grocy.core.resources.products_empty_filtered
import com.patrickzedler.grocy.core.resources.stock_overview_empty
import com.patrickzedler.grocy.core.resources.stock_report_location_content_sheet
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Location Content Sheet": a group per location with its products and their amounts,
 * the opened part below; "Show only in stock products" as chip. A tap opens the product
 * overview, like on Grocy's other stock pages.
 */
@Composable
internal fun LocationContentSheetScreen(
    viewModel: LocationContentSheetViewModel,
    actions: StockPageActions,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasStock == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )

    SegmentedListScreen(
        title = stringResource(Res.string.stock_report_location_content_sheet),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.showsPullIndicator(state?.hasStock == true),
        onRefresh = viewModel::refresh,
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            FilterChipRow(contentPadding = contentPadding) {
                ToggleFilterChip(
                    label = stringResource(Res.string.location_sheet_only_in_stock),
                    selected = current.onlyInStock,
                    onSelectedChange = viewModel::setOnlyInStock,
                )
            }
        }
        if (current.groups.isEmpty()) {
            emptyListItem(
                hasData = current.hasStock,
                refresh = refresh,
                emptyText = { stringResource(Res.string.stock_overview_empty) },
                filteredEmptyText = { stringResource(Res.string.products_empty_filtered) },
                onRetryClick = viewModel::refresh,
            )
        }
        current.groups.forEach { group ->
            insetItem(key = "location-${group.locationId}", contentType = "location") {
                SegmentedGroup(title = group.name) {
                    group.items.forEachIndexed { index, item ->
                        LocationSheetListItem(
                            item = item,
                            index = index,
                            count = group.items.size,
                            onClick = { actions.overview.show(item.productId) },
                        )
                    }
                }
            }
        }
    }

    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        labels = actions.labels,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id ->
            state?.groups?.firstNotNullOfOrNull { group ->
                group.items.firstOrNull { it.productId == id }
            }?.name.orEmpty()
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocationSheetListItem(
    item: LocationSheetItem,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = item.opened?.let { opened ->
            { Text(stringResource(Res.string.product_overview_opened, opened)) }
        },
        trailingContent = {
            Text(
                text = item.amount,
                style = MaterialTheme.typography.bodyLarge.withTabularNumbers(),
            )
        },
        content = { Text(item.name) },
    )
}
