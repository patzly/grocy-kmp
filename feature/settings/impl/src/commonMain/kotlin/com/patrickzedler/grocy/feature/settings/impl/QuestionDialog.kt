package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm

/**
 * A question with two answers; dismissing the dialog counts as [onDismiss]. While
 * [isConfirming] (the confirmed action runs) it shows a loading indicator and takes no answer.
 */
@Composable
internal fun QuestionDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isConfirming: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(onDismissRequest = { if (!isConfirming) onDismiss() }) {
        QuestionDialogContent(
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            isConfirming = isConfirming,
            onConfirmClick = {
                haptic.confirm()
                onConfirm()
            },
            onDismissClick = {
                haptic.confirm()
                onDismiss()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuestionDialogContent(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    isConfirming: Boolean = false,
    onConfirmClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        title = { Text(title) },
        confirmButton = {
            if (isConfirming) {
                LoadingIndicator(modifier = Modifier.size(40.dp))
            } else {
                TextButton(
                    onClick = onConfirmClick,
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissClick,
                modifier = Modifier.clickCursor(enabled = !isConfirming),
                enabled = !isConfirming,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(dismissText)
            }
        },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun QuestionDialogPreview() {
    GrocyTheme {
        QuestionDialogContent(
            title = "Log out?",
            message = "You return to the login screen.",
            confirmText = "Logout",
            dismissText = "Cancel",
        )
    }
}

@Preview
@Composable
private fun QuestionDialogConfirmingPreview() {
    GrocyTheme {
        QuestionDialogContent(
            title = "Switch demo server?",
            message = "The demo is also available in German.",
            confirmText = "Switch",
            dismissText = "Keep",
            isConfirming = true,
        )
    }
}
