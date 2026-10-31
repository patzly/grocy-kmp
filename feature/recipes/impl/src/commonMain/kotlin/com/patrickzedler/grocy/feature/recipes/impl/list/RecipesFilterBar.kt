package com.patrickzedler.grocy.feature.recipes.impl.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.forms.ClearFilterChip
import com.patrickzedler.grocy.core.forms.DropdownFilterChip
import com.patrickzedler.grocy.core.forms.FilterChipRow
import com.patrickzedler.grocy.core.forms.MultiSelectChip
import com.patrickzedler.grocy.core.forms.SortChip
import com.patrickzedler.grocy.core.model.recipes.RecipeField
import com.patrickzedler.grocy.core.model.recipes.RecipeGrouping
import com.patrickzedler.grocy.core.model.recipes.RecipeStatus
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_all
import com.patrickzedler.grocy.core.resources.data_none
import com.patrickzedler.grocy.core.resources.products_filter_status
import com.patrickzedler.grocy.core.resources.products_sort_name
import com.patrickzedler.grocy.core.resources.recipes_desired_servings
import com.patrickzedler.grocy.core.resources.recipes_due_score
import com.patrickzedler.grocy.core.resources.recipes_requirements_fulfilled
import com.patrickzedler.grocy.core.resources.recipes_status_enough
import com.patrickzedler.grocy.core.resources.recipes_status_enough_count
import com.patrickzedler.grocy.core.resources.recipes_status_not_enough
import com.patrickzedler.grocy.core.resources.recipes_status_not_enough_count
import com.patrickzedler.grocy.core.resources.recipes_status_on_list
import com.patrickzedler.grocy.core.resources.recipes_status_on_list_count
import com.patrickzedler.grocy.core.resources.stock_overview_fields
import com.patrickzedler.grocy.core.resources.stock_overview_group_by
import com.patrickzedler.grocy.core.resources.stock_overview_grouped_by
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The filter and table options of Grocy's recipes as chips: its status filter (with the stock
 * feature, the number of recipes of each in its menu), sort (the name and the shown fields),
 * grouping (every groupable column, shown or not) and the shown fields. "Clear filter" while
 * a filter is set.
 */
@Composable
internal fun RecipesFilterBar(
    state: RecipesUiState,
    onStatusChange: (RecipeStatus?) -> Unit,
    onSortChange: (RecipeField, Boolean) -> Unit,
    onGroupingChange: (RecipeGrouping) -> Unit,
    onFieldShownChange: (RecipeField, Boolean) -> Unit,
    onClearClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    val all = stringResource(Res.string.data_all)

    FilterChipRow(contentPadding = contentPadding) {
        if (state.config.stock) {
            DropdownFilterChip(
                label = stringResource(Res.string.products_filter_status),
                optionGroups = listOf(listOf(null), RecipeStatus.entries),
                selected = state.filter.status,
                optionLabel = { status ->
                    status?.let {
                        stringResource(it.countLabel, state.statusCounts[it] ?: 0)
                    } ?: all
                },
                selectedLabel = { status -> status?.let { stringResource(it.label) } ?: all },
                onSelect = onStatusChange,
            )
        }
        SortChip(
            fields = listOf(RecipeField.NAME) + state.fields,
            field = state.sort.field,
            ascending = state.sort.ascending,
            isDefault = state.isDefaultSort,
            fieldLabel = { stringResource(it.label) },
            onSortChange = onSortChange,
        )
        DropdownFilterChip(
            label = stringResource(Res.string.stock_overview_group_by),
            optionGroups = listOf(state.availableGroupings),
            selected = state.grouping,
            isDefault = { it == RecipeGrouping.NONE },
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
        if (state.filter.isActive) {
            ClearFilterChip(onClick = onClearClick)
        }
    }
}

/** The status as Grocy's filter and column name it. */
internal val RecipeStatus.label: StringResource
    get() = when (this) {
        RecipeStatus.ENOUGH_IN_STOCK -> Res.string.recipes_status_enough
        RecipeStatus.ON_SHOPPING_LIST -> Res.string.recipes_status_on_list
        RecipeStatus.NOT_ENOUGH_IN_STOCK -> Res.string.recipes_status_not_enough
    }

/** The status with the number of its recipes, as menu choice. */
private val RecipeStatus.countLabel: StringResource
    get() = when (this) {
        RecipeStatus.ENOUGH_IN_STOCK -> Res.string.recipes_status_enough_count
        RecipeStatus.ON_SHOPPING_LIST -> Res.string.recipes_status_on_list_count
        RecipeStatus.NOT_ENOUGH_IN_STOCK -> Res.string.recipes_status_not_enough_count
    }

/** A column's name like Grocy's header. */
internal val RecipeField.label: StringResource
    get() = when (this) {
        RecipeField.NAME -> Res.string.products_sort_name
        RecipeField.DESIRED_SERVINGS -> Res.string.recipes_desired_servings
        RecipeField.DUE_SCORE -> Res.string.recipes_due_score
        RecipeField.REQUIREMENTS_FULFILLED -> Res.string.recipes_requirements_fulfilled
    }

private val RecipeGrouping.label: StringResource
    get() = when (this) {
        RecipeGrouping.NONE -> Res.string.data_none
        RecipeGrouping.DESIRED_SERVINGS -> Res.string.recipes_desired_servings
        RecipeGrouping.DUE_SCORE -> Res.string.recipes_due_score
        RecipeGrouping.REQUIREMENTS_FULFILLED -> Res.string.recipes_requirements_fulfilled
    }
