package com.patrickzedler.grocy.feature.masterdata.impl.barcode

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.model.masterdata.nameFor
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.barcode_barcode
import com.patrickzedler.grocy.core.resources.barcode_create
import com.patrickzedler.grocy.core.resources.barcode_edit
import com.patrickzedler.grocy.core.resources.barcode_for_product
import com.patrickzedler.grocy.core.resources.barcode_required
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_quantity_unit
import com.patrickzedler.grocy.core.resources.data_store
import com.patrickzedler.grocy.core.resources.data_this_equals
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import com.patrickzedler.grocy.feature.masterdata.impl.product.DropdownItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.FieldError
import com.patrickzedler.grocy.feature.masterdata.impl.product.GroupItems
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import com.patrickzedler.grocy.feature.masterdata.impl.related.FormDialog
import com.patrickzedler.grocy.feature.masterdata.impl.related.ScannerToggleButton
import com.patrickzedler.grocy.feature.masterdata.impl.related.destinationAmount
import com.patrickzedler.grocy.feature.masterdata.impl.related.saveMessage
import org.jetbrains.compose.resources.stringResource

/** Grocy's barcode form of a product as dialog; the barcode can be scanned. */
@Composable
internal fun BarcodeDialog(
    state: BarcodeDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    var showScanner by rememberSaveable { mutableStateOf(false) }
    val errors = state.errors

    FormDialog(
        title = stringResource(
            if (state.isNew) Res.string.barcode_create else Res.string.barcode_edit,
        ),
        subtitle = stringResource(Res.string.barcode_for_product, state.productName),
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = state.barcode,
                        label = stringResource(Res.string.barcode_barcode),
                        error = fieldErrorText(
                            errors[BarcodeField.BARCODE],
                            Res.string.barcode_required,
                        ),
                        index = index,
                        count = count,
                        trailingAction = {
                            ScannerToggleButton(
                                isScannerOpen = showScanner,
                                onClick = { showScanner = !showScanner },
                            )
                        },
                        below = if (showScanner) {
                            {
                                BarcodeScanner(
                                    onBarcodeScanned = { barcode ->
                                        state.barcode.setTextAndPlaceCursorAtEnd(barcode.value)
                                        showScanner = false
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                )
                            }
                        } else {
                            null
                        },
                    )
                },
                { index, count -> AmountItem(state, errors, index, count) },
                { index, count ->
                    DropdownItem(
                        label = stringResource(Res.string.data_quantity_unit),
                        options = state.units.map { DropdownOption(it.unit.id, it.unit.name) },
                        selectedId = state.quId,
                        onSelectedIdChange = { state.quId = it },
                        onUnmatchedChange = { state.isUnitUnmatched = it },
                        optional = true,
                        // Grocy's picker is fixed while the product has only one unit
                        enabled = state.units.size > 1,
                        index = index,
                        count = count,
                    )
                },
                if (state.showsStore) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.data_store),
                            options = state.stores.map { DropdownOption(it.id, it.name) },
                            selectedId = state.storeId,
                            onSelectedIdChange = { state.storeId = it },
                            onUnmatchedChange = { state.isStoreUnmatched = it },
                            optional = true,
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                { index, count ->
                    TextItem(
                        state = state.note,
                        label = stringResource(Res.string.data_note),
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

/** The amount with Grocy's note what it equals in the product's purchase unit. */
@Composable
private fun AmountItem(
    state: BarcodeDialogState,
    errors: Map<BarcodeField, FieldError>,
    index: Int,
    count: Int,
) {
    val unit = state.units.firstOrNull { it.unit.id == state.quId }
    val destination = state.purchaseUnit
    val converted = destinationAmount(state.amount.text.toString(), unit)
    TextItem(
        state = state.amount,
        label = stringResource(Res.string.data_amount),
        error = fieldErrorText(errors[BarcodeField.AMOUNT]),
        hint = if (converted != null && destination != null) {
            stringResource(
                Res.string.data_this_equals,
                formatDecimal(converted, state.decimals, platformDecimalSeparator()),
                destination.nameFor(converted, Locale.current.toLanguageTag()),
            )
        } else {
            null
        },
        keyboardType = KeyboardType.Decimal,
        stepper = NumberStepper.Positive,
        index = index,
        count = count,
    )
}
