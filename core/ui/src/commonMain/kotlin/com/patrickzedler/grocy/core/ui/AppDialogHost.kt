package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalUriHandler
import com.patrickzedler.grocy.core.design.component.TextDialog
import com.patrickzedler.grocy.core.design.component.tryOpenUri
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_vending_key
import com.patrickzedler.grocy.core.ui.dialog.FeedbackDialog
import com.patrickzedler.grocy.core.ui.dialog.HelpDialog
import com.patrickzedler.grocy.core.ui.dialog.UnlockDialog
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppDialogHost(
    dialog: AppDialog?,
    unlockState: UnlockState?,
    onDialogChange: (AppDialog?) -> Unit,
) {
    if (dialog == null) return
    val onDismissRequest = { onDialogChange(null) }

    when (dialog) {
        AppDialog.FEEDBACK -> FeedbackDialog(
            unlockState = unlockState,
            onSupportClick = { onDialogChange(AppDialog.UNLOCK) },
            onDismissRequest = onDismissRequest,
        )

        AppDialog.HELP -> HelpDialog(onDismissRequest = onDismissRequest)

        AppDialog.UNLOCK -> {
            val uriHandler = LocalUriHandler.current
            val appVendingKey = stringResource(Res.string.app_vending_key)
            UnlockDialog(
                onOpen = { uriHandler.tryOpenUri(appVendingKey) },
                onDismissRequest = onDismissRequest,
            )
        }

        AppDialog.CHANGELOG,
        AppDialog.LICENSE_FONT,
        AppDialog.LICENSE_ICONS,
            -> key(dialog) {
            ResourceTextDialog(dialog = dialog, onDismissRequest = onDismissRequest)
        }
    }
}

@Composable
private fun ResourceTextDialog(
    dialog: AppDialog,
    onDismissRequest: () -> Unit,
) {
    val title = dialog.title ?: return
    val textFile = dialog.textFile ?: return
    val text = rememberResourceText(textFile) ?: return

    TextDialog(
        title = stringResource(title),
        text = text,
        onDismissRequest = onDismissRequest,
        link = dialog.link?.let { stringResource(it) },
    )
}
