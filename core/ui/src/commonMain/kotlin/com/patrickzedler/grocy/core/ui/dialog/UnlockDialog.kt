package com.patrickzedler.grocy.core.ui.dialog

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
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_open_play_store
import com.patrickzedler.grocy.core.resources.msg_unlock
import com.patrickzedler.grocy.core.resources.msg_unlock_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UnlockDialog(
    onOpen: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        UnlockDialogContent(
            onOpenClick = {
                haptic.confirm()
                onOpen()
                onDismissRequest()
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
private fun UnlockDialogContent(
    modifier: Modifier = Modifier,
    onOpenClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.msg_unlock)) },
        confirmButton = {
            TextButton(
                onClick = onOpenClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_open_play_store))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    ) {
        Text(
            text = stringResource(Res.string.msg_unlock_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun UnlockDialogPreview() {
    GrocyTheme {
        UnlockDialogContent()
    }
}
