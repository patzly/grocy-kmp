package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.FocusRequestEffect
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFocusTarget
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.ScannerToggleButton
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.productcard.ShoppingListItemFields
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.product_parent_not_found
import com.patrickzedler.grocy.core.resources.shopping_list_add_item
import com.patrickzedler.grocy.core.resources.shopping_list_item_edit
import com.patrickzedler.grocy.core.resources.shopping_list_product_or_note_required
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's shopping list item form, titled "Add item" / "Edit item" (Grocy's long titles do not
 * fit the app bar): list, product (typed, chosen or scanned inside its item), amount with its
 * unit, note and the item's userfields.
 */
@Composable
internal fun ShoppingListItemScreen(
    viewModel: ShoppingListItemViewModel,
    frame: FormFrame,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    ObjectFormScreen(
        title = if (viewModel.isNew) {
            stringResource(Res.string.shopping_list_add_item)
        } else {
            stringResource(Res.string.shopping_list_item_edit)
        },
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { form ->
        segmentedGroup(key = "item") {
            val amountFocus = remember { FocusRequester() }
            FocusRequestEffect(
                form.scanner.focusTarget == FormFocusTarget.NEXT_FIELD,
                amountFocus,
                form.scanner::onFocusHandled,
            )
            ShoppingListItemFields(
                inputs = form.inputs,
                errors = viewModel.errors,
                product = { index, count ->
                    ProductItem(
                        form = form,
                        error = viewModel.errors[ObjectField.PRODUCT],
                        snackbarHostState = snackbarHostState,
                        index = index,
                        count = count,
                    )
                },
                amountFocusRequester = amountFocus,
            )
        }
    }
}

/** Grocy's product picker, a barcode or grocycode scanned inside its item chooses too. */
@Composable
private fun ProductItem(
    form: ShoppingListItemFormState,
    error: FieldError?,
    snackbarHostState: SnackbarHostState,
    index: Int,
    count: Int,
) {
    val scope = rememberCoroutineScope()
    val scanner = form.scanner
    val focusRequester = remember { FocusRequester() }
    FocusRequestEffect(
        scanner.focusTarget == FormFocusTarget.CODE_FIELD,
        focusRequester,
        scanner::onFocusHandled,
    )
    DropdownItem(
        label = stringResource(Res.string.data_product),
        options = form.products.map { DropdownOption(it.id, it.name) },
        selectedId = form.productId,
        onSelectedIdChange = form::selectProduct,
        onUnmatchedChange = { form.isProductUnmatched = it },
        error = fieldErrorText(error, Res.string.shopping_list_product_or_note_required),
        // A note alone is an item too
        optional = true,
        index = index,
        count = count,
        focusRequester = focusRequester,
        onSubmitText = { text, _ -> form.onTextSubmitted(text) },
        onFocusChange = scanner::onFieldFocusChange,
        below = if (scanner.isOpen) {
            {
                BarcodeScanner(
                    onBarcodeScanned = { barcode ->
                        if (!form.onScanned(barcode.value)) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.product_parent_not_found),
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
    )
}
