package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListField
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListGrouping
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.data_product_group
import com.patrickzedler.grocy.core.resources.product_barcodes
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_store
import com.patrickzedler.grocy.core.resources.shopping_list_last_price_total
import com.patrickzedler.grocy.core.resources.shopping_list_last_price_unit
import com.patrickzedler.grocy.core.resources.shopping_list_list_with_count
import com.patrickzedler.grocy.core.resources.shopping_list_only_done
import com.patrickzedler.grocy.core.resources.shopping_list_only_undone
import com.patrickzedler.grocy.core.resources.shopping_list_status_done
import com.patrickzedler.grocy.core.resources.shopping_list_status_undone
import com.patrickzedler.grocy.core.resources.stock_overview_below_min_stock
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import com.patrickzedler.grocy.core.resources.stock_overview_status_below_min_stock
import com.patrickzedler.grocy.feature.masterdata.impl.objects.ClearFilterChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.DropdownFilterChip
import com.patrickzedler.grocy.feature.masterdata.impl.objects.FilterChipRow
import com.patrickzedler.grocy.feature.masterdata.impl.objects.MultiSelectChip
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The shopping list's chips: Grocy's list selector (with several lists, "Name (count)" like its
 * options), the status filter with the number of items of each, then its table options:
 * grouping and the shown fields. "Clear filter" while a status is chosen.
 */
@Composable
internal fun ShoppingListFilterBar(
    state: ShoppingListUiState,
    onListSelect: (Int) -> Unit,
    onStatusChange: (ShoppingListStatus?) -> Unit,
    onGroupingChange: (ShoppingListGrouping) -> Unit,
    onFieldShownChange: (ShoppingListField, Boolean) -> Unit,
    onClearClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)
    FilterChipRow(contentPadding = contentPadding) {
        val list = state.list
        if (state.config.shoppingListMultipleLists && list != null) {
            DropdownFilterChip(
                // Always the chosen list's name, there is no list to leave unchosen
                label = list.name,
                options = state.lists.map { it.id },
                selected = list.id,
                isDefault = { true },
                optionLabel = { id ->
                    val choice = state.lists.first { it.id == id }
                    stringResource(
                        Res.string.shopping_list_list_with_count,
                        choice.name,
                        choice.itemCount,
                    )
                },
                onSelect = onListSelect,
            )
        }
        DropdownFilterChip(
            label = stringResource(Res.string.products_filter_status),
            optionGroups = listOf(listOf<ShoppingListStatus?>(null), state.availableStatuses),
            selected = state.status,
            optionLabel = { status ->
                status?.let { stringResource(it.countLabel, state.statusCounts[it] ?: 0) } ?: all
            },
            selectedLabel = { status -> status?.let { stringResource(it.label) } ?: all },
            onSelect = onStatusChange,
        )
        DropdownFilterChip(
            label = stringResource(Res.string.stock_overview_group_by),
            optionGroups = listOf(state.availableGroupings),
            selected = state.grouping,
            isDefault = { it == ShoppingListGrouping.NONE },
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
            optionLabel = { stringResource(it.label) },
            onCheckedChange = onFieldShownChange,
        )
        if (state.status != null) {
            ClearFilterChip(onClick = onClearClick)
        }
    }
}

/** The status with the number of its items, as menu choice. */
private val ShoppingListStatus.countLabel: StringResource
    get() = when (this) {
        ShoppingListStatus.BELOW_MIN_STOCK -> Res.string.stock_overview_status_below_min_stock
        ShoppingListStatus.DONE -> Res.string.shopping_list_status_done
        ShoppingListStatus.UNDONE -> Res.string.shopping_list_status_undone
    }

/** The chosen status on the chip, Grocy's words. */
private val ShoppingListStatus.label: StringResource
    get() = when (this) {
        ShoppingListStatus.BELOW_MIN_STOCK -> Res.string.stock_overview_below_min_stock
        ShoppingListStatus.DONE -> Res.string.shopping_list_only_done
        ShoppingListStatus.UNDONE -> Res.string.shopping_list_only_undone
    }

/** A field's name like Grocy's column header. */
internal val ShoppingListField.label: StringResource
    get() = when (this) {
        ShoppingListField.AMOUNT -> Res.string.data_amount
        ShoppingListField.PRODUCT_GROUP -> Res.string.data_product_group
        ShoppingListField.LAST_PRICE_UNIT -> Res.string.shopping_list_last_price_unit
        ShoppingListField.LAST_PRICE_TOTAL -> Res.string.shopping_list_last_price_total
        ShoppingListField.DEFAULT_STORE -> Res.string.products_sort_store
        ShoppingListField.BARCODES -> Res.string.product_barcodes
        ShoppingListField.USERFIELDS -> Res.string.product_userfields
    }

private val ShoppingListGrouping.label: StringResource
    get() = when (this) {
        ShoppingListGrouping.NONE -> Res.string.data_none
        ShoppingListGrouping.PRODUCT_GROUP -> Res.string.data_product_group
        ShoppingListGrouping.DEFAULT_STORE -> Res.string.products_sort_store
    }
