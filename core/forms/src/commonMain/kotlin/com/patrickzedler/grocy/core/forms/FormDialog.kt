package com.patrickzedler.grocy.core.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_save
import org.jetbrains.compose.resources.stringResource

/**
 * A form in a dialog, like Grocy's forms that open as popup: [content] holds its groups of
 * segmented items, [error] shows why saving failed above them. While [isSaving] the confirm
 * button is a loading indicator and the dialog stays open.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FormDialog(
    title: String,
    isSaving: Boolean,
    error: String?,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
    subtitle: String? = null,
    saveText: String = stringResource(Res.string.action_save),
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dismiss = { if (!isSaving) onDismissRequest() }

    ScrollableAlertDialog(onDismissRequest = dismiss, paneTitle = title) {
        ScrollableAlertDialogContent(
            title = { Text(title) },
            subtitle = subtitle?.let { { Text(it) } },
            confirmButton = {
                if (isSaving) {
                    LoadingIndicator(modifier = Modifier.size(40.dp))
                } else {
                    TextButton(
                        onClick = {
                            haptic.confirm()
                            onSave()
                        },
                        modifier = Modifier.clickCursor(),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(saveText)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        dismiss()
                    },
                    modifier = Modifier.clickCursor(enabled = !isSaving),
                    enabled = !isSaving,
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SegmentedListDefaults.GroupSpacing),
            ) {
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                content()
            }
        }
    }
}
