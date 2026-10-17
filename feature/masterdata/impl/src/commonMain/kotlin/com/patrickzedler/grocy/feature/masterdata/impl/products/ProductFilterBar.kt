package com.patrickzedler.grocy.feature.masterdata.impl.products

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorPosition
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
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_downward
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_drop_down
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_right
import com.patrickzedler.grocy.core.resources.ic_rounded_arrow_upward
import com.patrickzedler.grocy.core.resources.ic_rounded_check
import com.patrickzedler.grocy.core.resources.ic_rounded_filter_list
import com.patrickzedler.grocy.core.resources.ic_rounded_filter_list_off
import com.patrickzedler.grocy.core.resources.products_filter
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
 * One chip for all filters (menu with sub menus like Grocy's filter row), one for the sort
 * order, and "Clear filter" while anything is filtered. Scrolls sideways on narrow screens.
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
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipWithMenu(filter, productGroups, onFilterChange)
        SortChipWithMenu(sort, showLocationSort, showStoreSort, onSortChange)
        if (showClear) {
            AssistChip(
                onClick = {
                    haptic.confirm()
                    onClearClick()
                },
                label = { Text(stringResource(Res.string.products_filter_clear)) },
                leadingIcon = { ChipIcon(Res.drawable.ic_rounded_filter_list_off) },
            )
        }
    }
}

@Composable
private fun FilterChipWithMenu(
    filter: ProductFilter,
    productGroups: List<ProductGroup>,
    onFilterChange: ((ProductFilter) -> ProductFilter) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val all = stringResource(Res.string.data_all)
    val groupName = productGroups.firstOrNull { it.id == filter.productGroupId }?.name
    val statusName = when (filter.stockStatus) {
        StockStatusFilter.ALL -> null
        StockStatusFilter.IN_STOCK -> stringResource(Res.string.products_filter_in_stock)
        StockStatusFilter.OUT_OF_STOCK -> stringResource(Res.string.products_filter_out_of_stock)
    }
    // The chip names what is filtered, so the list explains itself
    val summary = listOfNotNull(
        groupName,
        statusName,
        stringResource(Res.string.products_filter_show_disabled).takeIf { filter.showDisabled },
    ).joinToString(" · ").ifEmpty { stringResource(Res.string.products_filter) }

    Box {
        FilterChip(
            selected = filter.isActive,
            onClick = {
                haptic.confirm()
                expanded = true
            },
            label = { Text(summary, maxLines = 1) },
            // It opens a menu, so it is announced as one and not as a checkbox
            modifier = Modifier.semantics { role = Role.DropdownList },
            shapes = FilterChipDefaults.shapes(),
            leadingIcon = { ChipIcon(Res.drawable.ic_rounded_filter_list) },
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
        )
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                SubMenuItem(
                    text = stringResource(Res.string.products_filter_product_group),
                    value = groupName ?: all,
                    index = 0,
                    count = 2,
                    options = listOf(null to all) + productGroups.map { it.id to it.name },
                    selected = filter.productGroupId,
                    onSelect = { id ->
                        onFilterChange { it.copy(productGroupId = id) }
                        expanded = false
                    },
                )
                SubMenuItem(
                    text = stringResource(Res.string.products_filter_status),
                    value = statusName ?: all,
                    index = 1,
                    count = 2,
                    options = listOf(
                        StockStatusFilter.ALL to all,
                        StockStatusFilter.IN_STOCK to
                            stringResource(Res.string.products_filter_in_stock),
                        StockStatusFilter.OUT_OF_STOCK to
                            stringResource(Res.string.products_filter_out_of_stock),
                    ),
                    selected = filter.stockStatus,
                    onSelect = { status ->
                        onFilterChange { it.copy(stockStatus = status ?: StockStatusFilter.ALL) }
                        expanded = false
                    },
                )
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
                CheckableDropdownMenuItem(
                    checked = filter.showDisabled,
                    onCheckedChange = { checked ->
                        haptic.confirm()
                        onFilterChange { it.copy(showDisabled = checked) }
                    },
                    text = { Text(stringResource(Res.string.products_filter_show_disabled)) },
                    shapes = MenuDefaults.itemShape(index = 0, count = 1),
                    checkedLeadingIcon = { MenuIcon(Res.drawable.ic_rounded_check) },
                )
            }
        }
    }
}

/** A menu item that opens [options] in a menu beside it, like a cascading desktop menu. */
@Composable
private fun <T> SubMenuItem(
    text: String,
    value: String,
    index: Int,
    count: Int,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        DropdownMenuItem(
            onClick = {
                haptic.confirm()
                expanded = !expanded
            },
            text = { Text(text) },
            shape = MenuDefaults.itemShape(index = index, count = count).shape,
            supportingText = { Text(value, maxLines = 1) },
            trailingContent = { MenuIcon(Res.drawable.ic_rounded_arrow_right) },
        )
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.End,
            ),
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                // Long lists of product groups scroll inside the menu
                modifier = Modifier
                    .heightIn(max = SubMenuMaxHeight)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEachIndexed { optionIndex, (option, label) ->
                    SelectableDropdownMenuItem(
                        selected = option == selected,
                        onClick = {
                            haptic.confirm()
                            expanded = false
                            onSelect(option)
                        },
                        text = { Text(label) },
                        shapes = MenuDefaults.itemShape(index = optionIndex, count = options.size),
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
        (it != ProductSortField.LOCATION || showLocation) && (it != ProductSortField.STORE || showStore)
    }
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
            modifier = Modifier.semantics { role = Role.DropdownList },
            shapes = FilterChipDefaults.shapes(),
            leadingIcon = { ChipIcon(directionIcon) },
            trailingIcon = { ChipIcon(Res.drawable.ic_rounded_arrow_drop_down) },
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                fields.forEachIndexed { index, field ->
                    SelectableDropdownMenuItem(
                        selected = field == sort.field,
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

private val SubMenuMaxHeight = 360.dp
