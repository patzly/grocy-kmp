package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.data_this_equals
import com.patrickzedler.grocy.core.resources.product_overview_add_to_shopping_list
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.shopping_list_added
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.shopping_list_required
import com.patrickzedler.grocy.feature.masterdata.impl.product.DropdownItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.GroupItems
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import com.patrickzedler.grocy.feature.masterdata.impl.related.FormDialog
import com.patrickzedler.grocy.feature.masterdata.impl.related.destinationAmount
import com.patrickzedler.grocy.feature.masterdata.impl.related.saveMessage
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's shopping list item form as it opens from a product ("Add to shopping list"), and its
 * message once the product is on the list in [snackbarHostState].
 */
@Composable
internal fun AddToShoppingListHost(
    viewModel: AddToShoppingListViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.added.collect { added ->
            val language = Locale.current.toLanguageTag()
            val amount = parseDecimal(added.amount) ?: 0.0
            snackbarHostState.showSnackbar(
                getString(
                    Res.string.shopping_list_added,
                    listOfNotNull(added.amount, added.unit?.unit?.nameFor(amount, language))
                        .joinToString(" "),
                    added.productName,
                    added.listName,
                ),
            )
        }
    }
    viewModel.dialog?.let { state ->
        AddToShoppingListDialog(
            state = state,
            onSave = viewModel::save,
            onDismissRequest = viewModel::dismiss,
        )
    }
}

@Composable
private fun AddToShoppingListDialog(
    state: AddToShoppingListState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val errors = state.errors
    val unit = state.units.firstOrNull { it.unit.id == state.quId }
    val stockUnit = state.stockUnit
    val converted = destinationAmount(state.amount.text.toString(), unit)

    FormDialog(
        title = stringResource(Res.string.product_overview_add_to_shopping_list),
        subtitle = state.productName,
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                if (state.lists.isNotEmpty()) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.shopping_list_list),
                            options = state.lists.map { DropdownOption(it.id, it.name) },
                            selectedId = state.listId,
                            onSelectedIdChange = { state.listId = it },
                            onUnmatchedChange = { state.isListUnmatched = it },
                            error = fieldErrorText(
                                errors[ShoppingListItemField.LIST],
                                Res.string.shopping_list_required,
                            ),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                { index, count ->
                    TextItem(
                        state = state.amount,
                        label = stringResource(Res.string.data_amount),
                        error = fieldErrorText(errors[ShoppingListItemField.AMOUNT]),
                        hint = if (converted != null && stockUnit != null) {
                            val separator = platformDecimalSeparator()
                            stringResource(
                                Res.string.data_this_equals,
                                formatDecimal(converted, state.decimals, separator),
                                stockUnit.nameFor(converted, Locale.current.toLanguageTag()),
                            )
                        } else {
                            null
                        },
                        keyboardType = KeyboardType.Decimal,
                        stepper = NumberStepper.Positive,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.data_quantity_unit),
                        options = state.units.map { DropdownOption(it.unit.id, it.unit.name) },
                        selectedId = state.quId,
                        onSelectedIdChange = { state.quId = it },
                        onUnmatchedChange = { state.isUnitUnmatched = it },
                        optional = true,
                        enabled = state.units.size > 1,
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
            )
        }
        if (state.userfields.isNotEmpty()) {
            SegmentedGroup(title = stringResource(Res.string.product_userfields)) {
                state.userfields.forEachIndexed { index, input ->
                    UserfieldItem(
                        input = input,
                        error = state.userfieldErrors[input.userfield.name],
                        index = index,
                        count = state.userfields.size,
                    )
                }
            }
        }
    }
}
