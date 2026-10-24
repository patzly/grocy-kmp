@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.FloatingToolbarIconButton
import com.patrickzedler.grocy.core.design.component.FloatingToolbarWithFab
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
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
import com.patrickzedler.grocy.core.resources.ic_rounded_info
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
import com.patrickzedler.grocy.core.resources.product_overview_show
import com.patrickzedler.grocy.core.resources.products_presets
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewDialog
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProductsScreen(
    menuItems: List<AppBarMenuItem>,
    viewModel: ProductsViewModel,
    overviewViewModel: ProductOverviewViewModel,
    onBackClick: () -> Unit,
    onProductClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val overviewProductId by overviewViewModel.productId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    ProductsContent(
        state = state,
        refresh = refresh,
        searchState = viewModel.searchState,
        isSearchVisible = viewModel.isSearchVisible,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onOverviewClick = overviewViewModel::show,
        onAddClick = onAddClick,
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

    overviewProductId?.let { productId ->
        ProductOverviewDialog(
            productId = productId,
            name = state?.items?.find { it.id == productId }?.name.orEmpty(),
            viewModel = overviewViewModel,
            onEditClick = { id ->
                overviewViewModel.hide()
                onProductClick(id)
            },
            onDismissRequest = overviewViewModel::hide,
        )
    }
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
    onAddClick: () -> Unit,
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
                FloatingToolbarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = {
                        onSearchClick()
                        scope.launch { listState.animateScrollToItem(0) }
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
        insetItem(key = "search") {
            // Group spacing around the search, which springs open below the chips
            Column(Modifier.padding(top = SearchSpacing)) {
                val motionScheme = MaterialTheme.motionScheme
                AnimatedVisibility(
                    visible = isSearchVisible,
                    // From the top: the focused field then lies in view while it grows, so
                    // focusing it right away (keyboard included) does not scroll the list
                    enter = expandVertically(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        expandFrom = Alignment.Top,
                    ) + fadeIn(motionScheme.defaultEffectsSpec()),
                    exit = shrinkVertically(
                        animationSpec = motionScheme.defaultSpatialSpec(),
                        shrinkTowards = Alignment.Top,
                    ) + fadeOut(motionScheme.defaultEffectsSpec()),
                ) {
                    SearchField(
                        state = searchState,
                        onCloseClick = onCloseSearchClick,
                        // The keyboard closes with the start of the closing, not after it
                        isClosing = transition.targetState != EnterExitState.Visible,
                        modifier = Modifier.padding(
                            top = ListItemDefaults.SegmentedGap,
                            bottom = SearchSpacing + ListItemDefaults.SegmentedGap,
                        ),
                    )
                }
            }
        }
        productItems(
            state = state,
            refresh = refresh,
            onProductClick = onProductClick,
            onOverviewClick = onOverviewClick,
            onRetryClick = onRetryClick,
            loadThumbnail = loadThumbnail,
        )
    }
}

private fun LazyListScope.productItems(
    state: ProductsUiState,
    refresh: RefreshState,
    onProductClick: (Int) -> Unit,
    onOverviewClick: (Int) -> Unit,
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
                index = index,
                count = state.items.size,
                onClick = { onProductClick(item.id) },
                onOverviewClick = { onOverviewClick(item.id) },
                loadThumbnail = loadThumbnail,
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
    isClosing: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isClosing) {
        if (isClosing) focusManager.clearFocus() else focusRequester.requestFocus()
    }

    SegmentedContainer(index = 0, count = 1, modifier = modifier) {
        RoundedTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(Res.string.action_search)) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        haptic.confirm()
                        onCloseClick()
                    },
                    modifier = Modifier.clickCursor(),
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
    onOverviewClick: () -> Unit,
    loadThumbnail: suspend (fileName: String) -> ByteArray?,
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
            .semantics { if (item.pictureFileName != null) stateDescription = pictureDescription }
            .clickCursor(),
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
        trailingContent = if (item.active || item.pictureFileName != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Grocy computes no overview for disabled products
                    if (item.active) {
                        OverviewButton(onClick = onOverviewClick)
                    }
                    item.pictureFileName?.let { fileName ->
                        ProductThumbnail(fileName = fileName, load = loadThumbnail)
                    }
                }
            }
        } else {
            null
        },
    ) {
        Text(item.name, modifier = Modifier.alpha(contentAlpha))
    }
}

/** Opens Grocy's "Product overview", narrow so the item keeps its room for the name. */
@Composable
private fun OverviewButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val description = stringResource(Res.string.product_overview_show)

    HapticTooltipBox(text = description) {
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
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_info),
                contentDescription = description,
            )
        }
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
                    .padding(top = 16.dp)
                    .clickCursor(),
            ) {
                Text(actionText)
            }
        }
    }
}

private const val DisabledAlpha = 0.6f

// The list puts the segmented gap between items: chips, gap, this, gap, products make a group gap
private val SearchSpacing =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap * 2
