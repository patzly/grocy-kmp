package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLocale
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.product_overview_add_to_shopping_list
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.shopping_list_added
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldItem
import com.patrickzedler.grocy.feature.masterdata.impl.related.FormDialog
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
    val language = LocalLocale.current.toLanguageTag()
    LaunchedEffect(viewModel) {
        viewModel.added.collect { added ->
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
    FormDialog(
        title = stringResource(Res.string.product_overview_add_to_shopping_list),
        subtitle = state.productName,
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            ShoppingListItemFields(inputs = state.inputs, errors = state.errors)
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
