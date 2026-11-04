package com.patrickzedler.grocy.apps.wear

import android.app.RemoteInput
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.login_choice_demo_server
import com.patrickzedler.grocy.core.resources.wear_login_api_key
import com.patrickzedler.grocy.core.resources.wear_login_connect
import com.patrickzedler.grocy.core.resources.wear_login_failed
import com.patrickzedler.grocy.core.resources.wear_login_manual
import com.patrickzedler.grocy.core.resources.wear_login_server
import com.patrickzedler.grocy.core.resources.wear_login_waiting
import com.patrickzedler.grocy.core.resources.login_manual_url_invalid
import com.patrickzedler.grocy.core.wear.WearLoginError
import com.patrickzedler.grocy.core.wear.WearViewModel
import org.jetbrains.compose.resources.stringResource

private const val INPUT_KEY = "value"

/**
 * Waits for the login the phone hands over by itself; the way to the phone's keyboard is for
 * phones without Play Services.
 */
@Composable
internal fun WaitingForPhoneScreen(
    viewModel: WearViewModel,
    onManualClick: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val haptic = LocalHapticFeedback.current

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "title") {
                ListHeader(Modifier.transformedHeight(this, spec)) { Text("Grocy") }
            }
            item(key = "waiting") {
                Text(
                    text = stringResource(Res.string.wear_login_waiting),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item(key = "manual") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onManualClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(stringResource(Res.string.wear_login_manual)) },
                )
            }
            item(key = "demo") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.connectToDemo()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(stringResource(Res.string.login_choice_demo_server)) },
                )
            }
        }
    }
}

/**
 * Address and API key, each from the system's text input, which on a watch offers the keyboard
 * of the paired phone: nobody types an API key on a watch.
 */
@Composable
internal fun ManualLoginScreen(
    viewModel: WearViewModel,
    isLoggedIn: Boolean,
    onDone: () -> Unit,
) {
    val form by viewModel.login.collectAsStateWithLifecycle()
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val haptic = LocalHapticFeedback.current
    val server = stringResource(Res.string.wear_login_server)
    val apiKey = stringResource(Res.string.wear_login_api_key)
    val serverInput = rememberRemoteInput(viewModel::setServerUrl)
    val apiKeyInput = rememberRemoteInput(viewModel::setApiKey)

    if (isLoggedIn) LaunchedEffect(Unit) { onDone() }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    viewModel.connect()
                },
                enabled = !form.isConnecting && form.serverUrl.isNotBlank() &&
                    form.apiKey.isNotBlank(),
            ) {
                if (form.isConnecting) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Text(stringResource(Res.string.wear_login_connect))
                }
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item(key = "server") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        serverInput(server)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(server) },
                    secondaryLabel = form.serverUrl.ifEmpty { null }?.let { { Text(it) } },
                )
            }
            item(key = "apiKey") {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        apiKeyInput(apiKey)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(apiKey) },
                    // The key itself never shows on the wrist
                    secondaryLabel = form.apiKey.ifEmpty { null }?.let { { Text("••••••") } },
                )
            }
            form.error?.let { error ->
                item(key = "error") {
                    Text(
                        text = stringResource(
                            when (error) {
                                WearLoginError.INVALID_ADDRESS ->
                                    Res.string.login_manual_url_invalid
                                WearLoginError.FAILED -> Res.string.wear_login_failed
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** Opens the system's text input with a label, [onResult] gets what was entered. */
@Composable
private fun rememberRemoteInput(onResult: (String) -> Unit): (String) -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.let(RemoteInput::getResultsFromIntent)
            ?.getCharSequence(INPUT_KEY)
            ?.let { onResult(it.toString()) }
    }
    return { label ->
        val input = RemoteInput.Builder(INPUT_KEY)
            .setLabel(label)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }
            .build()
        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(input))
        launcher.launch(intent)
    }
}
