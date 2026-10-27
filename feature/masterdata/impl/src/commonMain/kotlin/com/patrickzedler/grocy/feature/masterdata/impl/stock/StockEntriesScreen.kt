@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.stock

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.model.stock.DueStatus
import com.patrickzedler.grocy.core.model.stock.StockEntry
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatRelativeTime
import com.patrickzedler.grocy.core.model.text.timeSpanBetween
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_undo
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_location
import com.patrickzedler.grocy.core.resources.data_never_overdue
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.grocycode_stock_entry
import com.patrickzedler.grocy.core.resources.ic_rounded_box
import com.patrickzedler.grocy.core.resources.ic_rounded_event
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import com.patrickzedler.grocy.core.resources.objects_empty
import com.patrickzedler.grocy.core.resources.objects_empty_filtered
import com.patrickzedler.grocy.core.resources.product_edit
import com.patrickzedler.grocy.core.resources.product_overview
import com.patrickzedler.grocy.core.resources.product_overview_add_to_shopping_list
import com.patrickzedler.grocy.core.resources.product_stock_entries
import com.patrickzedler.grocy.core.resources.product_stock_journal
import com.patrickzedler.grocy.core.resources.product_stock_journal_summary
import com.patrickzedler.grocy.core.resources.stock_booking_failed
import com.patrickzedler.grocy.core.resources.stock_booking_undone
import com.patrickzedler.grocy.core.resources.stock_entry_consume
import com.patrickzedler.grocy.core.resources.stock_entry_consume_spoiled
import com.patrickzedler.grocy.core.resources.stock_entry_consumed
import com.patrickzedler.grocy.core.resources.stock_entry_due
import com.patrickzedler.grocy.core.resources.stock_entry_due_soon
import com.patrickzedler.grocy.core.resources.stock_entry_edit
import com.patrickzedler.grocy.core.resources.stock_entry_expired
import com.patrickzedler.grocy.core.resources.stock_entry_moved
import com.patrickzedler.grocy.core.resources.stock_entry_open
import com.patrickzedler.grocy.core.resources.stock_entry_opened
import com.patrickzedler.grocy.core.resources.stock_entry_opened_message
import com.patrickzedler.grocy.core.resources.stock_entry_overdue
import com.patrickzedler.grocy.core.resources.stock_entry_price
import com.patrickzedler.grocy.core.resources.stock_entry_purchased
import com.patrickzedler.grocy.core.resources.stock_entry_spoiled
import com.patrickzedler.grocy.core.resources.stock_entry_updated
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ClearFilterChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.DropdownFilterChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.FilterChipRow
import com.patrickzedler.grocy.feature.masterdata.impl.objects.emptyListItem
import com.patrickzedler.grocy.feature.masterdata.impl.objects.filterGapItem
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductLinks
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewHost
import com.patrickzedler.grocy.feature.masterdata.impl.overview.ProductOverviewViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.related.ItemButton
import com.patrickzedler.grocy.feature.masterdata.impl.related.ItemButtons
import com.patrickzedler.grocy.feature.masterdata.impl.related.LabelPrintEffect
import com.patrickzedler.grocy.feature.masterdata.impl.related.LabelPrintViewModel
import com.patrickzedler.grocy.feature.masterdata.impl.related.actionMessage
import com.patrickzedler.grocy.feature.masterdata.impl.related.rememberGrocycodeDownload
import com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist.AddToShoppingListViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the stock pages open besides their own content. */
internal class StockPageActions(
    val overview: ProductOverviewViewModel,
    val shoppingList: AddToShoppingListViewModel,
    val labels: LabelPrintViewModel,
    val links: ProductLinks,
)

/**
 * Grocy's "Stock entries": the entries of every product (one product's, opened from it) with
 * consuming, opening and editing each, and the product's pages in each entry's menu.
 */
@Composable
internal fun StockEntriesScreen(
    viewModel: StockEntriesViewModel,
    actions: StockPageActions,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasEntries == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    LabelPrintEffect(actions.labels, snackbarHostState)
    StockEntryEvents(viewModel, snackbarHostState)

    SegmentedListScreen(
        title = stringResource(Res.string.product_stock_entries),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing && state?.hasEntries == true,
        onRefresh = viewModel::refresh,
        itemSpacing = ListItemDefaults.SegmentedGap,
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            StockEntryFilterBar(current, viewModel, contentPadding)
        }
        // Grocy's stock entries have no search; the items keep the other lists' distance
        filterGapItem()
        stockEntryItems(
            state = current,
            refresh = refresh,
            isBooking = viewModel.isBooking,
            onRetryClick = viewModel::refresh,
            menu = { item ->
                stockEntryMenu(
                    item = item,
                    state = current,
                    viewModel = viewModel,
                    actions = actions,
                    onGrocycodeDownload = downloadGrocycode,
                )
            },
            onEditClick = viewModel::edit,
            onConsumeClick = { viewModel.consume(it, spoiled = false) },
            onOpenClick = viewModel::open,
        )
    }

    viewModel.dialog?.let { dialog ->
        StockEntryDialog(
            state = dialog,
            onSave = viewModel::save,
            onDismissRequest = viewModel::dismissDialog,
        )
    }
    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id ->
            state?.products?.firstOrNull { it.id == id }?.name.orEmpty()
        },
    )
}

/** Grocy's messages of the bookings, those that can be undone with "Undo". */
@Composable
private fun StockEntryEvents(
    viewModel: StockEntriesViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            suspend fun showUndoable(message: String, bookingId: Int?) {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = bookingId?.let { getString(Res.string.action_undo) },
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed && bookingId != null) {
                    viewModel.undo(bookingId)
                }
            }
            when (event) {
                is StockEntryEvent.Consumed -> showUndoable(
                    message = getString(
                        Res.string.stock_entry_consumed,
                        event.amount,
                        event.productName,
                    ) + if (event.spoiled) {
                        " (${getString(Res.string.stock_entry_spoiled)})"
                    } else {
                        ""
                    },
                    bookingId = event.bookingId,
                )
                is StockEntryEvent.Opened -> {
                    event.movedTo?.let {
                        snackbarHostState.showSnackbar(getString(Res.string.stock_entry_moved, it))
                    }
                    showUndoable(
                        message = getString(
                            Res.string.stock_entry_opened_message,
                            event.amount,
                            event.productName,
                        ),
                        bookingId = event.bookingId,
                    )
                }
                is StockEntryEvent.Edited ->
                    showUndoable(getString(Res.string.stock_entry_updated), event.bookingId)
                StockEntryEvent.Undone ->
                    snackbarHostState.showSnackbar(getString(Res.string.stock_booking_undone))
                is StockEntryEvent.Failed -> snackbarHostState.showSnackbar(
                    event.error.actionMessage(Res.string.stock_booking_failed),
                )
            }
        }
    }
}

@Composable
private fun StockEntryFilterBar(
    state: StockEntriesUiState,
    viewModel: StockEntriesViewModel,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    val filter = state.filter
    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.data_product),
            options = listOf<Int?>(null) + state.products.map { it.id },
            selected = filter.productId,
            optionLabel = { id -> state.products.firstOrNull { it.id == id }?.name ?: all },
            onSelect = { id -> viewModel.setFilter { it.copy(productId = id) } },
        )
        if (state.config.stockLocationTracking) {
            DropdownFilterChip(
                label = stringResource(Res.string.data_location),
                options = listOf<Int?>(null) + state.locations.map { it.id },
                selected = filter.locationId,
                optionLabel = { id -> state.locations.firstOrNull { it.id == id }?.name ?: all },
                onSelect = { id -> viewModel.setFilter { it.copy(locationId = id) } },
            )
        }
        if (filter.isActive) {
            ClearFilterChip(onClick = viewModel::clearFilter)
        }
    }
}

private fun LazyListScope.stockEntryItems(
    state: StockEntriesUiState,
    refresh: RefreshState,
    isBooking: Boolean,
    onRetryClick: () -> Unit,
    menu: @Composable (StockEntryItem) -> List<List<AppBarMenuItem>>,
    onEditClick: (StockEntry) -> Unit,
    onConsumeClick: (StockEntry) -> Unit,
    onOpenClick: (StockEntry) -> Unit,
) {
    if (state.items.isEmpty()) {
        emptyListItem(
            hasData = state.hasEntries,
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    insetItems(
        items = state.items.withIndex().toList(),
        key = { it.value.entry.id },
        contentType = { "stockEntry" },
    ) { (index, item) ->
        StockEntryListItem(
            item = item,
            state = state,
            index = index,
            count = state.items.size,
            buttons = buildList {
                add(
                    ItemButton(
                        painter = painterResource(Res.drawable.ic_rounded_restaurant),
                        description = stringResource(Res.string.stock_entry_consume),
                        enabled = !isBooking,
                    ) { onConsumeClick(item.entry) },
                )
                if (state.config.stockProductOpenedTracking) {
                    add(
                        ItemButton(
                            painter = painterResource(Res.drawable.ic_rounded_box),
                            description = stringResource(Res.string.stock_entry_open),
                            enabled = item.canOpen && !isBooking,
                        ) { onOpenClick(item.entry) },
                    )
                }
            },
            menuGroups = menu(item),
            onClick = { onEditClick(item.entry) },
        )
    }
}

/** The actions of Grocy's menu of an entry, in its groups. */
@Composable
private fun stockEntryMenu(
    item: StockEntryItem,
    state: StockEntriesUiState,
    viewModel: StockEntriesViewModel,
    actions: StockPageActions,
    onGrocycodeDownload: (code: String, twoDimensional: Boolean) -> Unit,
): List<List<AppBarMenuItem>> {
    val entry = item.entry
    val productId = entry.productId
    val config = state.config
    val entryName = stringResource(Res.string.grocycode_stock_entry)
    val code = Grocycode.stockEntry(productId, entry.stockId)
    return listOf(
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.stock_entry_edit)) { viewModel.edit(entry) },
            AppBarMenuItem(stringResource(Res.string.product_overview_add_to_shopping_list)) {
                actions.shoppingList.open(productId)
            }.takeIf { config.shoppingList },
        ),
        listOf(
            AppBarMenuItem(stringResource(Res.string.stock_entry_consume_spoiled)) {
                viewModel.consume(entry, spoiled = true)
            },
        ),
        listOf(
            AppBarMenuItem(stringResource(Res.string.product_overview)) {
                actions.overview.show(productId)
            },
            AppBarMenuItem(stringResource(Res.string.product_stock_journal)) {
                actions.links.onStockJournalClick(productId)
            },
            AppBarMenuItem(stringResource(Res.string.product_stock_journal_summary)) {
                actions.links.onStockJournalSummaryClick(productId)
            },
            AppBarMenuItem(stringResource(Res.string.product_edit)) {
                actions.links.onEditClick(productId)
            },
        ),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, entryName)) {
                onGrocycodeDownload(code, config.grocycode2D)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, entryName)) {
                actions.labels.printStockEntry(entry.id)
            }.takeIf { config.labelPrinter && config.labelPrinterRunsOnServer },
        ),
    )
}

@Composable
private fun StockEntryListItem(
    item: StockEntryItem,
    state: StockEntriesUiState,
    index: Int,
    count: Int,
    buttons: List<ItemButton>,
    menuGroups: List<List<AppBarMenuItem>>,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val status = item.dueStatus
    val statusText = when (status) {
        DueStatus.DUE_SOON -> stringResource(Res.string.stock_entry_due_soon)
        DueStatus.OVERDUE -> stringResource(Res.string.stock_entry_overdue)
        DueStatus.EXPIRED -> stringResource(Res.string.stock_entry_expired)
        null -> null
    }
    val today = remember { todayEpochDay() }
    val dueLine = when {
        item.neverOverdue -> stringResource(
            Res.string.stock_entry_due,
            stringResource(Res.string.data_never_overdue),
        )
        item.dueDay != null -> {
            val relative = formatRelativeTime(timeSpanBetween(today, item.dueDay))
            stringResource(Res.string.stock_entry_due, "${formatDate(item.dueDay)} ($relative)") +
                statusText?.let { " · $it" }.orEmpty()
        }
        else -> null
    }
    val priceLine = item.price?.let {
        stringResource(
            Res.string.stock_entry_price,
            formatCurrency(it.price, state.currency, state.priceDecimals),
            it.unit,
        )
    }
    val lines = listOfNotNull(
        dueLine,
        item.locationName,
        listOfNotNull(item.storeName, priceLine).joinToString(" · ").ifEmpty { null },
        item.purchasedDay?.let { stringResource(Res.string.stock_entry_purchased, formatDate(it)) },
        item.entry.note,
    )
    val opened = stringResource(Res.string.stock_entry_opened)

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        modifier = Modifier.fillMaxWidth().clickCursor(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        // One product's page names it in its filter already
        overlineContent = if (state.filter.productId == null) {
            { Text(item.productName) }
        } else {
            null
        },
        leadingContent = status?.let {
            {
                LeadingTrailingContentWrapper {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_event),
                        contentDescription = null,
                        tint = it.color(),
                    )
                }
            }
        },
        supportingContent = { SupportingLines(lines) },
        trailingContent = { ItemButtons(buttons, menuGroups) },
    ) {
        Text(if (item.entry.open) "${item.amount} · $opened" else item.amount)
    }
}

/** Grocy's row colors: red when expired, grey when overdue, yellow when due soon. */
@Composable
private fun DueStatus.color(): Color = when (this) {
    DueStatus.EXPIRED -> MaterialTheme.colorScheme.error
    DueStatus.OVERDUE -> MaterialTheme.colorScheme.onSurfaceVariant
    DueStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
}
