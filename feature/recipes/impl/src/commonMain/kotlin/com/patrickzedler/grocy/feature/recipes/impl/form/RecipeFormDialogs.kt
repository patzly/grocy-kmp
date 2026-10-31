package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.forms.saveMessage
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.booking_product_required
import com.patrickzedler.grocy.core.resources.booking_unit_required
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.meal_plan_servings
import com.patrickzedler.grocy.core.resources.recipe_ingredient_add
import com.patrickzedler.grocy.core.resources.recipe_ingredient_edit
import com.patrickzedler.grocy.core.resources.recipe_ingredient_group
import com.patrickzedler.grocy.core.resources.recipe_ingredient_group_hint
import com.patrickzedler.grocy.core.resources.recipe_ingredient_not_check_stock_fulfillment
import com.patrickzedler.grocy.core.resources.recipe_ingredient_of
import com.patrickzedler.grocy.core.resources.recipe_ingredient_only_check_single_unit
import com.patrickzedler.grocy.core.resources.recipe_ingredient_only_check_single_unit_hint
import com.patrickzedler.grocy.core.resources.recipe_ingredient_price_factor
import com.patrickzedler.grocy.core.resources.recipe_ingredient_price_factor_hint
import com.patrickzedler.grocy.core.resources.recipe_ingredient_variable_amount
import com.patrickzedler.grocy.core.resources.recipe_ingredient_variable_amount_hint
import com.patrickzedler.grocy.core.resources.recipe_nesting_add
import com.patrickzedler.grocy.core.resources.recipe_nesting_edit
import com.patrickzedler.grocy.core.resources.recipe_nesting_recipe
import com.patrickzedler.grocy.core.resources.recipe_nesting_recipe_required
import com.patrickzedler.grocy.core.resources.shopping_list_settings_round_up
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's ingredient form of a recipe as dialog, every field in its order; the stock options
 * only with the stock feature, the price factor only with price tracking.
 */
@Composable
internal fun IngredientDialog(
    state: IngredientDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val errors = state.errors
    val units = state.units

    FormDialog(
        title = stringResource(
            if (state.isNew) {
                Res.string.recipe_ingredient_add
            } else {
                Res.string.recipe_ingredient_edit
            },
        ),
        subtitle = stringResource(Res.string.recipe_ingredient_of, state.recipeName),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.data_product),
                        options = state.products.map { DropdownOption(it.id, it.name) },
                        selectedId = state.productId,
                        onSelectedIdChange = state::selectProduct,
                        onUnmatchedChange = { state.isProductUnmatched = it },
                        error = fieldErrorText(
                            errors[IngredientField.PRODUCT],
                            Res.string.booking_product_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                if (state.showsStockOptions) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(
                                Res.string.recipe_ingredient_only_check_single_unit,
                            ),
                            supporting = stringResource(
                                Res.string.recipe_ingredient_only_check_single_unit_hint,
                            ),
                            checked = state.onlyCheckSingleUnitInStock,
                            onCheckedChange = state::setOnlyCheckSingleUnit,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
            )
        }
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = state.amount,
                        label = stringResource(Res.string.data_amount),
                        error = fieldErrorText(errors[IngredientField.AMOUNT]),
                        keyboardType = KeyboardType.Decimal,
                        stepper = NumberStepper(min = 0.0),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.data_quantity_unit),
                        options = units.map { DropdownOption(it.unit.id, it.unit.name) },
                        selectedId = state.quId,
                        onSelectedIdChange = { state.quId = it },
                        onUnmatchedChange = { state.isUnitUnmatched = it },
                        error = fieldErrorText(
                            errors[IngredientField.UNIT],
                            Res.string.booking_unit_required,
                        ),
                        // Grocy's picker is fixed while the product has only one unit
                        enabled = units.size > 1,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = state.variableAmount,
                        label = stringResource(Res.string.recipe_ingredient_variable_amount),
                        hint = stringResource(Res.string.recipe_ingredient_variable_amount_hint),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    SegmentedSwitchItem(
                        headline = stringResource(Res.string.shopping_list_settings_round_up),
                        checked = state.roundUp,
                        onCheckedChange = { state.roundUp = it },
                        index = index,
                        count = count,
                    )
                },
                if (state.showsStockOptions) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(
                                Res.string.recipe_ingredient_not_check_stock_fulfillment,
                            ),
                            checked = state.notCheckStockFulfillment,
                            onCheckedChange = { state.notCheckStockFulfillment = it },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
            )
        }
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = state.group,
                        label = stringResource(Res.string.recipe_ingredient_group),
                        hint = stringResource(Res.string.recipe_ingredient_group_hint),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = state.note,
                        label = stringResource(Res.string.data_note),
                        multiLine = true,
                        index = index,
                        count = count,
                    )
                },
                if (state.showsPriceFactor) {
                    { index, count ->
                        TextItem(
                            state = state.priceFactor,
                            label = stringResource(Res.string.recipe_ingredient_price_factor),
                            hint = stringResource(Res.string.recipe_ingredient_price_factor_hint),
                            error = fieldErrorText(errors[IngredientField.PRICE_FACTOR]),
                            keyboardType = KeyboardType.Decimal,
                            stepper = NumberStepper.Positive,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

/** Grocy's included recipe form as dialog: the recipe and its servings. */
@Composable
internal fun NestingDialog(
    state: NestingDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val errors = state.errors

    FormDialog(
        title = stringResource(
            if (state.isNew) Res.string.recipe_nesting_add else Res.string.recipe_nesting_edit,
        ),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.recipe_nesting_recipe),
                        options = state.recipes.map { DropdownOption(it.id, it.name) },
                        selectedId = state.recipeId,
                        onSelectedIdChange = { state.recipeId = it },
                        onUnmatchedChange = { state.isRecipeUnmatched = it },
                        error = fieldErrorText(
                            errors[ObjectField.NAME],
                            Res.string.recipe_nesting_recipe_required,
                        ),
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    TextItem(
                        state = state.servings,
                        label = stringResource(Res.string.meal_plan_servings),
                        error = fieldErrorText(errors[ObjectField.AMOUNT]),
                        keyboardType = KeyboardType.Decimal,
                        stepper = NumberStepper.Positive,
                        index = index,
                        count = count,
                    )
                },
            )
        }
    }
}
