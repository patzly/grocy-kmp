package com.patrickzedler.grocy.feature.recipes.impl.form

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FocusRequestEffect
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.GroupItems
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ProductFormRequest
import com.patrickzedler.grocy.core.forms.ScannerToggleButton
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.UnknownProductDialog
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.forms.saveMessage
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.booking_add_barcode_hint
import com.patrickzedler.grocy.core.resources.booking_not_in_stock
import com.patrickzedler.grocy.core.resources.booking_product_required
import com.patrickzedler.grocy.core.resources.product_picker_workflow_hint
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
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's ingredient form of a recipe as dialog, every field in its order; the stock options
 * only with the stock feature, the price factor only with price tracking.
 */
@Composable
internal fun IngredientDialog(
    state: IngredientDialogState,
    onOpenProduct: (ProductFormRequest) -> Unit,
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
        val scanner = state.scanner
        UnknownProductDialog(
            flow = state.unknownProduct,
            products = state.products,
            onOpenProduct = onOpenProduct,
            onBarcodeAssigned = state::onBarcodeAssigned,
            onScanAgain = state::scanAgain,
            onDismissRequest = state::dismissUnknownProduct,
        )
        val productFocus = remember { FocusRequester() }
        FocusRequestEffect(
            scanner.focusTarget == FormFocusTarget.CODE_FIELD,
            productFocus,
            scanner::onFocusHandled,
        )
        val amountFocus = remember { FocusRequester() }
        FocusRequestEffect(
            scanner.focusTarget == FormFocusTarget.NEXT_FIELD,
            amountFocus,
            scanner::onFocusHandled,
        )
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.data_product),
                        focusRequester = productFocus,
                        onFocusChange = scanner::onFieldFocusChange,
                        options = state.products.map { DropdownOption(it.id, it.name) },
                        selectedId = state.productId,
                        onSelectedIdChange = state::selectProduct,
                        onUnmatchedChange = { state.isProductUnmatched = it },
                        hint = state.unknownProduct.pendingBarcode
                            ?.let { stringResource(Res.string.booking_add_barcode_hint, it) }
                            ?: stringResource(Res.string.product_picker_workflow_hint),
                        error = if (state.unknownProduct.isNotOffered) {
                            stringResource(Res.string.booking_not_in_stock)
                        } else {
                            fieldErrorText(
                                errors[IngredientField.PRODUCT],
                                Res.string.booking_product_required,
                            )
                        },
                        onSubmitText = state::onTextSubmitted,
                        submitsTextOnLeave = true,
                        textResetKey = state.unknownProduct.fieldResetKey,
                        below = if (scanner.isOpen) {
                            {
                                BarcodeScanner(
                                    onBarcodeScanned = { state.onScanned(it.value) },
                                    shape = MaterialTheme.shapes.small,
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                )
                            }
                        } else {
                            null
                        },
                        trailingAction = {
                            ScannerToggleButton(
                                isScannerOpen = scanner.isOpen,
                                onClick = scanner::toggle,
                            )
                        },
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
                        focusRequester = amountFocus,
                        autoFocus = !state.isNew,
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
                            isLastField = true,
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
                        autoFocus = state.isNew,
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
                        autoFocus = !state.isNew,
                        isLastField = true,
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
