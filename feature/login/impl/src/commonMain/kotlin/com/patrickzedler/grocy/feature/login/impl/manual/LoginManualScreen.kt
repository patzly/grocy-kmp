package com.patrickzedler.grocy.feature.login.impl.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.InsetLazyColumn
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.RoundedSecureTextField
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.network.ServerUrl
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.login_manual_api_key
import com.patrickzedler.grocy.core.resources.login_manual_connect
import com.patrickzedler.grocy.core.resources.login_manual_create_key
import com.patrickzedler.grocy.core.resources.login_manual_ha_create_token
import com.patrickzedler.grocy.core.resources.login_manual_ha_pending
import com.patrickzedler.grocy.core.resources.login_manual_ha_token
import com.patrickzedler.grocy.core.resources.login_manual_ha_url
import com.patrickzedler.grocy.core.resources.login_manual_ha_url_hint
import com.patrickzedler.grocy.core.resources.login_manual_ingress_id
import com.patrickzedler.grocy.core.resources.login_manual_ingress_id_hint
import com.patrickzedler.grocy.core.resources.login_manual_server_url
import com.patrickzedler.grocy.core.resources.login_manual_type_grocy
import com.patrickzedler.grocy.core.resources.login_manual_type_home_assistant
import com.patrickzedler.grocy.core.resources.login_manual_url_invalid
import com.patrickzedler.grocy.core.resources.login_title
import com.patrickzedler.grocy.feature.login.impl.message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginManualScreen(
    viewModel: LoginManualViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isEditable = !viewModel.isLoggingIn

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.login_title)) },
                navigationIcon = { NavigateBackButton(onClick = onBackClick) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        InsetLazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            insetItem {
                ConnectedButtonGroup(
                    options = ServerType.entries,
                    checked = viewModel.serverType,
                    onCheckedChange = viewModel::selectServerType,
                    enabled = isEditable,
                    label = { it.label() },
                )
            }
            insetItem {
                ConnectedButtonGroup(
                    options = ServerUrl.Scheme.entries,
                    checked = viewModel.scheme,
                    onCheckedChange = viewModel::selectScheme,
                    enabled = isEditable,
                    // Protocol names, not translated
                    label = { it.name.uppercase() },
                )
            }

            when (viewModel.serverType) {
                ServerType.GROCY -> grocyFields(
                    viewModel = viewModel,
                    enabled = isEditable,
                    onCreateKeyClick = {
                        viewModel.normalizedGrocyUrl?.let { uriHandler.openUri("$it/manageapikeys") }
                    },
                )

                ServerType.HOME_ASSISTANT -> homeAssistantFields(
                    viewModel = viewModel,
                    enabled = isEditable,
                    onCreateTokenClick = {
                        viewModel.normalizedHomeAssistantUrl?.let {
                            uriHandler.openUri("$it/profile/security")
                        }
                    },
                )
            }

            viewModel.error?.let { error ->
                insetItem {
                    Text(
                        text = error.message(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        // Screen readers announce the error as soon as it appears
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }

            insetItem {
                ConnectButton(
                    isLoggingIn = viewModel.isLoggingIn,
                    enabled = viewModel.canConnect,
                    onClick = viewModel::connect,
                )
            }
        }
    }
}

private fun LazyListScope.grocyFields(
    viewModel: LoginManualViewModel,
    enabled: Boolean,
    onCreateKeyClick: () -> Unit,
) {
    insetItem {
        UrlField(
            state = viewModel.grocyUrl,
            label = stringResource(Res.string.login_manual_server_url),
            isInvalid = viewModel.grocyUrl.text.isNotBlank() && viewModel.normalizedGrocyUrl == null,
            enabled = enabled,
        )
    }
    insetItem {
        SecretField(
            state = viewModel.apiKey,
            label = stringResource(Res.string.login_manual_api_key),
            enabled = enabled,
            imeAction = ImeAction.Done,
            onDone = viewModel::connect,
        )
    }
    insetItem {
        Button(
            onClick = onCreateKeyClick,
            enabled = enabled && viewModel.normalizedGrocyUrl != null,
        ) {
            Text(stringResource(Res.string.login_manual_create_key))
        }
    }
}

private fun LazyListScope.homeAssistantFields(
    viewModel: LoginManualViewModel,
    enabled: Boolean,
    onCreateTokenClick: () -> Unit,
) {
    insetItem {
        UrlField(
            state = viewModel.homeAssistantUrl,
            label = stringResource(Res.string.login_manual_ha_url),
            isInvalid = viewModel.homeAssistantUrl.text.isNotBlank() &&
                viewModel.normalizedHomeAssistantUrl == null,
            enabled = enabled,
            hint = stringResource(Res.string.login_manual_ha_url_hint),
        )
    }
    insetItem {
        SecretField(
            state = viewModel.homeAssistantToken,
            label = stringResource(Res.string.login_manual_ha_token),
            enabled = enabled,
        )
    }
    insetItem {
        Button(
            onClick = onCreateTokenClick,
            enabled = enabled && viewModel.normalizedHomeAssistantUrl != null,
        ) {
            Text(stringResource(Res.string.login_manual_ha_create_token))
        }
    }
    insetItem {
        RoundedTextField(
            state = viewModel.ingressProxyId,
            label = { Text(stringResource(Res.string.login_manual_ingress_id)) },
            supportingText = { Text(stringResource(Res.string.login_manual_ingress_id_hint)) },
            enabled = enabled,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    insetItem {
        SecretField(
            state = viewModel.apiKey,
            label = stringResource(Res.string.login_manual_api_key),
            enabled = enabled,
        )
    }
    insetItem {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.login_manual_ha_pending),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun UrlField(
    state: TextFieldState,
    label: String,
    isInvalid: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    val invalidText = stringResource(Res.string.login_manual_url_invalid)
    val supportingText = if (isInvalid) invalidText else hint

    RoundedTextField(
        state = state,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isInvalid,
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SecretField(
    state: TextFieldState,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    RoundedSecureTextField(
        state = state,
        label = { Text(label) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        onKeyboardAction = { performDefaultAction ->
            if (imeAction == ImeAction.Done) onDone() else performDefaultAction()
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectButton(
    isLoggingIn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Button(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapesFor(buttonHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = buttonHeight),
    ) {
        if (isLoggingIn) {
            LoadingIndicator(modifier = Modifier.size(32.dp))
        } else {
            Text(
                text = stringResource(Res.string.login_manual_connect),
                style = ButtonDefaults.textStyleFor(buttonHeight),
            )
        }
    }
}

@Composable
private fun ServerType.label(): String =
    stringResource(
        when (this) {
            ServerType.GROCY -> Res.string.login_manual_type_grocy
            ServerType.HOME_ASSISTANT -> Res.string.login_manual_type_home_assistant
        },
    )
