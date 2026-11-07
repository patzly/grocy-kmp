package com.patrickzedler.grocy.feature.settings.impl.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.network.NetworkSettings
import com.patrickzedler.grocy.core.model.network.isValidProxyHost
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_later
import com.patrickzedler.grocy.core.resources.action_restart
import com.patrickzedler.grocy.core.resources.ic_rounded_restart_alt
import com.patrickzedler.grocy.core.resources.settings_network
import com.patrickzedler.grocy.core.resources.settings_network_http_proxy
import com.patrickzedler.grocy.core.resources.settings_network_http_proxy_hint
import com.patrickzedler.grocy.core.resources.settings_network_proxy
import com.patrickzedler.grocy.core.resources.settings_network_proxy_host
import com.patrickzedler.grocy.core.resources.settings_network_proxy_host_invalid
import com.patrickzedler.grocy.core.resources.settings_network_proxy_host_missing
import com.patrickzedler.grocy.core.resources.settings_network_proxy_port
import com.patrickzedler.grocy.core.resources.settings_network_restart
import com.patrickzedler.grocy.core.resources.settings_network_restart_hint
import com.patrickzedler.grocy.core.resources.settings_network_tor
import com.patrickzedler.grocy.core.resources.settings_network_tor_hint
import com.patrickzedler.grocy.core.resources.settings_network_tor_off_message
import com.patrickzedler.grocy.core.resources.settings_network_tor_on_message
import com.patrickzedler.grocy.core.resources.settings_server_restart_title
import com.patrickzedler.grocy.feature.settings.impl.QuestionDialog
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import com.patrickzedler.grocy.feature.settings.impl.settingErrorText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The way of the app's requests under "Proxy": Tor through Orbot, else an HTTP proxy whose host
 * and port show below its switch while it is on. Changing Tor asks to restart, other changes
 * leave an item that does, since the app keeps the way it started with.
 */
@Composable
internal fun NetworkSettingsScreen(
    viewModel: NetworkSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScaffold(
        title = stringResource(Res.string.settings_network),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
    ) {
        // Empty for the moment until the stored settings are read
        settings?.let { current ->
            networkSettings(
                settings = current,
                fields = ProxyFields(
                    host = viewModel.host,
                    port = viewModel.port,
                    hostInvalid = viewModel.hostInvalid,
                    portError = viewModel.portError,
                    focusesHost = viewModel.focusesHost,
                ),
                restartRequired = viewModel.restartRequired(current),
                onUseTorChange = viewModel::setUseTor,
                onHttpProxyChange = viewModel::setHttpProxyEnabled,
                onRestartClick = onRestart,
            )
        }
    }

    val useTor = settings?.useTor
    if (viewModel.isRestartOffered && useTor != null) {
        QuestionDialog(
            title = stringResource(Res.string.settings_server_restart_title),
            message = stringResource(
                if (useTor) {
                    Res.string.settings_network_tor_on_message
                } else {
                    Res.string.settings_network_tor_off_message
                },
            ),
            confirmText = stringResource(Res.string.action_restart),
            dismissText = stringResource(Res.string.action_later),
            onConfirm = {
                viewModel.onRestartAnswered()
                onRestart()
            },
            onDismiss = viewModel::onRestartAnswered,
        )
    }
}

/** What the HTTP proxy's fields show. */
private class ProxyFields(
    val host: TextFieldState,
    val port: TextFieldState,
    val hostInvalid: Boolean,
    val portError: SettingError?,
    val focusesHost: Boolean,
)

private fun LazyListScope.networkSettings(
    settings: NetworkSettings,
    fields: ProxyFields,
    restartRequired: Boolean,
    onUseTorChange: (Boolean) -> Unit,
    onHttpProxyChange: (Boolean) -> Unit,
    onRestartClick: () -> Unit,
) {
    segmentedGroup(key = "proxy", title = Res.string.settings_network_proxy) {
        val count = if (settings.httpProxyEnabled) 4 else 2
        SegmentedSwitchItem(
            headline = stringResource(Res.string.settings_network_tor),
            supporting = stringResource(Res.string.settings_network_tor_hint),
            checked = settings.useTor,
            onCheckedChange = onUseTorChange,
            index = 0,
            count = count,
        )
        // Tor takes every request, the proxy would not see any
        SegmentedSwitchItem(
            headline = stringResource(Res.string.settings_network_http_proxy),
            supporting = stringResource(Res.string.settings_network_http_proxy_hint),
            checked = settings.httpProxyEnabled,
            onCheckedChange = onHttpProxyChange,
            index = 1,
            count = count,
            enabled = !settings.useTor,
        )
        val motionScheme = MaterialTheme.motionScheme
        AnimatedVisibility(
            visible = settings.httpProxyEnabled,
            enter = expandVertically(
                animationSpec = motionScheme.defaultSpatialSpec(),
                expandFrom = Alignment.Top,
            ) + fadeIn(motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(
                animationSpec = motionScheme.defaultSpatialSpec(),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(motionScheme.defaultEffectsSpec()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                ProxyHostField(
                    fields = fields,
                    storedHostMissing = !isValidProxyHost(settings.httpProxyHost),
                    enabled = !settings.useTor,
                )
                ProxyPortField(fields = fields, enabled = !settings.useTor)
            }
        }
    }
    if (restartRequired) {
        segmentedGroup(key = "restart") {
            RestartItem(onClick = onRestartClick)
        }
    }
}

@Composable
private fun ProxyHostField(fields: ProxyFields, storedHostMissing: Boolean, enabled: Boolean) {
    // Without a host nothing goes out, which the user should know before wondering why
    val supporting = when {
        fields.hostInvalid -> stringResource(Res.string.settings_network_proxy_host_invalid)
        storedHostMissing -> stringResource(Res.string.settings_network_proxy_host_missing)
        else -> null
    }
    SegmentedContainer(index = 2, count = 4) {
        RoundedTextField(
            state = fields.host,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(stringResource(Res.string.settings_network_proxy_host)) },
            supportingText = supporting?.let { { Text(it) } },
            isError = fields.hostInvalid,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            autoFocus = fields.focusesHost,
        )
    }
}

@Composable
private fun ProxyPortField(fields: ProxyFields, enabled: Boolean) {
    val error = fields.portError?.let { settingErrorText(it) }
    SegmentedContainer(index = 3, count = 4) {
        RoundedTextField(
            state = fields.port,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(stringResource(Res.string.settings_network_proxy_port)) },
            supportingText = error?.let { { Text(it) } },
            isError = error != null,
            // The last field of the page: done closes the keyboard
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestartItem(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_restart_alt),
                    contentDescription = null,
                )
            }
        },
        supportingContent = { Text(stringResource(Res.string.settings_network_restart_hint)) },
        content = { Text(stringResource(Res.string.settings_network_restart)) },
    )
}

@Preview
@Composable
private fun NetworkSettingsPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_network),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            networkSettings(
                settings = NetworkSettings(httpProxyEnabled = true, httpProxyHost = "proxy.lan"),
                fields = ProxyFields(
                    host = TextFieldState("proxy.lan"),
                    port = TextFieldState("8080"),
                    hostInvalid = false,
                    portError = null,
                    focusesHost = false,
                ),
                restartRequired = true,
                onUseTorChange = {},
                onHttpProxyChange = {},
                onRestartClick = {},
            )
        }
    }
}
