@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.model.recipes.RecipesView
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_add
import com.patrickzedler.grocy.core.resources.action_scroll_to_top
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_grid_view
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.ic_rounded_vertical_align_top
import com.patrickzedler.grocy.core.resources.ic_rounded_view_agenda
import com.patrickzedler.grocy.core.resources.recipes_due_score_value
import com.patrickzedler.grocy.core.resources.recipes_empty
import com.patrickzedler.grocy.core.resources.recipes_empty_filtered
import com.patrickzedler.grocy.core.resources.recipes_show_gallery
import com.patrickzedler.grocy.core.resources.recipes_show_list
import com.patrickzedler.grocy.core.resources.recipes_title
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionEvent
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsHost
import com.patrickzedler.grocy.feature.recipes.impl.actions.RecipeActionsViewModel
import com.patrickzedler.grocy.feature.recipes.impl.actions.recipeMenuGroups
import com.patrickzedler.grocy.feature.recipes.impl.actions.rememberRecipeGrocycodeDownload
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Where the recipes page leads. */
internal class RecipesLinks(
    /** Shows the recipe, beside the list where the window is wide enough. */
    val onRecipeClick: (id: Int) -> Unit,
    /** Opens the recipe's form, beside the list where the window is wide enough. */
    val onEditClick: (id: Int) -> Unit,
    val onAddClick: () -> Unit,
    /** Gets the copies and deletions, e.g. to open the copy or close a deleted recipe. */
    val onActionDone: (RecipeActionEvent) -> Unit,
)

/**
 * Grocy's "Recipes": the recipes as list or gallery (its tabs, switched in the floating
 * toolbar), its status filter and table options as chips, the search, and "Add" as the
 * toolbar's FAB. A tap shows the recipe, each item's menu has Grocy's actions of a recipe.
 * Beside the recipe or its form ([isBesideDetail]) the items mark the [selectedId].
 */
@Composable
internal fun RecipesScreen(
    viewModel: RecipesViewModel,
    actions: RecipeActionsViewModel,
    links: RecipesLinks,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    selectedId: Int?,
    isBesideDetail: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val current = state

    // With saved data a failed download is only a note, without it the list explains itself
    RefreshErrorEffect(
        error = refresh.error,
        hasData = current?.hasRecipes == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    // Back closes the search first and stays on the list
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.isSearchVisible,
        onBackCompleted = viewModel::hideSearch,
    )
    RecipeActionsHost(actions, snackbarHostState, links.onActionDone)
    val downloadGrocycode = rememberRecipeGrocycodeDownload(snackbarHostState) {
        current?.config?.grocycode2D ?: true
    }

    // The gallery fits as many cards in a row as the pane's width allows
    BoxWithConstraints(modifier) {
        val columns = galleryColumns(maxWidth)
        SegmentedListScreen(
            title = stringResource(Res.string.recipes_title),
            navigationIcon = { NavigateBackButton(onClick = onBackClick) },
            menuItems = menuItems,
            pane = ScreenPane.List.takeIf { isBesideDetail },
            listState = listState,
            snackbarHostState = snackbarHostState,
            // The first download shows its own indicator in the empty list
            isRefreshing = refresh.isRefreshing && current?.hasRecipes == true,
            onRefresh = viewModel::refresh,
            // Every recipe is an item of its group, the group spacing follows the filters
            itemSpacing = ListItemDefaults.SegmentedGap,
            floatingToolbar = { scrollBehavior, toolbarModifier ->
                FloatingToolbarWithFab(
                    scrollBehavior = scrollBehavior,
                    modifier = toolbarModifier,
                    fabPainter = painterResource(Res.drawable.ic_rounded_add),
                    fabDescription = stringResource(Res.string.action_add),
                    onFabClick = links.onAddClick,
                ) {
                    // Grocy's tabs: the other view than the shown one
                    val isGallery = current?.view == RecipesView.GALLERY
                    FloatingToolbarIconButton(
                        painter = painterResource(
                            if (isGallery) {
                                Res.drawable.ic_rounded_view_agenda
                            } else {
                                Res.drawable.ic_rounded_grid_view
                            },
                        ),
                        contentDescription = stringResource(
                            if (isGallery) {
                                Res.string.recipes_show_list
                            } else {
                                Res.string.recipes_show_gallery
                            },
                        ),
                        onClick = {
                            viewModel.setView(
                                if (isGallery) RecipesView.LIST else RecipesView.GALLERY,
                            )
                        },
                        enabled = current != null,
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
            if (current == null) return@SegmentedListScreen
            // The chips lie on the background and scroll sideways up to the screen's edges
            edgeToEdgeItem(key = "filters") { contentPadding ->
                RecipesFilterBar(
                    state = current,
                    onStatusChange = viewModel::setStatus,
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
            recipeItems(
                state = current,
                refresh = refresh,
                isFiltered = current.filter.isActive || viewModel.searchState.text.isNotBlank(),
                columns = columns,
                selectedId = selectedId.takeIf { isBesideDetail },
                isBesideDetail = isBesideDetail,
                onRecipeClick = links.onRecipeClick,
                onRetryClick = viewModel::refresh,
                loadPicture = viewModel::picture,
                menuGroups = { id ->
                    recipeMenuGroups(
                        recipeId = id,
                        config = current.config,
                        actions = actions,
                        onEditClick = links.onEditClick,
                        onGrocycodeDownload = downloadGrocycode,
                    )
                },
            )
        }
    }
}

private fun LazyListScope.recipeItems(
    state: RecipesUiState,
    refresh: RefreshState,
    isFiltered: Boolean,
    columns: Int,
    selectedId: Int?,
    isBesideDetail: Boolean,
    onRecipeClick: (Int) -> Unit,
    onRetryClick: () -> Unit,
    loadPicture: suspend (fileName: String) -> ByteArray?,
    menuGroups: @Composable (id: Int) -> List<List<AppBarMenuItem>>,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means there are no recipes
            hasData = state.hasRecipes && isFiltered,
            refresh = refresh,
            emptyText = { stringResource(Res.string.recipes_empty) },
            filteredEmptyText = { stringResource(Res.string.recipes_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    val selected = { id: Int -> if (isBesideDetail) id == selectedId else null }
    state.groups.forEachIndexed { groupIndex, group ->
        group.title?.let { title ->
            insetItem(key = "group-$title", contentType = "groupTitle") {
                GroupTitle(
                    text = groupTitleText(title, state.amountDecimals),
                    // The first group follows the filters' group gap, the others the items
                    modifier = if (groupIndex > 0) {
                        Modifier.padding(top = GroupTitleTopPadding)
                    } else {
                        Modifier
                    },
                )
            }
        }
        when (state.view) {
            RecipesView.LIST -> insetItems(
                items = group.items.withIndex().toList(),
                key = { "${group.title}-${it.value.id}" },
                contentType = { "recipe" },
            ) { (index, item) ->
                RecipeRow(
                    item = item,
                    fields = state.fields,
                    decimals = state.amountDecimals,
                    selected = selected(item.id),
                    index = index,
                    count = group.items.size,
                    onClick = { onRecipeClick(item.id) },
                    menuGroups = menuGroups(item.id),
                )
            }
            RecipesView.GALLERY -> insetItems(
                items = group.items.chunked(columns).withIndex().toList(),
                key = { "${group.title}-row-${it.value.first().id}" },
                contentType = { "recipeRow" },
            ) { (rowIndex, row) ->
                Row(
                    // The rows keep the cards' gap between them, the list only the items' one
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (rowIndex > 0) GalleryRowTopPadding else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(GalleryGap),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { item ->
                        RecipeCard(
                            item = item,
                            fields = state.fields,
                            decimals = state.amountDecimals,
                            selected = selected(item.id),
                            onClick = { onRecipeClick(item.id) },
                            menuGroups = menuGroups(item.id),
                            loadPicture = loadPicture,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // A last row keeps the cards as wide as the full rows
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun groupTitleText(title: RecipeGroupTitle, decimals: Int): String = when (title) {
    is RecipeGroupTitle.Servings -> servingsText(title.servings, decimals)
    is RecipeGroupTitle.DueScore -> stringResource(Res.string.recipes_due_score_value, title.score)
    is RecipeGroupTitle.Status -> stringResource(title.status.label)
    RecipeGroupTitle.None -> stringResource(Res.string.stock_overview_group_not_set)
}

/** Cards of at least [GalleryCardWidth] next to each other, up to [MAX_GALLERY_COLUMNS]. */
internal fun galleryColumns(width: Dp): Int {
    val content = width - GalleryMargins
    return ((content + GalleryGap) / (GalleryCardWidth + GalleryGap)).toInt()
        .coerceIn(1, MAX_GALLERY_COLUMNS)
}

private val GalleryCardWidth = 260.dp
private val GalleryGap = 8.dp
private const val MAX_GALLERY_COLUMNS = 4

// The list's margins of 16.dp at both sides
private val GalleryMargins = 32.dp

// The list puts the segmented gap between its rows, the cards keep their own gap
private val GalleryRowTopPadding = GalleryGap - ListItemDefaults.SegmentedGap

// The list puts the segmented gap between its items, the title adds the rest of a group gap
private val GroupTitleTopPadding =
    SegmentedListDefaults.GroupSpacing - ListItemDefaults.SegmentedGap
