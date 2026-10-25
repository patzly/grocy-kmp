package com.patrickzedler.grocy.feature.masterdata.impl.stock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.KeyboardType
import com.patrickzedler.grocy.core.design.component.DropdownOption
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_amount
import com.patrickzedler.grocy.core.resources.data_due_date
import com.patrickzedler.grocy.core.resources.data_location
import com.patrickzedler.grocy.core.resources.data_never_overdue
import com.patrickzedler.grocy.core.resources.data_note
import com.patrickzedler.grocy.core.resources.data_per_stock_unit
import com.patrickzedler.grocy.core.resources.data_price
import com.patrickzedler.grocy.core.resources.data_purchased_date
import com.patrickzedler.grocy.core.resources.data_store
import com.patrickzedler.grocy.core.resources.product_location_required
import com.patrickzedler.grocy.core.resources.product_userfields
import com.patrickzedler.grocy.core.resources.stock_entry_due_required
import com.patrickzedler.grocy.core.resources.stock_entry_edit
import com.patrickzedler.grocy.core.resources.stock_entry_opened
import com.patrickzedler.grocy.core.resources.stock_entry_purchased_required
import com.patrickzedler.grocy.core.resources.stock_entry_reprint_label
import com.patrickzedler.grocy.feature.masterdata.impl.product.DatePickerButton
import com.patrickzedler.grocy.feature.masterdata.impl.product.DropdownItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.GroupItems
import com.patrickzedler.grocy.feature.masterdata.impl.product.TextItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.UserfieldItem
import com.patrickzedler.grocy.feature.masterdata.impl.product.fieldErrorText
import com.patrickzedler.grocy.feature.masterdata.impl.related.FormDialog
import com.patrickzedler.grocy.feature.masterdata.impl.related.saveMessage
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.stringResource

/** Grocy's "Edit stock entry" form as dialog, fields of turned off features hidden. */
@Composable
internal fun StockEntryDialog(
    state: StockEntryDialogState,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val config = state.config
    val errors = state.errors
    // Like Grocy, a changed due date gets a new label
    LaunchedEffect(state) {
        snapshotFlow { state.dueDate.text.toString() to state.neverOverdue }
            .drop(1)
            .collect { if (state.canPrintLabel) state.reprintLabel = true }
    }

    FormDialog(
        title = stringResource(Res.string.stock_entry_edit),
        subtitle = state.productName,
        isSaving = state.isSaving,
        error = state.error?.saveMessage(),
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            GroupItems(
                { index, count ->
                    TextItem(
                        state = state.amount,
                        label = stringResource(Res.string.data_amount),
                        error = fieldErrorText(errors[StockEntryField.AMOUNT]),
                        suffix = state.stockUnitName,
                        keyboardType = KeyboardType.Decimal,
                        stepper = NumberStepper.Positive,
                        index = index,
                        count = count,
                    )
                },
                { index, count ->
                    DateItem(
                        state = state,
                        field = StockEntryField.PURCHASED_DATE,
                        label = stringResource(Res.string.data_purchased_date),
                        index = index,
                        count = count,
                    )
                },
                if (config.stockBestBeforeDateTracking) {
                    { index, count ->
                        DateItem(
                            state = state,
                            field = StockEntryField.DUE_DATE,
                            label = stringResource(Res.string.data_due_date),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (config.stockBestBeforeDateTracking) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(Res.string.data_never_overdue),
                            checked = state.neverOverdue,
                            onCheckedChange = { state.neverOverdue = it },
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
                if (config.stockPriceTracking) {
                    { index, count ->
                        TextItem(
                            state = state.price,
                            label = stringResource(Res.string.data_price),
                            hint = stringResource(Res.string.data_per_stock_unit),
                            error = fieldErrorText(errors[StockEntryField.PRICE]),
                            keyboardType = KeyboardType.Decimal,
                            stepper = NumberStepper(min = 0.0),
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (config.stockPriceTracking) {
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
                if (config.stockLocationTracking) {
                    { index, count ->
                        DropdownItem(
                            label = stringResource(Res.string.data_location),
                            options = state.locations.map { DropdownOption(it.id, it.name) },
                            selectedId = state.locationId,
                            onSelectedIdChange = { state.locationId = it },
                            onUnmatchedChange = { state.isLocationUnmatched = it },
                            error = fieldErrorText(
                                errors[StockEntryField.LOCATION],
                                Res.string.product_location_required,
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
                        state = state.note,
                        label = stringResource(Res.string.data_note),
                        index = index,
                        count = count,
                    )
                },
                if (config.stockProductOpenedTracking) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(Res.string.stock_entry_opened),
                            checked = state.open,
                            onCheckedChange = { state.open = it },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
                },
                if (state.canPrintLabel) {
                    { index, count ->
                        SegmentedSwitchItem(
                            headline = stringResource(Res.string.stock_entry_reprint_label),
                            checked = state.reprintLabel,
                            onCheckedChange = { state.reprintLabel = it },
                            index = index,
                            count = count,
                        )
                    }
                } else {
                    null
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

/** A date with the calendar button; the due date is off while it is never overdue. */
@Composable
private fun DateItem(
    state: StockEntryDialogState,
    field: StockEntryField,
    label: String,
    index: Int,
    count: Int,
) {
    val isDue = field == StockEntryField.DUE_DATE
    val text = if (isDue) state.dueDate else state.purchasedDate
    val enabled = !isDue || !state.neverOverdue
    TextItem(
        state = text,
        label = label,
        hint = "YYYY-MM-DD",
        error = fieldErrorText(
            state.errors[field],
            if (isDue) {
                Res.string.stock_entry_due_required
            } else {
                Res.string.stock_entry_purchased_required
            },
        ).takeIf { enabled },
        enabled = enabled,
        index = index,
        count = count,
        trailingAction = if (enabled) {
            { DatePickerButton(text, withTime = false) }
        } else {
            null
        },
    )
}
