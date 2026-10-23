package com.patrickzedler.grocy.feature.masterdata.impl.product

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_discard
import com.patrickzedler.grocy.core.resources.action_save
import com.patrickzedler.grocy.core.resources.msg_unsaved_changes
import com.patrickzedler.grocy.core.resources.msg_unsaved_changes_description
import org.jetbrains.compose.resources.stringResource

/** Asks before leaving a form with changes: save them, discard them, or keep editing. */
@Composable
internal fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        UnsavedChangesDialogContent(
            onSaveClick = {
                haptic.confirm()
                onDismissRequest()
                onSave()
            },
            onDiscardClick = {
                haptic.confirm()
                onDismissRequest()
                onDiscard()
            },
            onCancelClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnsavedChangesDialogContent(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {},
    onDiscardClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.msg_unsaved_changes)) },
        confirmButton = {
            TextButton(onClick = onSaveClick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscardClick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.action_discard))
            }
        },
        extraButton = {
            TextButton(onClick = onCancelClick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    ) {
        Text(
            text = stringResource(Res.string.msg_unsaved_changes_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun UnsavedChangesDialogPreview() {
    GrocyTheme {
        UnsavedChangesDialogContent()
    }
}
