@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.GroupTitle
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.ChipSize
import com.patrickzedler.grocy.core.forms.LocalChipSize
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListItemSync
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.ic_rounded_close
import com.patrickzedler.grocy.core.resources.shopping_list_choose
import com.patrickzedler.grocy.core.resources.shopping_list_empty
import com.patrickzedler.grocy.core.resources.shopping_list_empty_filtered
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_ungrouped
import com.patrickzedler.grocy.core.resources.shopping_mode_auto_sync
import com.patrickzedler.grocy.core.resources.shopping_mode_settings
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_total_value
import com.patrickzedler.grocy.core.ui.KeepScreenOn
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The shopping mode, for the store: the shown shopping list in large type, a tap checks an item
 * off (offline too), done items move to the end with the setting, the list's total value below
 * it. With auto sync the list is downloaded every few seconds while the screen is visible, so
 * everyone shopping together sees what the others checked off. The status filter and table
 * options are the shopping list's; no search, no toolbar, the list chosen in the title. The
 * screen stays on while shown if the user wants it.
 */
@Composable
internal fun ShoppingModeScreen(
    viewModel: ShoppingModeViewModel,
    onCloseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    KeepScreenOn(enabled = settings?.keepScreenOn == true)
    // Only while visible: a list in the background needs no downloads
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.syncWhileShown() }
    }
    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasData == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )

    val current = state
    if (viewModel.isChoosingList && current != null) {
        ShoppingListChoiceDialog(
            lists = current.lists,
            selectedId = current.list?.id,
            onSelect = viewModel::selectList,
            onDismissRequest = viewModel::dismissListChoice,
        )
    }

    SegmentedListScreen(
        title = current?.list?.name ?: stringResource(Res.string.shopping_list_list),
        modifier = modifier,
        onTitleClick = viewModel::chooseList.takeIf { current?.canChooseList == true },
        titleClickLabel = stringResource(Res.string.shopping_list_choose),
        navigationIcon = {
            AppBarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_close),
                contentDescription = stringResource(Res.string.action_close),
                onClick = onCloseClick,
            )
        },
        refreshMenuItems = settings?.let {
            listOf(
                AppBarMenuItem(
                    text = stringResource(Res.string.shopping_mode_auto_sync),
                    checked = it.autoSync,
                    onClick = { viewModel.setAutoSync(!it.autoSync) },
                ),
            )
        }.orEmpty(),
        actionMenuGroups = listOf(
            listOf(
                AppBarMenuItem(
                    text = stringResource(Res.string.shopping_mode_settings),
                    onClick = onSettingsClick,
                ),
            ),
        ),
        snackbarHostState = snackbarHostState,
        // The first download shows its own indicator in the empty list
        isRefreshing = refresh.isRefreshing && current?.hasData == true,
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
    ) {
        if (current == null) return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            CompositionLocalProvider(LocalChipSize provides ChipSize.LARGE) {
                ShoppingListFilterBar(
                    state = current,
                    onStatusChange = viewModel::setStatus,
                    onGroupingChange = viewModel::setGrouping,
                    onFieldShownChange = viewModel::setFieldShown,
                    onClearClick = viewModel::clearFilter,
                    contentPadding = contentPadding,
                )
            }
        }
        shoppingModeRows(
            state = current,
            refresh = refresh,
            onToggleDone = viewModel::toggleDone,
            onRetryClick = viewModel::refresh,
        )
        current.totalValue?.takeIf { !current.isEmpty }?.let { value ->
            insetItem(key = "total") {
                Text(
                    text = stringResource(
                        Res.string.stock_overview_total_value,
                        formatCurrency(value, current.currency, current.priceDecimals),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = GroupTitleTopPadding),
                )
            }
        }
    }
}

private fun LazyListScope.shoppingModeRows(
    state: ShoppingListUiState,
    refresh: RefreshState,
    onToggleDone: (ShoppingListRow) -> Unit,
    onRetryClick: () -> Unit,
) {
    if (state.isEmpty) {
        emptyListItem(
            // Without a filter an empty list means nothing is on it
            hasData = state.hasData && state.status != null,
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
                    style = MaterialTheme.typography.headlineSmallEmphasized,
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
            ShoppingModeRowItem(
                row = row,
                style = style,
                sync = state.syncStates[row.id],
                index = index,
                count = group.rows.size,
                onToggleDone = { onToggleDone(row) },
            )
        }
    }
}

/**
 * A row of the shopping mode: the checkbox shows whether the item is done, a tap anywhere checks
 * it off or on again; the title in headline medium wraps instead of being cut off, struck
 * through once done; note and chips large below it.
 */
@Composable
private fun ShoppingModeRowItem(
    row: ShoppingListRow,
    style: ShoppingListRowStyle,
    sync: ShoppingListItemSync?,
    index: Int,
    count: Int,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val supportingContent = supporting(row, style)
    SegmentedListItem(
        onClick = {
            haptic.performHapticFeedback(
                if (row.done) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
            )
            onToggleDone()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(row.done)
            }
            .clickCursor(),
        leadingContent = {
            LeadingTrailingContentWrapper {
                // The item handles the tap, the checkbox only shows the state
                Checkbox(checked = row.done, onCheckedChange = null)
            }
        },
        supportingContent = supportingContent?.let { content ->
            {
                CompositionLocalProvider(LocalChipSize provides ChipSize.LARGE) {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium, content)
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.title,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.headlineMedium,
                // Grocy writes notes in italics
                fontStyle = if (row.productId == null) FontStyle.Italic else null,
                textDecoration = if (row.done) TextDecoration.LineThrough else null,
                color = if (row.done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    LocalContentColor.current
                },
            )
            SyncIndicator(sync, iconSize = SyncIconSize)
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

// As large as the icons of the larger text
private val SyncIconSize = 24.dp
