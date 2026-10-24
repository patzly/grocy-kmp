package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_downward
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_upward
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_filter_list_off
import com.patrickzedler.grocy.core.resources.products_filter_clear
import com.patrickzedler.grocy.core.resources.products_filter_in_stock
import com.patrickzedler.grocy.core.resources.products_filter_out_of_stock
import com.patrickzedler.grocy.core.resources.products_filter_product_group
import com.patrickzedler.grocy.core.resources.products_filter_show_disabled
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_ascending
import com.patrickzedler.grocy.core.resources.products_sort_descending
import com.patrickzedler.grocy.core.resources.products_sort_location
import com.patrickzedler.grocy.core.resources.products_sort_min_stock_amount
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.products_sort_product_group
import com.patrickzedler.grocy.core.resources.products_sort_qu_stock
import com.patrickzedler.grocy.core.resources.products_sort_store
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's filter row as Material 3 chips, one per filter: product group and stock status as
 * dropdown chips, "Show disabled" as toggle chip, the sort order, and "Clear filter" while
 * anything is filtered. Scrolls sideways on narrow screens; [contentPadding] scrolls along.
 * The row lies on the screen's background, so the outlined chips get a container color of their
 * own and no elevation, not even on hover.
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
    val haptic = LocalHapticFeedback.current
    val all = stringResource(Res.string.data_all)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // Inside the scrolling, so chips scroll up to the screen's edge instead of a margin
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        FilterChip(
            selected = filter.showDisabled,
            modifier = Modifier.clickCursor(),
            onClick = {
                haptic.confirm()
                onFilterChange { it.copy(showDisabled = !it.showDisabled) }
            },
            label = { Text(stringResource(Res.string.products_filter_show_disabled)) },
            colors = filterChipColors(),
            elevation = filterChipElevation(),
            leadingIcon = if (filter.showDisabled) {
                { ChipIcon(Res.drawable.ic_rounded_check) }
            } else {
                null
            },
        )
        SortChipWithMenu(sort, showLocationSort, showStoreSort, onSortChange)
        if (showClear) {
            // An action, not an input to remove: assist chip with the reset icon at its start
            AssistChip(
                onClick = {
                    haptic.confirm()
                    onClearClick()
                },
                modifier = Modifier.clickCursor(),
                label = { Text(stringResource(Res.string.products_filter_clear)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                elevation = AssistChipDefaults.assistChipElevation(
                    elevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    draggedElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_filter_list_off),
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

/**
 * A filter chip that opens the choices of one filter. While a value other than the default is
 * chosen, the chip is selected and shows that value instead of the filter's name.
 */
@Composable
private fun <T> DropdownFilterChip(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    isDefault: (T) -> Boolean = { it == null },
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val active = !isDefault(selected)

    Box {
        FilterChip(
            selected = active,
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(if (active) optionLabel(selected) else label, maxLines = 1) },
            // It opens a menu, so it is announced as button with the filter's name
            modifier = Modifier
                .semantics {
                    role = Role.Button
                    if (active) stateDescription = label
                }
                .clickCursor(),
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
            colors = filterChipColors(),
            elevation = filterChipElevation(),
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                // Long lists of product groups scroll inside the menu
                modifier = Modifier
                    .heightIn(max = MenuMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEachIndexed { index, option ->
                    SelectableDropdownMenuItem(
                        selected = option == selected,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            expanded = false
                            onSelect(option)
                        },
                        text = { Text(optionLabel(option)) },
                        shapes = MenuDefaults.itemShape(index = index, count = options.size),
                        selectedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChipWithMenu(
    sort: ProductSort,
    showLocation: Boolean,
    showStore: Boolean,
    onSortChange: (ProductSort) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val fields = ProductSortField.entries.filter {
        (it != ProductSortField.LOCATION || showLocation) &&
            (it != ProductSortField.STORE || showStore)
    }
    val sortDescription = stringResource(
        if (sort.ascending) {
            Res.string.products_sort_ascending
        } else {
            Res.string.products_sort_descending
        },
    )
    val directionIcon = if (sort.ascending) {
        Res.drawable.ic_rounded_arrow_upward
    } else {
        Res.drawable.ic_rounded_arrow_downward
    }

    Box {
        FilterChip(
            selected = sort != ProductSort(),
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(stringResource(sort.field.label), maxLines = 1) },
            modifier = Modifier.semantics {
                role = Role.Button
                stateDescription = sortDescription
            }.clickCursor(),
            leadingIcon = { ChipIcon(directionIcon) },
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
            colors = filterChipColors(),
            elevation = filterChipElevation(),
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                fields.forEachIndexed { index, field ->
                    SelectableDropdownMenuItem(
                        selected = field == sort.field,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            onSortChange(sort.copy(field = field))
                            expanded = false
                        },
                        text = { Text(stringResource(field.label)) },
                        shapes = MenuDefaults.itemShape(index = index, count = fields.size),
                        selectedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
                listOf(true, false).forEachIndexed { index, ascending ->
                    SelectableDropdownMenuItem(
                        selected = sort.ascending == ascending,
                        modifier = Modifier.clickCursor(),
                        onClick = {
                            haptic.confirm()
                            onSortChange(sort.copy(ascending = ascending))
                            expanded = false
                        },
                        text = {
                            Text(
                                stringResource(
                                    if (ascending) {
                                        Res.string.products_sort_ascending
                                    } else {
                                        Res.string.products_sort_descending
                                    },
                                ),
                            )
                        },
                        shapes = MenuDefaults.itemShape(index = index, count = 2),
                        leadingIcon = {
                            MenuIcon(
                                if (ascending) {
                                    Res.drawable.ic_rounded_arrow_upward
                                } else {
                                    Res.drawable.ic_rounded_arrow_downward
                                },
                            )
                        },
                    )
                }
            }
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

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)

@Composable
private fun filterChipElevation() = FilterChipDefaults.filterChipElevation(
    elevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp,
)

@Composable
private fun ChipIcon(icon: DrawableResource) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Composable
private fun MenuIcon(icon: DrawableResource) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
    )
}

private val MenuMaxHeight = 360.dp
