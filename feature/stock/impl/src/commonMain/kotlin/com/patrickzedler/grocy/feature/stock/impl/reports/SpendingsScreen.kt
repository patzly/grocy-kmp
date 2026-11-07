package com.patrickzedler.grocy.feature.stock.impl.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.data.util.RefreshState
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.edgeToEdgeItem
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.insetItems
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.ClearFilterChip
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.emptyListItem
import com.patrickzedler.grocy.core.model.stock.ProductGroupFilter
import com.patrickzedler.grocy.core.model.stock.SpendingsGrouping
import com.patrickzedler.grocy.core.model.stock.SpendingsRangePreset
import com.patrickzedler.grocy.core.model.stock.SpendingsRow
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_ok
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.data_product_group
import com.patrickzedler.grocy.core.resources.data_store
import com.patrickzedler.grocy.core.resources.journal_date_range
import com.patrickzedler.grocy.core.resources.shopping_list_ungrouped
import com.patrickzedler.grocy.core.resources.spendings_chart_description
import com.patrickzedler.grocy.core.resources.spendings_empty
import com.patrickzedler.grocy.core.resources.spendings_other
import com.patrickzedler.grocy.core.resources.spendings_percent
import com.patrickzedler.grocy.core.resources.spendings_range_custom
import com.patrickzedler.grocy.core.resources.spendings_range_last_14_days
import com.patrickzedler.grocy.core.resources.spendings_range_last_30_days
import com.patrickzedler.grocy.core.resources.spendings_range_last_7_days
import com.patrickzedler.grocy.core.resources.spendings_range_last_month
import com.patrickzedler.grocy.core.resources.spendings_range_last_year
import com.patrickzedler.grocy.core.resources.spendings_range_this_month
import com.patrickzedler.grocy.core.resources.spendings_range_this_year
import com.patrickzedler.grocy.core.resources.spendings_range_today
import com.patrickzedler.grocy.core.resources.spendings_range_yesterday
import com.patrickzedler.grocy.core.resources.spendings_total
import com.patrickzedler.grocy.core.resources.stock_overview_group_not_set
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.resources.stock_report_spendings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's stock report "Spendings": "Group by", the date range and, grouped by product, the
 * product group as chips; the doughnut of the totals and Grocy's table as list below it, each
 * row with its slice's color, its total and its share.
 */
@Composable
internal fun SpendingsScreen(
    viewModel: SpendingsViewModel,
    menuItems: List<AppBarMenuItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var selectedSlice by rememberSaveable { mutableStateOf<Int?>(null) }
    var pickingRange by rememberSaveable { mutableStateOf(false) }

    SegmentedListScreen(
        title = stringResource(Res.string.stock_report_spendings),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
        menuItems = menuItems,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
    ) {
        val current = state ?: return@SegmentedListScreen
        edgeToEdgeItem(key = "filters") { contentPadding ->
            SpendingsFilterBar(
                state = current,
                contentPadding = contentPadding,
                onGroupingChange = {
                    selectedSlice = null
                    viewModel.setGrouping(it)
                },
                onRangeChange = { preset ->
                    if (preset == SpendingsRangePreset.CUSTOM) {
                        pickingRange = true
                    } else {
                        selectedSlice = null
                        viewModel.setRange(preset)
                    }
                },
                onProductGroupChange = {
                    selectedSlice = null
                    viewModel.setProductGroup(it)
                },
                onClearClick = {
                    selectedSlice = null
                    viewModel.clearFilter()
                },
            )
        }
        spendingsItems(
            state = current,
            selectedSlice = selectedSlice?.takeIf { it < current.chart.slices.size },
            onSelectSlice = { selectedSlice = it },
            onRetryClick = viewModel::refresh,
        )
    }

    if (pickingRange) {
        state?.let { current ->
            RangePickerDialog(
                start = current.filter.range.start,
                end = current.filter.range.end,
                onPick = { start, end ->
                    pickingRange = false
                    selectedSlice = null
                    viewModel.setCustomRange(start, end)
                },
                onDismissRequest = { pickingRange = false },
            )
        }
    }
}

@Composable
private fun SpendingsFilterBar(
    state: SpendingsUiState,
    contentPadding: PaddingValues,
    onGroupingChange: (SpendingsGrouping) -> Unit,
    onRangeChange: (SpendingsRangePreset) -> Unit,
    onProductGroupChange: (ProductGroupFilter) -> Unit,
    onClearClick: () -> Unit,
) {
    val filter = state.filter
    FilterChipRow(contentPadding = contentPadding) {
        // Always grouped by something, the chip names it
        val groupedBy = stringResource(
            Res.string.stock_overview_grouped_by,
            stringResource(filter.grouping.label),
        )
        DropdownFilterChip(
            label = groupedBy,
            optionGroups = listOf(SpendingsGrouping.entries),
            selected = filter.grouping,
            optionLabel = { stringResource(it.label) },
            onSelect = onGroupingChange,
            isDefault = { it == SpendingsGrouping.PRODUCT },
            selectedLabel = {
                stringResource(Res.string.stock_overview_grouped_by, stringResource(it.label))
            },
        )
        val range = filter.range
        val rangeText = if (range.preset == SpendingsRangePreset.CUSTOM) {
            "${formatDate(range.start)} – ${formatDate(range.end)}"
        } else {
            stringResource(range.preset.label)
        }
        DropdownFilterChip(
            label = rangeText,
            optionGroups = listOf(SpendingsRangePreset.entries),
            selected = range.preset,
            optionLabel = { stringResource(it.label) },
            onSelect = onRangeChange,
            isDefault = { it == SpendingsRangePreset.THIS_MONTH },
            selectedLabel = { rangeText },
        )
        // Grocy offers the group only beside the grouping by product
        if (filter.grouping == SpendingsGrouping.PRODUCT) {
            val all = stringResource(Res.string.data_all)
            val ungrouped = stringResource(Res.string.shopping_list_ungrouped)
            DropdownFilterChip(
                label = stringResource(Res.string.data_product_group),
                optionGroups = listOf(
                    listOf(ProductGroupFilter.All),
                    state.productGroups.map { ProductGroupFilter.Group(it.id) },
                    listOf(ProductGroupFilter.Ungrouped),
                ),
                selected = filter.productGroup,
                optionLabel = { option ->
                    when (option) {
                        ProductGroupFilter.All -> all
                        ProductGroupFilter.Ungrouped -> ungrouped
                        is ProductGroupFilter.Group ->
                            state.productGroups.firstOrNull { it.id == option.id }?.name.orEmpty()
                    }
                },
                onSelect = onProductGroupChange,
                isDefault = { it == ProductGroupFilter.All },
            )
        }
        if (!state.isDefaultFilter) ClearFilterChip(onClick = onClearClick)
    }
}

private fun LazyListScope.spendingsItems(
    state: SpendingsUiState,
    selectedSlice: Int?,
    onSelectSlice: (Int?) -> Unit,
    onRetryClick: () -> Unit,
) {
    val load = state.load
    if (state.rows.isEmpty() || load !is SpendingsLoad.Loaded) {
        emptyListItem(
            hasData = false,
            refresh = when (load) {
                SpendingsLoad.Loading -> RefreshState(isRefreshing = true)
                is SpendingsLoad.Failed -> RefreshState(
                    error = if (load.error == ChangeError.Offline) {
                        RefreshError.OFFLINE
                    } else {
                        RefreshError.FAILED
                    },
                )
                is SpendingsLoad.Loaded -> RefreshState()
            },
            emptyText = { stringResource(Res.string.spendings_empty) },
            filteredEmptyText = { stringResource(Res.string.spendings_empty) },
            onRetryClick = onRetryClick,
        )
        return
    }
    val chart = state.chart
    insetItem(key = "chart") {
        val totalText = formatCurrency(chart.total, state.currency, state.priceDecimals)
        val other = stringResource(Res.string.spendings_other)
        // The selected slice's total and name in the middle instead of the whole total
        val selectedText = selectedSlice?.let { chart.slices[it] }?.let { slice ->
            val name = slice.row?.let { rowName(it, state.filter.grouping) } ?: other
            formatCurrency(slice.total, state.currency, state.priceDecimals) to name
        }
        SegmentedContainer(index = 0, count = 1) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SpendingsDonut(
                    chart = chart,
                    totalText = totalText,
                    totalLabel = stringResource(Res.string.spendings_total),
                    selected = selectedSlice,
                    selectedText = selectedText,
                    description = stringResource(Res.string.spendings_chart_description, totalText),
                    onSelect = onSelectSlice,
                )
            }
        }
    }
    insetItems(
        items = state.rows.withIndex().toList(),
        key = { "row-${state.filter.grouping}-${it.value.id}" },
        contentType = { "spendings" },
    ) { (index, row) ->
        val slot = state.chart.colorOf[index]
        val sliceIndex = chart.slices.indexOfFirst { slice ->
            if (slot == null) slice.slot == null else slice.slot == slot
        }.takeIf { it >= 0 }
        SpendingsRowItem(
            row = row,
            grouping = state.filter.grouping,
            slot = slot,
            share = if (chart.total > 0.0) row.total / chart.total else 0.0,
            amountText = formatCurrency(row.total, state.currency, state.priceDecimals),
            isSelected = sliceIndex != null && sliceIndex == selectedSlice,
            onClick = { onSelectSlice(sliceIndex.takeIf { it != selectedSlice }) },
            index = index,
            count = state.rows.size,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SpendingsRowItem(
    row: SpendingsRow,
    grouping: SpendingsGrouping,
    slot: Int?,
    share: Double,
    amountText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
) {
    val haptic = LocalHapticFeedback.current
    val name = rowName(row, grouping)
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        selected = isSelected,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Box(
                    Modifier
                        .size(16.dp)
                        .background(spendingsColor(slot), CircleShape),
                )
            }
        },
        supportingContent = if (grouping == SpendingsGrouping.PRODUCT) {
            {
                Text(
                    text = row.productGroup ?: stringResource(Res.string.shopping_list_ungrouped),
                    fontStyle = if (row.productGroup == null) FontStyle.Italic else null,
                )
            }
        } else {
            null
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge.withTabularNumbers(),
                )
                Text(
                    text = stringResource(Res.string.spendings_percent, percentText(share)),
                    style = MaterialTheme.typography.bodySmall.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = name,
                fontStyle = if (row.name == null) FontStyle.Italic else null,
            )
        },
    )
}

/** The row's name; Grocy writes its ungrouped products in italics, so does a missing store. */
@Composable
private fun rowName(row: SpendingsRow, grouping: SpendingsGrouping): String = row.name
    ?: stringResource(
        if (grouping == SpendingsGrouping.STORE) {
            Res.string.stock_overview_group_not_set
        } else {
            Res.string.shopping_list_ungrouped
        },
    )

/** The share in percent, one decimal below 10 %, where whole numbers would say too little. */
internal fun percentText(share: Double, separator: Char = platformDecimalSeparator()): String {
    val percent = share * 100
    return formatDecimal(percent, if (percent < 10) 1 else 0, separator)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RangePickerDialog(
    start: Long,
    end: Long,
    onPick: (start: Long, end: Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // The picker counts in UTC milliseconds
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start * MILLIS_PER_DAY,
        initialSelectedEndDateMillis = end * MILLIS_PER_DAY,
    )
    val pickedStart = pickerState.selectedStartDateMillis
    val pickedEnd = pickerState.selectedEndDateMillis
    val canPick = pickedStart != null && pickedEnd != null
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    if (pickedStart != null && pickedEnd != null) {
                        onPick(
                            pickedStart.floorDiv(MILLIS_PER_DAY),
                            pickedEnd.floorDiv(MILLIS_PER_DAY),
                        )
                    }
                },
                enabled = canPick,
                modifier = Modifier.clickCursor(enabled = canPick),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptic.confirm()
                    onDismissRequest()
                },
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.weight(1f),
            title = {
                Text(
                    text = stringResource(Res.string.journal_date_range),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

private val SpendingsGrouping.label: StringResource
    get() = when (this) {
        SpendingsGrouping.PRODUCT -> Res.string.data_product
        SpendingsGrouping.PRODUCT_GROUP -> Res.string.data_product_group
        SpendingsGrouping.STORE -> Res.string.data_store
    }

private val SpendingsRangePreset.label: StringResource
    get() = when (this) {
        SpendingsRangePreset.TODAY -> Res.string.spendings_range_today
        SpendingsRangePreset.YESTERDAY -> Res.string.spendings_range_yesterday
        SpendingsRangePreset.LAST_7_DAYS -> Res.string.spendings_range_last_7_days
        SpendingsRangePreset.LAST_14_DAYS -> Res.string.spendings_range_last_14_days
        SpendingsRangePreset.LAST_30_DAYS -> Res.string.spendings_range_last_30_days
        SpendingsRangePreset.THIS_MONTH -> Res.string.spendings_range_this_month
        SpendingsRangePreset.LAST_MONTH -> Res.string.spendings_range_last_month
        SpendingsRangePreset.THIS_YEAR -> Res.string.spendings_range_this_year
        SpendingsRangePreset.LAST_YEAR -> Res.string.spendings_range_last_year
        SpendingsRangePreset.CUSTOM -> Res.string.spendings_range_custom
    }
