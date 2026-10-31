package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.patrickzedler.grocy.core.forms.ClearFilterChip
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.ShowDisabledChip
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.products_filter_in_stock
import com.patrickzedler.grocy.core.resources.products_filter_out_of_stock
import com.patrickzedler.grocy.core.resources.products_filter_product_group
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_location
import com.patrickzedler.grocy.core.resources.products_sort_min_stock_amount
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.products_sort_product_group
import com.patrickzedler.grocy.core.resources.products_sort_qu_stock
import com.patrickzedler.grocy.core.resources.products_sort_store
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's filter row of the product list as chips: product group and stock status as dropdown
 * chips, "Show disabled" as toggle chip, the sort order, and "Clear filter" while anything is
 * filtered.
 */
@Composable
internal fun ProductFilterBar(
    filter: ProductFilter,
    sort: ProductSort,
    productGroups: List<ProductGroup>,
    showClear: Boolean,
    showLocationSort: Boolean,
    showStoreSort: Boolean,
    onFilterChange: ((ProductFilter) -> ProductFilter) -> Unit,
    onSortChange: (ProductSort) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val all = stringResource(Res.string.data_all)

    FilterChipRow(modifier = modifier, contentPadding = contentPadding) {
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_product_group),
            options = listOf<Int?>(null) + productGroups.map { it.id },
            selected = filter.productGroupId,
            optionLabel = { id -> productGroups.firstOrNull { it.id == id }?.name ?: all },
            onSelect = { id -> onFilterChange { it.copy(productGroupId = id) } },
        )
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_status),
            options = StockStatusFilter.entries,
            selected = filter.stockStatus,
            isDefault = { it == StockStatusFilter.ALL },
            optionLabel = { status ->
                when (status) {
                    StockStatusFilter.ALL -> all
                    StockStatusFilter.IN_STOCK ->
                        stringResource(Res.string.products_filter_in_stock)
                    StockStatusFilter.OUT_OF_STOCK ->
                        stringResource(Res.string.products_filter_out_of_stock)
                }
            },
            onSelect = { status -> onFilterChange { it.copy(stockStatus = status) } },
        )
        ShowDisabledChip(
            selected = filter.showDisabled,
            onSelectedChange = { show -> onFilterChange { it.copy(showDisabled = show) } },
        )
        SortChip(
            fields = ProductSortField.entries.filter {
                (it != ProductSortField.LOCATION || showLocationSort) &&
                    (it != ProductSortField.STORE || showStoreSort)
            },
            field = sort.field,
            ascending = sort.ascending,
            isDefault = sort == ProductSort(),
            fieldLabel = { stringResource(it.label) },
            onSortChange = { field, ascending -> onSortChange(ProductSort(field, ascending)) },
        )
        if (showClear) {
            ClearFilterChip(onClick = onClearClick)
        }
    }
}

private val ProductSortField.label: StringResource
    get() = when (this) {
        ProductSortField.NAME -> Res.string.products_sort_name
        ProductSortField.LOCATION -> Res.string.products_sort_location
        ProductSortField.MIN_STOCK_AMOUNT -> Res.string.products_sort_min_stock_amount
        ProductSortField.PRODUCT_GROUP -> Res.string.products_sort_product_group
        ProductSortField.QU_STOCK -> Res.string.products_sort_qu_stock
        ProductSortField.STORE -> Res.string.products_sort_store
    }
