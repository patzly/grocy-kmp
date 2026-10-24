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
import com.patrickzedler.grocy.core.design.component.TextDialog
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.UpdateCheck
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_later
import com.patrickzedler.grocy.core.resources.action_update
import com.patrickzedler.grocy.core.resources.update_available
import com.patrickzedler.grocy.core.resources.update_available_unknown
import com.patrickzedler.grocy.core.resources.update_available_version
import com.patrickzedler.grocy.core.resources.update_failed
import com.patrickzedler.grocy.core.resources.update_failed_description
import com.patrickzedler.grocy.core.resources.update_none
import com.patrickzedler.grocy.core.resources.update_none_description
import org.jetbrains.compose.resources.stringResource

/**
 * Answer of an update check the user asked for: the update with its choice, or a text dialog
 * that the app is up to date ([versionName] is the running version) or the check failed.
 */
@Composable
fun UpdateCheckDialog(
    result: UpdateCheck,
    versionName: String,
    onUpdate: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    when (result) {
        is UpdateCheck.Available -> UpdateAvailableDialog(
            update = result,
            onUpdate = onUpdate,
            onLater = onDismissRequest,
        )

        UpdateCheck.UpToDate -> TextDialog(
            title = stringResource(Res.string.update_none),
            text = stringResource(Res.string.update_none_description, versionName),
            onDismissRequest = onDismissRequest,
        )

        UpdateCheck.Failed -> TextDialog(
            title = stringResource(Res.string.update_failed),
            text = stringResource(Res.string.update_failed_description),
            onDismissRequest = onDismissRequest,
        )
    }
}

/** Offers [update]; dismissing the dialog counts as later. */
@Composable
fun UpdateAvailableDialog(
    update: UpdateCheck.Available,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(
        onDismissRequest = onLater,
        modifier = modifier,
    ) {
        UpdateAvailableDialogContent(
            versionName = update.versionName,
            onUpdateClick = {
                haptic.confirm()
                onUpdate()
            },
            onLaterClick = {
                haptic.confirm()
                onLater()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateAvailableDialogContent(
    versionName: String?,
    modifier: Modifier = Modifier,
    onUpdateClick: () -> Unit = {},
    onLaterClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.update_available)) },
        confirmButton = {
            TextButton(
                onClick = onUpdateClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_update))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLaterClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_later))
            }
        },
    ) {
        Text(
            text = if (versionName != null) {
                stringResource(Res.string.update_available_version, versionName)
            } else {
                stringResource(Res.string.update_available_unknown)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun UpdateAvailableDialogPreview() {
    GrocyTheme {
        UpdateAvailableDialogContent(versionName = "1.2.0")
    }
}

@Preview
@Composable
private fun UpdateAvailableUnknownVersionDialogPreview() {
    GrocyTheme {
        UpdateAvailableDialogContent(versionName = null)
    }
}
