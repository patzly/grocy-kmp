@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.LeadingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_clear
import com.patrickzedler.grocy.core.resources.action_retry
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_attributes
import com.patrickzedler.grocy.core.resources.ic_rounded_image
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.ic_rounded_settings_suggest
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.masterdata_products
import com.patrickzedler.grocy.core.resources.products_configure_userfields
import com.patrickzedler.grocy.core.resources.products_disabled
import com.patrickzedler.grocy.core.resources.products_empty
import com.patrickzedler.grocy.core.resources.products_empty_filtered
import com.patrickzedler.grocy.core.resources.products_has_picture
import com.patrickzedler.grocy.core.resources.products_min_stock_amount
import com.patrickzedler.grocy.core.resources.products_presets
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProductsScreen(
    viewModel: ProductsViewModel,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // With saved data a failed download is only a note, without it the list explains itself
    LaunchedEffect(refresh.error, state?.hasProducts) {
        val error = refresh.error ?: return@LaunchedEffect
        if (state?.hasProducts == true) {
            snackbarHostState.showSnackbar(
                getString(
                    if (error == RefreshError.OFFLINE) {
                        Res.string.data_offline
                    } else {
                        Res.string.data_load_failed
                    },
                ),
            )
            viewModel.onErrorShown()
        }
    }

    ProductsContent(
        state = state,
        refresh = refresh,
        searchState = viewModel.searchState,
        isSearchVisible = viewModel.isSearchVisible,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onAddClick = onAddClick,
        onSearchClick = viewModel::showSearch,
        onCloseSearchClick = viewModel::hideSearch,
        onFilterChange = viewModel::setFilter,
        onSortChange = viewModel::setSort,
        onClearFilterClick = viewModel::clearFilter,
        onRetryClick = viewModel::refresh,
        modifier = modifier,
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
    onAddClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCloseSearchClick: () -> Unit,
    onFilterChange: ((ProductFilter) -> ProductFilter) -> Unit,
    onSortChange: (ProductSort) -> Unit,
    onClearFilterClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    SegmentedListScreen(
        title = stringResource(Res.string.masterdata_products),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        listState = listState,
        snackbarHostState = snackbarHostState,
        // Every product is an item of one large group, the group spacing follows the filters
        itemSpacing = ListItemDefaults.SegmentedGap,
        floatingToolbar = { scrollBehavior ->
            FloatingToolbarWithFab(
                scrollBehavior = scrollBehavior,
                fabPainter = painterResource(Res.drawable.ic_rounded_add),
                fabDescription = stringResource(Res.string.action_add),
                onFabClick = onAddClick,
            ) {
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = {
                        onSearchClick()
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
                // Not available in the app yet, like the web frontend they lead elsewhere
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_edit_attributes),
                    contentDescription = stringResource(Res.string.products_configure_userfields),
                    onClick = {},
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_settings_suggest),
                    contentDescription = stringResource(Res.string.products_presets),
                    onClick = {},
                )
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_vertical_align_top),
                    contentDescription = stringResource(Res.string.action_scroll_to_top),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    enabled = listState.canScrollBackward,
                )
            }
        },
    ) {
        if (state == null) return@SegmentedListScreen
        insetItem(key = "filters") {
            ProductFilterBar(
                modifier = Modifier.padding(bottom = GroupSpacingAfterGap),
                filter = state.filter,
                sort = state.sort,
                productGroups = state.productGroups,
                showClear = state.filter.isActive || isSearchVisible,
                showLocationSort = state.showLocation,
                showStoreSort = state.showStore,
                onFilterChange = onFilterChange,
                onSortChange = onSortChange,
                onClearClick = onClearFilterClick,
            )
        }
        if (isSearchVisible) {
            insetItem(key = "search") {
                SearchField(
                    state = searchState,
                    onCloseClick = onCloseSearchClick,
                    modifier = Modifier.padding(bottom = GroupSpacingAfterGap),
                )
            }
        }
        productItems(
            state = state,
            refresh = refresh,
            onProductClick = onProductClick,
            onRetryClick = onRetryClick,
        )
    }
}

private fun LazyListScope.productItems(
    state: ProductsUiState,
    refresh: RefreshState,
    onProductClick: (Int) -> Unit,
    onRetryClick: () -> Unit,
) {
    when {
        state.items.isNotEmpty() -> insetItems(
            items = state.items.withIndex().toList(),
            key = { it.value.id },
            contentType = { "product" },
        ) { (index, item) ->
            ProductItem(
                item = item,
                index = index,
                count = state.items.size,
                onClick = { onProductClick(item.id) },
            )
        }

        !state.hasProducts && refresh.isRefreshing -> insetItem(key = "loading") {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }

        !state.hasProducts && refresh.error != null -> insetItem(key = "error") {
            MessageItem(
                text = stringResource(
                    if (refresh.error == RefreshError.OFFLINE) {
                        Res.string.data_offline
                    } else {
                        Res.string.data_load_failed
                    },
                ),
                actionText = stringResource(Res.string.action_retry),
                onActionClick = onRetryClick,
            )
        }

        else -> insetItem(key = "empty") {
            MessageItem(
                text = stringResource(
                    if (state.hasProducts) {
                        Res.string.products_empty_filtered
                    } else {
                        Res.string.products_empty
                    },
                ),
            )
        }
    }
}

@Composable
private fun SearchField(
    state: TextFieldState,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

    SegmentedContainer(index = 0, count = 1, modifier = modifier) {
        RoundedTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(Res.string.action_search)) },
            leadingIcon = {
                Icon(painterResource(Res.drawable.ic_rounded_search), contentDescription = null)
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        haptic.confirm()
                        onCloseClick()
                    },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_close),
                        contentDescription = stringResource(Res.string.action_clear),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
    }
}

@Composable
private fun ProductItem(
    item: ProductListItem,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // Disabled products are greyed out like Grocy's muted rows
    val contentAlpha = if (item.active) 1f else DisabledAlpha
    val details = listOfNotNull(item.locationName, item.productGroupName).joinToString(" · ")
    val minStock = item.minStockAmount?.let { amount ->
        stringResource(Res.string.products_min_stock_amount, amount, item.minStockUnit.orEmpty())
            .trim()
    }
    val supporting = listOfNotNull(details.ifEmpty { null }, minStock)
    val pictureDescription = stringResource(Res.string.products_has_picture)

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        // The picture hint belongs to the item, not to a node of its own
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (item.hasPicture) stateDescription = pictureDescription },
        overlineContent = if (!item.active) {
            { Text(stringResource(Res.string.products_disabled)) }
        } else {
            null
        },
        supportingContent = if (supporting.isNotEmpty()) {
            {
                Column(
                    modifier = Modifier.alpha(contentAlpha),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    supporting.forEach { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        } else {
            null
        },
        trailingContent = if (item.hasPicture) {
            {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_image),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
    ) {
        Text(item.name, modifier = Modifier.alpha(contentAlpha))
    }
}

@Composable
private fun MessageItem(
    text: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    SegmentedContainer(index = 0, count = 1) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (actionText != null) {
            Button(
                onClick = {
                    haptic.confirm()
                    onActionClick()
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
            ) {
                Text(actionText)
            }
        }
    }
}

private const val DisabledAlpha = 0.6f

// The list puts the segmented gap between items, this adds up to the gap between groups
private val GroupSpacingAfterGap = SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap
