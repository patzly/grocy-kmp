package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.forms.DropdownItem
import com.patrickzedler.grocy.core.forms.FieldError
import com.patrickzedler.grocy.core.forms.FormFrame
import com.patrickzedler.grocy.core.forms.ObjectField
import com.patrickzedler.grocy.core.forms.ObjectFormScreen
import com.patrickzedler.grocy.core.forms.ScannerToggleButton
import com.patrickzedler.grocy.core.forms.fieldErrorText
import com.patrickzedler.grocy.core.productcard.ShoppingListItemFields
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_product
import com.patrickzedler.grocy.core.resources.product_parent_not_found
import com.patrickzedler.grocy.core.resources.shopping_list_item_create
import com.patrickzedler.grocy.core.resources.shopping_list_item_edit
import com.patrickzedler.grocy.core.resources.shopping_list_product_or_note_required
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's "Create shopping list item" / "Edit shopping list item" form: list, product (typed,
 * chosen or scanned inside its item), amount with its unit, note and the item's userfields.
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
            stringResource(Res.string.shopping_list_item_create)
        } else {
            stringResource(Res.string.shopping_list_item_edit)
        },
        viewModel = viewModel,
        frame = frame,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { form ->
        segmentedGroup(key = "item") {
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
    var showScanner by rememberSaveable { mutableStateOf(false) }
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
        below = if (showScanner) {
            {
                BarcodeScanner(
                    onBarcodeScanned = { barcode ->
                        val product = form.productForCode(barcode.value)
                        if (product == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.product_parent_not_found),
                                )
                            }
                        } else {
                            form.selectProduct(product.id)
                            showScanner = false
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
                isScannerOpen = showScanner,
                onClick = { showScanner = !showScanner },
            )
        },
    )
}
