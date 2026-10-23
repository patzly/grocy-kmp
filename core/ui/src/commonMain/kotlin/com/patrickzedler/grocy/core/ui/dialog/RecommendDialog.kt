package com.patrickzedler.grocy.core.ui.dialog

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_copy_to_clipboard
import com.patrickzedler.grocy.core.resources.action_recommend
import com.patrickzedler.grocy.core.ui.plainTextClipEntry
import com.patrickzedler.grocy.core.ui.rememberRecommendText
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The recommendation to copy, where the platform has no share sheet. */
@Composable
internal fun RecommendDialog(
    isPlayVariant: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val text = rememberRecommendText(isPlayVariant)
    val label = stringResource(Res.string.action_recommend)

    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        RecommendDialogContent(
            text = text,
            onCopyClick = {
                haptic.confirm()
                // Started right away, closing the dialog cancels its scope
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    clipboard.setClipEntry(plainTextClipEntry(text, label))
                }
                onDismissRequest()
            },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecommendDialogContent(
    text: String,
    modifier: Modifier = Modifier,
    onCopyClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.action_recommend)) },
        confirmButton = {
            TextButton(onClick = onCopyClick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.action_copy_to_clipboard))
            }
        },
        dismissButton = {
            TextButton(onClick = onCloseClick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.action_close))
            }
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun RecommendDialogPreview() {
    GrocyTheme {
        RecommendDialogContent(text = "Grocy keeps track of groceries: https://github.com")
    }
}
