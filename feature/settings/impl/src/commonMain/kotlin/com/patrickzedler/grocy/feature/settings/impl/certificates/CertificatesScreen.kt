package com.patrickzedler.grocy.feature.settings.impl.certificates

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.model.network.CertificateSettings
import com.patrickzedler.grocy.core.model.network.ServerCertificate
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.certificates_check
import com.patrickzedler.grocy.core.resources.certificates_check_description
import com.patrickzedler.grocy.core.resources.certificates_check_failed
import com.patrickzedler.grocy.core.resources.certificates_check_ok
import com.patrickzedler.grocy.core.resources.certificates_client
import com.patrickzedler.grocy.core.resources.certificates_client_none
import com.patrickzedler.grocy.core.resources.certificates_client_remove
import com.patrickzedler.grocy.core.resources.certificates_fingerprint
import com.patrickzedler.grocy.core.resources.certificates_remove
import com.patrickzedler.grocy.core.resources.certificates_trusted
import com.patrickzedler.grocy.core.resources.certificates_trusted_empty
import com.patrickzedler.grocy.core.resources.certificates_valid_until
import com.patrickzedler.grocy.core.resources.ic_rounded_delete
import com.patrickzedler.grocy.core.resources.ic_rounded_key
import com.patrickzedler.grocy.core.resources.ic_rounded_policy
import com.patrickzedler.grocy.core.resources.ic_rounded_refresh
import com.patrickzedler.grocy.core.resources.settings_certificates
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The client certificate, chosen from the device's, and the servers' certificates the user
 * trusted while connecting. Without either the app's connections work like the system's.
 */
@Composable
internal fun CertificatesScreen(
    viewModel: CertificatesViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val checkableServer by viewModel.checkableServer.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pick = rememberClientCertificatePicker(
        currentAlias = settings?.clientAlias,
        onPicked = viewModel::setClientAlias,
    )

    CertificateCheckEffect(viewModel, snackbarHostState)

    SettingsScaffold(
        title = stringResource(Res.string.settings_certificates),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
        snackbarHostState = snackbarHostState,
    ) {
        // Empty for the moment until they are read
        settings?.let {
            certificates(
                settings = it,
                canCheckServer = checkableServer != null,
                isChecking = viewModel.isChecking,
                onClientClick = pick,
                onClientRemove = { viewModel.setClientAlias(null) },
                onTrustedRemove = viewModel::remove,
                onCheckClick = viewModel::checkServer,
            )
        }
    }
}

/** Tells how the check of the server's certificate went. */
@Composable
internal fun CertificateCheckEffect(
    viewModel: CertificatesViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel.checkResult) {
        val message = when (viewModel.checkResult) {
            CertificateCheck.TRUSTED -> Res.string.certificates_check_ok
            CertificateCheck.FAILED -> Res.string.certificates_check_failed
            null -> return@LaunchedEffect
        }
        // Cleared afterwards, a new key would cancel the snackbar while it shows
        snackbarHostState.showNewestSnackbar(getString(message))
        viewModel.onCheckResultShown()
    }
}

/** Everything about certificates as one group of the server settings, below their [title]. */
internal fun LazyListScope.certificateGroup(
    title: StringResource,
    settings: CertificateSettings,
    canCheckServer: Boolean,
    isChecking: Boolean,
    onClientClick: () -> Unit,
    onClientRemove: () -> Unit,
    onTrustedRemove: (TrustedCertificate) -> Unit,
    onCheckClick: () -> Unit,
) {
    segmentedGroup(key = "certificates", title = title) {
        val count = 1 + trustedItemCount(settings, canCheckServer)
        ClientItem(settings.clientAlias, onClientClick, onClientRemove, index = 0, count = count)
        TrustedItems(
            settings,
            canCheckServer,
            isChecking,
            firstIndex = 1,
            count,
            onTrustedRemove,
            onCheckClick,
        )
    }
}

private fun trustedItemCount(settings: CertificateSettings, canCheckServer: Boolean): Int =
    settings.trusted.size.coerceAtLeast(1) + if (canCheckServer) 1 else 0

@Composable
private fun TrustedItems(
    settings: CertificateSettings,
    canCheckServer: Boolean,
    isChecking: Boolean,
    firstIndex: Int,
    count: Int,
    onTrustedRemove: (TrustedCertificate) -> Unit,
    onCheckClick: () -> Unit,
) {
    if (settings.trusted.isEmpty()) EmptyItem(firstIndex, count)
    settings.trusted.forEachIndexed { index, trusted ->
        TrustedItem(trusted, firstIndex + index, count) { onTrustedRemove(trusted) }
    }
    if (canCheckServer) CheckItem(index = count - 1, count = count, isChecking, onCheckClick)
}

private fun LazyListScope.certificates(
    settings: CertificateSettings,
    canCheckServer: Boolean,
    isChecking: Boolean,
    onClientClick: () -> Unit,
    onClientRemove: () -> Unit,
    onTrustedRemove: (TrustedCertificate) -> Unit,
    onCheckClick: () -> Unit,
) {
    segmentedGroup(key = "client") {
        ClientItem(settings.clientAlias, onClientClick, onClientRemove, index = 0, count = 1)
    }
    segmentedGroup(key = "trusted", title = Res.string.certificates_trusted) {
        TrustedItems(
            settings,
            canCheckServer,
            isChecking,
            firstIndex = 0,
            count = trustedItemCount(settings, canCheckServer),
            onTrustedRemove,
            onCheckClick,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClientItem(
    alias: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    index: Int,
    count: Int,
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
        leadingContent = { ItemIcon(Res.drawable.ic_rounded_key) },
        trailingContent = if (alias != null) {
            { RemoveButton(stringResource(Res.string.certificates_client_remove), onRemove) }
        } else {
            null
        },
        supportingContent = {
            Text(alias ?: stringResource(Res.string.certificates_client_none))
        },
        content = { Text(stringResource(Res.string.certificates_client)) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyItem(index: Int, count: Int) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        content = { Text(stringResource(Res.string.certificates_trusted_empty)) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrustedItem(trusted: TrustedCertificate, index: Int, count: Int, onRemove: () -> Unit) {
    val certificate = trusted.certificate
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        leadingContent = { ItemIcon(Res.drawable.ic_rounded_policy) },
        trailingContent = {
            RemoveButton(stringResource(Res.string.certificates_remove), onRemove)
        },
        supportingContent = {
            // The fingerprint wraps, it is only worth something complete
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(
                        Res.string.certificates_valid_until,
                        formatDate(certificate.notAfterEpochDay),
                    ),
                )
                Text(stringResource(Res.string.certificates_fingerprint, certificate.fingerprint))
            }
        },
        content = { Text(trusted.host) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CheckItem(index: Int, count: Int, isChecking: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            // A second tap would only repeat the running check
            if (!isChecking) onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { ItemIcon(Res.drawable.ic_rounded_refresh) },
        trailingContent = if (isChecking) {
            { LoadingIndicator(modifier = Modifier.size(40.dp)) }
        } else {
            null
        },
        supportingContent = { Text(stringResource(Res.string.certificates_check_description)) },
        content = { Text(stringResource(Res.string.certificates_check)) },
    )
}

@Composable
private fun ItemIcon(icon: DrawableResource) {
    LeadingTrailingContentWrapper {
        Icon(painter = painterResource(icon), contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemoveButton(description: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    HapticTooltipBox(text = description) {
        IconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = Modifier.clickCursor(),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_delete),
                contentDescription = description,
            )
        }
    }
}

@Preview
@Composable
private fun CertificatesPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_certificates),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            certificates(
                settings = CertificateSettings(
                    clientAlias = "Phone",
                    trusted = listOf(
                        TrustedCertificate(
                            host = "grocy.local",
                            certificate = ServerCertificate("abcd", "CN=grocy", "CN=grocy", 21_000),
                        ),
                    ),
                ),
                canCheckServer = true,
                isChecking = false,
                onClientClick = {},
                onClientRemove = {},
                onTrustedRemove = {},
                onCheckClick = {},
            )
        }
    }
}
