@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.journal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarAction
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.forms.ClearFilterChip
import com.patrickzedler.grocy.core.forms.DisabledAlpha
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.ItemButton
import com.patrickzedler.grocy.core.forms.ItemButtons
import com.patrickzedler.grocy.core.forms.actionMessage
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.forms.searchItem
import com.patrickzedler.grocy.core.forms.showSearchAtTop
import com.patrickzedler.grocy.core.model.stock.StockTransactionType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatRelativeTime
import com.patrickzedler.grocy.core.model.text.timeOfDayOf
import com.patrickzedler.grocy.core.model.text.timeSpanBetween
import com.patrickzedler.grocy.core.model.text.todayEpochDay
import com.patrickzedler.grocy.core.productcard.LabelPrintEffect
import com.patrickzedler.grocy.core.productcard.ProductOverviewHost
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.productcard.rememberGrocycodeDownload
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_search
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_location
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.grocycode_download_of
import com.patrickzedler.grocy.core.resources.grocycode_print_of
import com.patrickzedler.grocy.core.resources.grocycode_product
import com.patrickzedler.grocy.core.resources.ic_rounded_search
import com.patrickzedler.grocy.core.resources.ic_rounded_undo
import com.patrickzedler.grocy.core.resources.journal_date_range
import com.patrickzedler.grocy.core.resources.journal_done_by
import com.patrickzedler.grocy.core.resources.journal_months
import com.patrickzedler.grocy.core.resources.journal_transaction_time
import com.patrickzedler.grocy.core.resources.journal_transaction_type
import com.patrickzedler.grocy.core.resources.journal_undo
import com.patrickzedler.grocy.core.resources.journal_undone_on
import com.patrickzedler.grocy.core.resources.journal_user
import com.patrickzedler.grocy.core.resources.journal_years
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
import com.patrickzedler.grocy.core.resources.stock_entry_spoiled
import com.patrickzedler.grocy.core.resources.transaction_consume
import com.patrickzedler.grocy.core.resources.transaction_inventory_correction
import com.patrickzedler.grocy.core.resources.transaction_product_opened
import com.patrickzedler.grocy.core.resources.transaction_purchase
import com.patrickzedler.grocy.core.resources.transaction_self_production
import com.patrickzedler.grocy.core.resources.transaction_stock_edit_new
import com.patrickzedler.grocy.core.resources.transaction_stock_edit_old
import com.patrickzedler.grocy.core.resources.transaction_transfer_from
import com.patrickzedler.grocy.core.resources.transaction_transfer_to
import com.patrickzedler.grocy.core.scanner.writer.Grocycode
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Stock journal" with undoing bookings and the product's pages in each booking's
 * menu, or its "Stock journal summary" of the sums per product, type and user.
 */
@Composable
internal fun StockJournalScreen(
    viewModel: StockJournalViewModel,
    actions: StockPageActions,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)

    RefreshErrorEffect(
        error = refresh.error,
        hasData = state?.hasBookings == true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )
    LabelPrintEffect(actions.labels, snackbarHostState)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    JournalEvent.Undone -> getString(Res.string.stock_booking_undone)
                    is JournalEvent.Failed ->
                        event.error.actionMessage(Res.string.stock_booking_failed)
                },
            )
        }
    }
    // Back closes the search first and stays on the page
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = viewModel.isSearchVisible,
        onBackCompleted = viewModel::hideSearch,
    )

    SegmentedListScreen(
        title = stringResource(
            if (viewModel.summary) {
                Res.string.product_stock_journal_summary
            } else {
                Res.string.product_stock_journal
            },
        ),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        actions = listOf(
            AppBarAction { actionModifier, interactionSource ->
                AppBarIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_search),
                    contentDescription = stringResource(Res.string.action_search),
                    onClick = {
                        if (viewModel.isSearchVisible) {
                            viewModel.hideSearch()
                        } else {
                            scope.showSearchAtTop(listState, viewModel::showSearch)
                        }
                    },
                    modifier = actionModifier,
                    interactionSource = interactionSource,
                )
            },
        ),
        menuItems = menuItems,
        snackbarHostState = snackbarHostState,
        isRefreshing = refresh.isRefreshing && state?.hasBookings == true,
        onRefresh = viewModel::refresh,
        listState = listState,
        itemSpacing = ListItemDefaults.SegmentedGap,
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            JournalFilterBar(current, viewModel, contentPadding)
        }
        searchItem(
            isVisible = viewModel.isSearchVisible,
            state = viewModel.searchState,
            onCloseClick = viewModel::hideSearch,
        )
        if (viewModel.summary) {
            summaryItems(current, refresh, onRetryClick = viewModel::refresh)
        } else {
            journalItems(
                state = current,
                refresh = refresh,
                isUndoing = viewModel.isUndoing,
                onRetryClick = viewModel::refresh,
                onUndoClick = { viewModel.undo(it.booking) },
                onItemClick = { actions.overview.show(it.booking.productId) },
                menu = { item ->
                    journalMenu(item, current, actions, downloadGrocycode)
                },
            )
        }
    }

    ProductOverviewHost(
        overview = actions.overview,
        shoppingList = actions.shoppingList,
        labels = actions.labels,
        snackbarHostState = snackbarHostState,
        links = actions.links,
        nameOf = { id -> state?.products?.firstOrNull { it.id == id }?.name.orEmpty() },
    )
}

@Composable
private fun JournalFilterBar(
    state: JournalUiState,
    viewModel: StockJournalViewModel,
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
        DropdownFilterChip(
            label = stringResource(Res.string.journal_transaction_type),
            options = listOf<StockTransactionType?>(null) + StockTransactionType.entries,
            selected = filter.transactionType,
            optionLabel = { type -> type?.let { stringResource(it.label) } ?: all },
            onSelect = { type -> viewModel.setFilter { it.copy(transactionType = type) } },
        )
        if (!viewModel.summary && state.config.stockLocationTracking) {
            DropdownFilterChip(
                label = stringResource(Res.string.data_location),
                options = listOf<Int?>(null) + state.locations.map { it.id },
                selected = filter.locationId,
                optionLabel = { id -> state.locations.firstOrNull { it.id == id }?.name ?: all },
                onSelect = { id -> viewModel.setFilter { it.copy(locationId = id) } },
            )
        }
        if (state.users.isNotEmpty()) {
            DropdownFilterChip(
                label = stringResource(Res.string.journal_user),
                options = listOf<Int?>(null) + state.users.map { it.id },
                selected = filter.userId,
                optionLabel = { id ->
                    state.users.firstOrNull { it.id == id }?.displayName ?: all
                },
                onSelect = { id -> viewModel.setFilter { it.copy(userId = id) } },
            )
        }
        if (!viewModel.summary) {
            DropdownFilterChip(
                label = stringResource(Res.string.journal_date_range),
                options = JournalRanges,
                selected = filter.months,
                optionLabel = { months -> rangeLabel(months, all) },
                onSelect = { months -> viewModel.setFilter { it.copy(months = months) } },
                isDefault = { it == JOURNAL_DEFAULT_MONTHS },
            )
        }
        val isFiltered = if (viewModel.summary) {
            filter.copy(months = JOURNAL_DEFAULT_MONTHS).isActive
        } else {
            filter.isActive
        }
        if (isFiltered) {
            ClearFilterChip(onClick = viewModel::clearFilter)
        }
    }
}

/** Grocy's ranges: "1 month", "6 months", "1 year", "2 years", "All". */
@Composable
private fun rangeLabel(months: Int, all: String): String = when {
    months >= JOURNAL_ALL_MONTHS -> all
    months % 12 == 0 -> pluralStringResource(Res.plurals.journal_years, months / 12, months / 12)
    else -> pluralStringResource(Res.plurals.journal_months, months, months)
}

private fun LazyListScope.journalItems(
    state: JournalUiState,
    refresh: RefreshState,
    isUndoing: Boolean,
    onRetryClick: () -> Unit,
    onUndoClick: (JournalItem) -> Unit,
    onItemClick: (JournalItem) -> Unit,
    menu: @Composable (JournalItem) -> List<List<AppBarMenuItem>>,
) {
    if (state.items.isEmpty()) {
        emptyListItem(
            hasData = state.hasBookings,
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    insetItems(
        items = state.items.withIndex().toList(),
        key = { it.value.booking.id },
        contentType = { "booking" },
    ) { (index, item) ->
        JournalListItem(
            item = item,
            index = index,
            count = state.items.size,
            buttons = listOf(
                ItemButton(
                    painter = painterResource(Res.drawable.ic_rounded_undo),
                    description = stringResource(Res.string.journal_undo),
                    enabled = !item.booking.undone && !isUndoing,
                ) { onUndoClick(item) },
            ),
            menuGroups = menu(item),
            onClick = { onItemClick(item) },
        )
    }
}

/** The actions of Grocy's menu of a booking, in its groups. */
@Composable
private fun journalMenu(
    item: JournalItem,
    state: JournalUiState,
    actions: StockPageActions,
    onGrocycodeDownload: (code: String, twoDimensional: Boolean) -> Unit,
): List<List<AppBarMenuItem>> {
    val productId = item.booking.productId
    val config = state.config
    val productName = stringResource(Res.string.grocycode_product)
    val code = Grocycode.product(productId)
    return listOf(
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.product_overview_add_to_shopping_list)) {
                actions.shoppingList.open(productId)
            }.takeIf { config.shoppingList },
        ),
        listOf(
            AppBarMenuItem(stringResource(Res.string.product_overview)) {
                actions.overview.show(productId)
            },
            AppBarMenuItem(stringResource(Res.string.product_stock_entries)) {
                actions.links.onStockEntriesClick(productId)
            },
            AppBarMenuItem(stringResource(Res.string.product_stock_journal_summary)) {
                actions.links.onStockJournalSummaryClick(productId)
            },
            AppBarMenuItem(stringResource(Res.string.product_edit)) {
                actions.links.onEditClick(productId)
            },
        ),
        listOfNotNull(
            AppBarMenuItem(stringResource(Res.string.grocycode_download_of, productName)) {
                onGrocycodeDownload(code, config.grocycode2D)
            },
            AppBarMenuItem(stringResource(Res.string.grocycode_print_of, productName)) {
                actions.labels.printProduct(productId)
            }.takeIf { config.labelPrinter && config.labelPrinterRunsOnServer },
        ),
    )
}

@Composable
private fun JournalListItem(
    item: JournalItem,
    index: Int,
    count: Int,
    buttons: List<ItemButton>,
    menuGroups: List<List<AppBarMenuItem>>,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val booking = item.booking
    val spoiled = stringResource(Res.string.stock_entry_spoiled)
    val type = item.type?.let { stringResource(it.label) } ?: booking.transactionType
    val lines = listOfNotNull(
        item.amount,
        dateTimeText(booking.createdTimestamp)
            ?.let { stringResource(Res.string.journal_transaction_time, it) },
        item.locationName,
        item.userName?.let { stringResource(Res.string.journal_done_by, it) },
        booking.note,
        booking.undoneTimestamp?.takeIf { booking.undone }
            ?.let { dateTimeText(it) ?: it }
            ?.let { stringResource(Res.string.journal_undone_on, it) },
    )
    // Undone bookings are muted and struck through like Grocy's rows
    val contentAlpha = if (booking.undone) DisabledAlpha else 1f

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        modifier = Modifier.fillMaxWidth().clickCursor(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        overlineContent = {
            Text(
                text = if (booking.spoiled) "$type · $spoiled" else type,
                modifier = Modifier.alpha(contentAlpha),
            )
        },
        supportingContent = {
            SupportingLines(lines, modifier = Modifier.alpha(contentAlpha))
        },
        trailingContent = { ItemButtons(buttons, menuGroups) },
    ) {
        Text(
            text = item.productName,
            modifier = Modifier.alpha(contentAlpha),
            textDecoration = if (booking.undone) TextDecoration.LineThrough else null,
        )
    }
}

private fun LazyListScope.summaryItems(
    state: JournalUiState,
    refresh: RefreshState,
    onRetryClick: () -> Unit,
) {
    if (state.summary.isEmpty()) {
        emptyListItem(
            hasData = state.hasBookings,
            refresh = refresh,
            emptyText = { stringResource(Res.string.objects_empty) },
            filteredEmptyText = { stringResource(Res.string.objects_empty_filtered) },
            onRetryClick = onRetryClick,
        )
        return
    }
    insetItems(
        items = state.summary.withIndex().toList(),
        key = { (_, item) -> "${item.productId}-${item.type}-${item.userName}" },
        contentType = { "summary" },
    ) { (index, item) ->
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            shapes = ListItemDefaults.segmentedShapes(index = index, count = state.summary.size),
            colors = SegmentedListDefaults.colors(),
            overlineContent = {
                Text(item.type?.let { stringResource(it.label) }.orEmpty())
            },
            supportingContent = {
                SupportingLines(listOfNotNull(item.amount, item.userName))
            },
            content = { Text(item.productName) },
        )
    }
}

/** Grocy's "2026-10-22 18:00:00" as date, time and how long ago, e.g. "22.10.2026 18:00". */
@Composable
private fun dateTimeText(timestamp: String): String? {
    val day = epochDayOf(timestamp) ?: return null
    val today = remember { todayEpochDay() }
    val time = timeOfDayOf(timestamp)?.let { (hour, minute) ->
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
    val relative = formatRelativeTime(timeSpanBetween(today, day))
    return listOfNotNull(formatDate(day), time).joinToString(" ") + " ($relative)"
}

/** Grocy's names of the transaction types. */
internal val StockTransactionType.label: StringResource
    get() = when (this) {
        StockTransactionType.CONSUME -> Res.string.transaction_consume
        StockTransactionType.INVENTORY_CORRECTION -> Res.string.transaction_inventory_correction
        StockTransactionType.PRODUCT_OPENED -> Res.string.transaction_product_opened
        StockTransactionType.PURCHASE -> Res.string.transaction_purchase
        StockTransactionType.SELF_PRODUCTION -> Res.string.transaction_self_production
        StockTransactionType.STOCK_EDIT_NEW -> Res.string.transaction_stock_edit_new
        StockTransactionType.STOCK_EDIT_OLD -> Res.string.transaction_stock_edit_old
        StockTransactionType.TRANSFER_FROM -> Res.string.transaction_transfer_from
        StockTransactionType.TRANSFER_TO -> Res.string.transaction_transfer_to
    }
