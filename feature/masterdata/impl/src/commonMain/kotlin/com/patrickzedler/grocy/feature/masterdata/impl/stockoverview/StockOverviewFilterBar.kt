package com.patrickzedler.grocy.feature.masterdata.impl.stockoverview

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.model.stock.StockOverviewField
import com.patrickzedler.grocy.core.model.stock.StockOverviewGrouping
import com.patrickzedler.grocy.core.model.stock.StockOverviewOptions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_location
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_per_stock_unit
import com.patrickzedler.grocy.core.resources.data_product_group
import com.patrickzedler.grocy.core.resources.product_overview_average_price
import com.patrickzedler.grocy.core.resources.product_overview_last_price
import com.patrickzedler.grocy.core.resources.product_overview_last_purchased
import com.patrickzedler.grocy.core.resources.product_parent
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_min_stock_amount
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.products_sort_store
import com.patrickzedler.grocy.core.resources.stock_entry_due_soon
import com.patrickzedler.grocy.core.resources.stock_entry_expired
import com.patrickzedler.grocy.core.resources.stock_entry_opened
import com.patrickzedler.grocy.core.resources.stock_entry_overdue
import com.patrickzedler.grocy.core.resources.stock_overview_below_min_stock
import com.patrickzedler.grocy.core.resources.stock_overview_default_location
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.resources.stock_overview_in_stock
import com.patrickzedler.grocy.core.resources.stock_overview_next_due_date
import com.patrickzedler.grocy.core.resources.stock_overview_not_fresh
import com.patrickzedler.grocy.core.resources.stock_overview_product_description
import com.patrickzedler.grocy.core.resources.stock_overview_product_picture
import com.patrickzedler.grocy.core.resources.stock_overview_status_below_min_stock
import com.patrickzedler.grocy.core.resources.stock_overview_status_due_soon
import com.patrickzedler.grocy.core.resources.stock_overview_status_expired
import com.patrickzedler.grocy.core.resources.stock_overview_status_in_stock
import com.patrickzedler.grocy.core.resources.stock_overview_status_not_fresh
import com.patrickzedler.grocy.core.resources.stock_overview_status_opened
import com.patrickzedler.grocy.core.resources.stock_overview_status_overdue
import com.patrickzedler.grocy.core.resources.stock_overview_value
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ClearFilterChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.DropdownFilterChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.FilterChipRow
import com.patrickzedler.grocy.feature.masterdata.impl.objects.MultiSelectChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.SortChip
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The filters of Grocy's stock overview as chips, status first (with the number of products
 * of each, which Grocy shows as colored messages above its table), then location and product
 * group, followed by its table options: sort, grouping and the shown fields. "Clear filter"
 * while a filter is set.
 */
@Composable
internal fun StockOverviewFilterBar(
    state: StockOverviewUiState,
    energyUnit: String,
    onFilterChange: ((StockOverviewFilter) -> StockOverviewFilter) -> Unit,
    onSortChange: (StockOverviewField, Boolean) -> Unit,
    onGroupingChange: (StockOverviewGrouping) -> Unit,
    onFieldShownChange: (StockOverviewField, Boolean) -> Unit,
    onClearClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    val filter = state.filter
    val statuses = state.availableStatuses

    FilterChipRow(contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_status),
            // "All" alone, the freshness, then the amounts
            optionGroups = listOf(
                listOf<StockStatus?>(null),
                statuses.filter { it in FreshnessStatuses },
                statuses.filter { it !in FreshnessStatuses },
            ),
            selected = filter.status,
            optionLabel = { status ->
                status?.let { stringResource(it.countLabel, state.statusCounts[it] ?: 0) } ?: all
            },
            selectedLabel = { status -> status?.let { stringResource(it.label) } ?: all },
            onSelect = { status -> onFilterChange { it.copy(status = status) } },
        )
        if (state.config.stockLocationTracking) {
            DropdownFilterChip(
                label = stringResource(Res.string.data_location),
                options = listOf<Int?>(null) + state.locations.map { it.id },
                selected = filter.locationId,
                optionLabel = { id -> state.locations.firstOrNull { it.id == id }?.name ?: all },
                onSelect = { id -> onFilterChange { it.copy(locationId = id) } },
            )
        }
        DropdownFilterChip(
            label = stringResource(Res.string.data_product_group),
            options = listOf<Int?>(null) + state.productGroups.map { it.id },
            selected = filter.productGroupId,
            optionLabel = { id -> state.productGroups.firstOrNull { it.id == id }?.name ?: all },
            onSelect = { id -> onFilterChange { it.copy(productGroupId = id) } },
        )
        SortChip(
            fields = sortFields(state.fields),
            field = state.sort.field,
            ascending = state.sort.ascending,
            isDefault = state.isDefaultSort,
            fieldLabel = { fieldLabel(it, energyUnit) },
            onSortChange = onSortChange,
        )
        DropdownFilterChip(
            label = stringResource(Res.string.stock_overview_group_by),
            optionGroups = listOf(state.availableGroupings),
            selected = state.grouping,
            isDefault = { it == StockOverviewGrouping.NONE },
            optionLabel = { stringResource(it.label) },
            selectedLabel = {
                stringResource(Res.string.stock_overview_grouped_by, stringResource(it.label))
            },
            onSelect = onGroupingChange,
        )
        MultiSelectChip(
            label = stringResource(Res.string.stock_overview_fields),
            options = state.availableFields,
            checked = state.fields.toSet(),
            isDefault = state.fields.toSet() ==
                StockOverviewOptions.DEFAULT_FIELDS.intersect(state.availableFields.toSet()),
            optionLabel = { fieldLabel(it, energyUnit) },
            onCheckedChange = onFieldShownChange,
        )
        if (filter.isActive) {
            ClearFilterChip(onClick = onClearClick)
        }
    }
}

private val FreshnessStatuses = setOf(
    StockStatus.NOT_FRESH,
    StockStatus.DUE_SOON,
    StockStatus.OVERDUE,
    StockStatus.EXPIRED,
)

/** The status with the number of its products, as menu choice. */
private val StockStatus.countLabel: StringResource
    get() = when (this) {
        StockStatus.NOT_FRESH -> Res.string.stock_overview_status_not_fresh
        StockStatus.DUE_SOON -> Res.string.stock_overview_status_due_soon
        StockStatus.OVERDUE -> Res.string.stock_overview_status_overdue
        StockStatus.EXPIRED -> Res.string.stock_overview_status_expired
        StockStatus.BELOW_MIN_STOCK -> Res.string.stock_overview_status_below_min_stock
        StockStatus.IN_STOCK -> Res.string.stock_overview_status_in_stock
        StockStatus.OPENED -> Res.string.stock_overview_status_opened
    }

/** The chosen status on the chip, without number. */
private val StockStatus.label: StringResource
    get() = when (this) {
        StockStatus.NOT_FRESH -> Res.string.stock_overview_not_fresh
        StockStatus.DUE_SOON -> Res.string.stock_entry_due_soon
        StockStatus.OVERDUE -> Res.string.stock_entry_overdue
        StockStatus.EXPIRED -> Res.string.stock_entry_expired
        StockStatus.BELOW_MIN_STOCK -> Res.string.stock_overview_below_min_stock
        StockStatus.IN_STOCK -> Res.string.stock_overview_in_stock
        StockStatus.OPENED -> Res.string.stock_entry_opened
    }

/** A field's name like Grocy's column header; [energyUnit] names the energy columns. */
@Composable
internal fun fieldLabel(field: StockOverviewField, energyUnit: String): String =
    field.label?.let { stringResource(it) } ?: when (field) {
        StockOverviewField.CALORIES_PER_UNIT ->
            "$energyUnit (${stringResource(Res.string.data_per_stock_unit)})"
        else -> energyUnit
    }

/** The header's text, null for the energy columns, which Grocy names with the energy unit. */
private val StockOverviewField.label: StringResource?
    get() = when (this) {
        StockOverviewField.NAME -> Res.string.products_sort_name
        StockOverviewField.PRODUCT_GROUP -> Res.string.data_product_group
        StockOverviewField.AMOUNT -> Res.string.data_amount
        StockOverviewField.VALUE -> Res.string.stock_overview_value
        StockOverviewField.NEXT_DUE_DATE -> Res.string.stock_overview_next_due_date
        StockOverviewField.CALORIES_PER_UNIT, StockOverviewField.CALORIES -> null
        StockOverviewField.LAST_PURCHASED -> Res.string.product_overview_last_purchased
        StockOverviewField.LAST_PRICE -> Res.string.product_overview_last_price
        StockOverviewField.MIN_STOCK_AMOUNT -> Res.string.products_sort_min_stock_amount
        StockOverviewField.DESCRIPTION -> Res.string.stock_overview_product_description
        StockOverviewField.PARENT_PRODUCT -> Res.string.product_parent
        StockOverviewField.DEFAULT_LOCATION -> Res.string.stock_overview_default_location
        StockOverviewField.PICTURE -> Res.string.stock_overview_product_picture
        StockOverviewField.AVERAGE_PRICE -> Res.string.product_overview_average_price
        StockOverviewField.DEFAULT_STORE -> Res.string.products_sort_store
    }

private val StockOverviewGrouping.label: StringResource
    get() = when (this) {
        StockOverviewGrouping.NONE -> Res.string.data_none
        StockOverviewGrouping.PRODUCT_GROUP -> Res.string.data_product_group
        StockOverviewGrouping.NEXT_DUE_DATE -> Res.string.stock_overview_next_due_date
        StockOverviewGrouping.LAST_PURCHASED -> Res.string.product_overview_last_purchased
        StockOverviewGrouping.MIN_STOCK_AMOUNT -> Res.string.products_sort_min_stock_amount
        StockOverviewGrouping.PARENT_PRODUCT -> Res.string.product_parent
        StockOverviewGrouping.DEFAULT_LOCATION -> Res.string.stock_overview_default_location
        StockOverviewGrouping.DEFAULT_STORE -> Res.string.products_sort_store
    }
