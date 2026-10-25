package com.patrickzedler.grocy.feature.settings.impl.server

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.data.server.GrocyVersionCheck
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.TextDialog
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_later
import com.patrickzedler.grocy.core.resources.action_restart
import com.patrickzedler.grocy.core.resources.ic_rounded_dns
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.ic_rounded_logout
import com.patrickzedler.grocy.core.resources.ic_rounded_refresh
import com.patrickzedler.grocy.core.resources.ic_rounded_warning
import com.patrickzedler.grocy.core.resources.settings_server
import com.patrickzedler.grocy.core.resources.settings_server_demo
import com.patrickzedler.grocy.core.resources.settings_server_demo_switch_failed
import com.patrickzedler.grocy.core.resources.settings_server_demo_warning
import com.patrickzedler.grocy.core.resources.settings_server_logout
import com.patrickzedler.grocy.core.resources.settings_server_logout_description
import com.patrickzedler.grocy.core.resources.settings_server_logout_message
import com.patrickzedler.grocy.core.resources.settings_server_logout_title
import com.patrickzedler.grocy.core.resources.settings_server_reload
import com.patrickzedler.grocy.core.resources.settings_server_reload_description
import com.patrickzedler.grocy.core.resources.settings_server_reload_failed
import com.patrickzedler.grocy.core.resources.settings_server_reload_offline
import com.patrickzedler.grocy.core.resources.settings_server_restart_message
import com.patrickzedler.grocy.core.resources.settings_server_restart_title
import com.patrickzedler.grocy.core.resources.settings_server_url
import com.patrickzedler.grocy.core.resources.settings_server_version
import com.patrickzedler.grocy.core.resources.settings_server_version_check_failed
import com.patrickzedler.grocy.core.resources.settings_server_version_loading
import com.patrickzedler.grocy.core.resources.settings_server_version_unknown
import com.patrickzedler.grocy.core.resources.settings_server_version_up_to_date_message
import com.patrickzedler.grocy.core.resources.settings_server_version_up_to_date_title
import com.patrickzedler.grocy.core.resources.settings_server_version_update_message
import com.patrickzedler.grocy.core.resources.settings_server_version_update_title
import com.patrickzedler.grocy.feature.settings.impl.QuestionDialog
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ServerSettingsScreen(
    viewModel: ServerSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val server by viewModel.server.collectAsStateWithLifecycle()
    val reloadState by viewModel.reloadState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showDemoServerDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.error) {
        val message = when (viewModel.error) {
            ServerSettingsError.DEMO_SERVER_UNREACHABLE ->
                Res.string.settings_server_demo_switch_failed
            ServerSettingsError.VERSION_CHECK_FAILED ->
                Res.string.settings_server_version_check_failed
            null -> return@LaunchedEffect
        }
        // Cleared afterwards, a new key would cancel the snackbar while it shows
        snackbarHostState.showSnackbar(getString(message))
        viewModel.onErrorShown()
    }

    LaunchedEffect(reloadState.error) {
        val message = when (reloadState.error) {
            RefreshError.OFFLINE -> Res.string.settings_server_reload_offline
            RefreshError.FAILED -> Res.string.settings_server_reload_failed
            null -> return@LaunchedEffect
        }
        // Cleared afterwards, a new key would cancel the snackbar while it shows
        snackbarHostState.showSnackbar(getString(message))
        viewModel.onReloadErrorShown()
    }

    if (viewModel.isRestartOffered) {
        RestartDialog(
            onRestart = {
                viewModel.onRestartAnswered()
                onRestart()
            },
            onLater = viewModel::onRestartAnswered,
        )
    }

    if (showDemoServerDialog) {
        server?.let {
            DemoServerDialog(
                selectedUrl = it.connection.serverUrl,
                onDemoServerSelected = viewModel::switchDemoServer,
                onDismissRequest = { showDemoServerDialog = false },
            )
        }
    }

    viewModel.versionCheck?.let { check ->
        VersionCheckDialog(check = check, onDismissRequest = viewModel::onVersionCheckShown)
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onLogout = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onCancel = { showLogoutDialog = false },
        )
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_server),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) {
        // Empty only for the moment until the stored login is read
        server?.let {
            serverSettings(
                server = it,
                isLoadingVersion = viewModel.isLoadingVersion,
                isSwitchingDemoServer = viewModel.isSwitchingDemoServer,
                isCheckingVersion = viewModel.isCheckingVersion,
                isReloading = reloadState.isRefreshing,
                onUrlClick = { showDemoServerDialog = true },
                onVersionClick = viewModel::checkGrocyVersion,
                onReloadClick = viewModel::reloadConfiguration,
                onLogoutClick = { showLogoutDialog = true },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.serverSettings(
    server: ServerState,
    isLoadingVersion: Boolean,
    isSwitchingDemoServer: Boolean,
    isCheckingVersion: Boolean,
    isReloading: Boolean,
    onUrlClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReloadClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    if (server.isDemo) {
        segmentedGroup(key = "demo") {
            DemoWarningItem()
        }
    }
    segmentedGroup(key = "server") {
        ValueItem(
            title = stringResource(Res.string.settings_server_url),
            value = server.connection.serverUrl,
            icon = Res.drawable.ic_rounded_dns,
            index = 0,
            count = 2,
            // Only a demo can be swapped for another, an own server needs a new login
            onClick = if (server.isDemo) {
                { if (!isSwitchingDemoServer) onUrlClick() }
            } else {
                null
            },
            isLoading = isSwitchingDemoServer,
        )
        ValueItem(
            title = stringResource(Res.string.settings_server_version),
            value = server.grocyVersion ?: stringResource(
                if (isLoadingVersion) {
                    Res.string.settings_server_version_loading
                } else {
                    Res.string.settings_server_version_unknown
                },
            ),
            icon = Res.drawable.ic_rounded_info,
            index = 1,
            count = 2,
            // A second tap would only repeat the running check
            onClick = { if (!isCheckingVersion) onVersionClick() },
            isLoading = isCheckingVersion,
        )
    }
    segmentedGroup(key = "actions") {
        ActionItem(
            title = stringResource(Res.string.settings_server_reload),
            description = stringResource(Res.string.settings_server_reload_description),
            icon = Res.drawable.ic_rounded_refresh,
            index = 0,
            count = 2,
            // A second tap would only repeat the running download
            onClick = { if (!isReloading) onReloadClick() },
            trailing = if (isReloading) {
                { LoadingIndicator(modifier = Modifier.size(40.dp)) }
            } else {
                null
            },
        )
        ActionItem(
            title = stringResource(Res.string.settings_server_logout),
            description = stringResource(Res.string.settings_server_logout_description),
            icon = Res.drawable.ic_rounded_logout,
            index = 1,
            count = 2,
            onClick = onLogoutClick,
        )
    }
}

/**
 * Grocy's public demo: everything entered there is public until the daily reset. A group of
 * its own above the server, in the error colors, so the warning cannot be overlooked or taken
 * for a setting.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DemoWarningItem() {
    val containerColor = MaterialTheme.colorScheme.error
    val contentColor = MaterialTheme.colorScheme.onError

    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { SettingIcon(Res.drawable.ic_rounded_warning) },
        supportingContent = { Text(stringResource(Res.string.settings_server_demo_warning)) },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.segmentedColors(
            containerColor = containerColor,
            contentColor = contentColor,
            leadingContentColor = contentColor,
            supportingContentColor = contentColor,
        ),
        content = { Text(stringResource(Res.string.settings_server_demo)) },
    )
}

/** A value of the server, clickable if [onClick] is given, with a loading indicator at its end. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ValueItem(
    title: String,
    value: String,
    icon: DrawableResource,
    index: Int,
    count: Int,
    onClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val trailing: (@Composable () -> Unit)? = if (isLoading) {
        { LoadingIndicator(modifier = Modifier.size(40.dp)) }
    } else {
        null
    }
    // The version arrives later on a first visit, a switched demo changes the address
    val supporting: @Composable () -> Unit = {
        Text(
            text = value,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    if (onClick == null) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            leadingContent = { SettingIcon(icon) },
            trailingContent = trailing,
            supportingContent = supporting,
            shapes = shapes,
            colors = SegmentedListDefaults.colors(),
            content = { Text(title) },
        )
    } else {
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                onClick()
            },
            shapes = shapes,
            colors = SegmentedListDefaults.colors(),
            modifier = Modifier.fillMaxWidth().clickCursor(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = { SettingIcon(icon) },
            trailingContent = trailing,
            supportingContent = supporting,
            content = { Text(title) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionItem(
    title: String,
    description: String,
    icon: DrawableResource,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { SettingIcon(icon) },
        trailingContent = trailing,
        supportingContent = { Text(description) },
        content = { Text(title) },
    )
}

@Composable
private fun SettingIcon(icon: DrawableResource) {
    LeadingTrailingContentWrapper {
        Icon(painter = painterResource(icon), contentDescription = null)
    }
}

/** Asked after the configuration was reloaded; dismissing it counts as later. */
@Composable
private fun RestartDialog(
    onRestart: () -> Unit,
    onLater: () -> Unit,
) {
    QuestionDialog(
        title = stringResource(Res.string.settings_server_restart_title),
        message = stringResource(Res.string.settings_server_restart_message),
        confirmText = stringResource(Res.string.action_restart),
        dismissText = stringResource(Res.string.action_later),
        onConfirm = onRestart,
        onDismiss = onLater,
    )
}

/** Logging in again needs the address and the API key, so a stray tap must not log out. */
@Composable
private fun LogoutDialog(
    onLogout: () -> Unit,
    onCancel: () -> Unit,
) {
    QuestionDialog(
        title = stringResource(Res.string.settings_server_logout_title),
        message = stringResource(Res.string.settings_server_logout_message),
        confirmText = stringResource(Res.string.settings_server_logout),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = onLogout,
        onDismiss = onCancel,
    )
}

/** The answer of a version check; a newer Grocy links to its release. */
@Composable
private fun VersionCheckDialog(
    check: GrocyVersionCheck,
    onDismissRequest: () -> Unit,
) {
    when (check) {
        is GrocyVersionCheck.UpToDate -> TextDialog(
            title = stringResource(Res.string.settings_server_version_up_to_date_title),
            text = stringResource(
                Res.string.settings_server_version_up_to_date_message,
                check.serverVersion,
            ),
            onDismissRequest = onDismissRequest,
        )
        is GrocyVersionCheck.Available -> TextDialog(
            title = stringResource(Res.string.settings_server_version_update_title),
            text = stringResource(
                Res.string.settings_server_version_update_message,
                check.latestVersion,
                check.serverVersion,
            ),
            onDismissRequest = onDismissRequest,
            link = check.releaseUrl,
        )
        // Shown as snackbar
        GrocyVersionCheck.Failed -> Unit
    }
}

@Preview
@Composable
private fun ServerSettingsDemoPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_server),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            serverSettings(
                server = ServerState(ServerConnection.Demo(), grocyVersion = "4.7.1"),
                isLoadingVersion = false,
                isSwitchingDemoServer = false,
                isCheckingVersion = true,
                isReloading = false,
                onUrlClick = {},
                onVersionClick = {},
                onReloadClick = {},
                onLogoutClick = {},
            )
        }
    }
}
