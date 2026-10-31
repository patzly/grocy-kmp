@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedGroup
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.forms.ChangeError
import com.patrickzedler.grocy.core.forms.DeleteDialog
import com.patrickzedler.grocy.core.forms.FormDialog
import com.patrickzedler.grocy.core.forms.TextItem
import com.patrickzedler.grocy.core.forms.saveMessage
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.shopping_list_adding_item
import com.patrickzedler.grocy.core.resources.shopping_list_clear
import com.patrickzedler.grocy.core.resources.shopping_list_clear_question
import com.patrickzedler.grocy.core.resources.shopping_list_clear_question_single
import com.patrickzedler.grocy.core.resources.shopping_list_delete_question
import com.patrickzedler.grocy.core.resources.shopping_list_item_not_booked
import com.patrickzedler.grocy.core.resources.shopping_list_notes
import com.patrickzedler.grocy.core.resources.shopping_list_print_failed
import com.patrickzedler.grocy.core.resources.shopping_list_print_options
import com.patrickzedler.grocy.core.resources.shopping_list_print_show_header
import com.patrickzedler.grocy.core.resources.shopping_list_skip
import com.patrickzedler.grocy.core.resources.shopping_list_thermal_printer
import org.jetbrains.compose.resources.stringResource

/** The questions of [viewModel] and the forms it opens over the list. */
@Composable
internal fun ShoppingListDialogs(viewModel: ShoppingListViewModel) {
    when (val dialog = viewModel.dialog) {
        is ShoppingListDialog.ClearList -> DeleteDialog(
            question = if (dialog.name != null) {
                stringResource(Res.string.shopping_list_clear_question, dialog.name)
            } else {
                stringResource(Res.string.shopping_list_clear_question_single)
            },
            hint = null,
            confirmText = stringResource(Res.string.shopping_list_clear),
            onDelete = { viewModel.clear(dialog.listId) },
            onDismissRequest = viewModel::dismissDialog,
        )
        is ShoppingListDialog.DeleteList -> DeleteDialog(
            question = stringResource(Res.string.shopping_list_delete_question, dialog.name),
            hint = null,
            onDelete = { viewModel.deleteList(dialog.listId) },
            onDismissRequest = viewModel::dismissDialog,
        )
        is ShoppingListDialog.StockItemSkipped -> StockItemSkippedDialog(
            dialog = dialog,
            onSkip = viewModel::skipStockItem,
            onStop = viewModel::stopStockWorkflow,
        )
        null -> Unit
    }
    viewModel.notes?.let { editor ->
        NotesDialog(
            editor = editor,
            onSave = { viewModel.saveNotes() },
            onDismissRequest = viewModel::dismissNotes,
        )
    }
    viewModel.print?.let { options ->
        PrintDialog(
            options = options,
            onShowHeaderChange = viewModel::setPrintHeader,
            onPrint = viewModel::printThermal,
            onDismissRequest = viewModel::dismissPrint,
        )
    }
}

/** Grocy's "Notes" of the list, edited as plain text. */
@Composable
private fun NotesDialog(
    editor: NotesEditor,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val title = stringResource(Res.string.shopping_list_notes)
    // Saved offline too, so there is nothing to wait for
    FormDialog(
        title = title,
        isSaving = false,
        error = null,
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    ) {
        SegmentedGroup {
            TextItem(
                state = editor.text,
                label = title,
                multiLine = true,
                index = 0,
                count = 1,
            )
        }
    }
}

/** Grocy's print options for its thermal printer, the server prints. */
@Composable
private fun PrintDialog(
    options: PrintOptions,
    onShowHeaderChange: (Boolean) -> Unit,
    onPrint: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    FormDialog(
        title = stringResource(Res.string.shopping_list_print_options),
        isSaving = options.isPrinting,
        error = options.error?.printMessage(),
        onSave = onPrint,
        onDismissRequest = onDismissRequest,
        saveText = stringResource(Res.string.shopping_list_thermal_printer),
    ) {
        SegmentedGroup {
            SegmentedSwitchItem(
                headline = stringResource(Res.string.shopping_list_print_show_header),
                checked = options.showHeader,
                onCheckedChange = onShowHeaderChange,
                index = 0,
                count = 1,
            )
        }
    }
}

/** Grocy's "Unable to print" with the server's explanation. */
@Composable
private fun ChangeError.printMessage(): String = when (this) {
    ChangeError.Offline -> saveMessage()
    is ChangeError.Failed -> listOfNotNull(
        stringResource(Res.string.shopping_list_print_failed),
        serverMessage,
    ).joinToString("\n")
}

/**
 * "Add all list items to stock" goes on once a purchase is booked; one left without booking
 * asks whether to skip that item (it stays on the list) or to stop.
 */
@Composable
private fun StockItemSkippedDialog(
    dialog: ShoppingListDialog.StockItemSkipped,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = onStop) {
        ScrollableAlertDialogContent(
            title = {
                Text(
                    stringResource(
                        Res.string.shopping_list_adding_item,
                        dialog.position.toString(),
                        dialog.count.toString(),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onSkip()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.shopping_list_skip))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onStop()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
            content = {
                Text(
                    text = stringResource(Res.string.shopping_list_item_not_booked, dialog.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
