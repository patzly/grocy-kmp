package com.patrickzedler.grocy.apps.shared

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.network.LocalNetworkGate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_continue
import com.patrickzedler.grocy.core.resources.action_not_now
import com.patrickzedler.grocy.core.resources.local_network_denied_message
import com.patrickzedler.grocy.core.resources.local_network_message
import com.patrickzedler.grocy.core.resources.local_network_title
import com.patrickzedler.grocy.core.resources.scanner_open_settings
import com.patrickzedler.grocy.core.ui.LocalNetworkPermission
import com.patrickzedler.grocy.core.ui.rememberLocalNetworkAccess
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the local network when a request goes there without the permission (Android 17), so
 * the server answers right after "Allow"; the request waits at [gate] meanwhile. Explains first
 * why, since the question can come at app start, and leads to the settings once Android no
 * longer asks.
 */
@Composable
internal fun LocalNetworkPrompt(gate: LocalNetworkGate) {
    // Hidden while Android's own question is shown, the request keeps waiting for its answer
    var isRequesting by rememberSaveable { mutableStateOf(false) }
    val access = rememberLocalNetworkAccess {
        isRequesting = false
        gate.onAnswered()
    } ?: return

    // Android's question is an activity of its own, which only pauses this one
    LifecycleStartEffect(gate) {
        gate.setCanAsk(true)
        onStopOrDispose { gate.setCanAsk(false) }
    }

    val isAsking by gate.isAsking.collectAsStateWithLifecycle()
    if (!isAsking || isRequesting) return
    val isDenied = access.permission == LocalNetworkPermission.Denied
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = gate::onAnswered) {
        LocalNetworkDialogContent(
            isDenied = isDenied,
            onConfirmClick = {
                haptic.confirm()
                if (isDenied) {
                    access.openSettings()
                    gate.onAnswered()
                } else {
                    isRequesting = true
                    access.request()
                }
            },
            onDismissClick = {
                haptic.confirm()
                gate.onAnswered()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocalNetworkDialogContent(
    isDenied: Boolean,
    onConfirmClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        title = { Text(stringResource(Res.string.local_network_title)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    stringResource(
                        if (isDenied) Res.string.scanner_open_settings else Res.string.action_continue,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_not_now))
            }
        },
    ) {
        Text(
            text = stringResource(
                if (isDenied) {
                    Res.string.local_network_denied_message
                } else {
                    Res.string.local_network_message
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun LocalNetworkDialogPreview() {
    GrocyTheme {
        LocalNetworkDialogContent(isDenied = false)
    }
}
