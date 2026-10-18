package com.patrickzedler.grocy.feature.login.impl.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.ConnectedButtonGroup
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.RoundedSecureTextField
import com.patrickzedler.grocy.core.design.component.RoundedTextField
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.tryOpenUri
import com.patrickzedler.grocy.core.network.ServerUrl
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_dns
import com.patrickzedler.grocy.core.resources.ic_rounded_key
import com.patrickzedler.grocy.core.resources.login_manual_addon_slug
import com.patrickzedler.grocy.core.resources.login_manual_addon_slug_hint
import com.patrickzedler.grocy.core.resources.login_manual_api_key
import com.patrickzedler.grocy.core.resources.login_manual_connect
import com.patrickzedler.grocy.core.resources.login_manual_create_key
import com.patrickzedler.grocy.core.resources.login_manual_create_key_description
import com.patrickzedler.grocy.core.resources.login_manual_group_addon
import com.patrickzedler.grocy.core.resources.login_manual_group_grocy
import com.patrickzedler.grocy.core.resources.login_manual_ha_create_token
import com.patrickzedler.grocy.core.resources.login_manual_ha_create_token_description
import com.patrickzedler.grocy.core.resources.login_manual_ha_token
import com.patrickzedler.grocy.core.resources.login_manual_ha_unsupported
import com.patrickzedler.grocy.core.resources.login_manual_ha_url
import com.patrickzedler.grocy.core.resources.login_manual_ha_url_hint
import com.patrickzedler.grocy.core.resources.login_manual_server_type
import com.patrickzedler.grocy.core.resources.login_manual_server_type_description
import com.patrickzedler.grocy.core.resources.login_manual_server_url
import com.patrickzedler.grocy.core.resources.login_manual_type_grocy
import com.patrickzedler.grocy.core.resources.login_manual_type_home_assistant
import com.patrickzedler.grocy.core.resources.login_manual_url_invalid
import com.patrickzedler.grocy.core.resources.login_manual_use_grocy
import com.patrickzedler.grocy.core.resources.login_title
import com.patrickzedler.grocy.feature.login.impl.LoginErrorItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginManualScreen(
    viewModel: LoginManualViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val isEditable = !viewModel.isLoggingIn
    val isHomeAssistant = viewModel.serverType == ServerType.HOME_ASSISTANT

    SegmentedListScreen(
        title = stringResource(Res.string.login_title),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
    ) {
        segmentedGroup(key = "type") {
            SegmentedContainer(index = 0, count = 1) {
                SegmentedTexts(
                    headline = stringResource(Res.string.login_manual_server_type),
                    supporting = stringResource(Res.string.login_manual_server_type_description),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ConnectedButtonGroup(
                    options = ServerType.entries,
                    checked = viewModel.serverType,
                    onCheckedChange = viewModel::selectServerType,
                    enabled = isEditable,
                    label = { it.label() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        when {
            !isHomeAssistant -> grocyGroup(
                viewModel = viewModel,
                enabled = isEditable,
                onCreateKeyClick = {
                    viewModel.normalizedGrocyUrl?.let { uriHandler.tryOpenUri("$it/manageapikeys") }
                },
            )

            viewModel.isHomeAssistantSupported -> homeAssistantGroups(
                viewModel = viewModel,
                enabled = isEditable,
                onCreateTokenClick = {
                    viewModel.normalizedHomeAssistantUrl?.let {
                        uriHandler.tryOpenUri("$it/profile/security")
                    }
                },
            )

            else -> homeAssistantUnsupportedGroup(
                onUseGrocyClick = { viewModel.selectServerType(ServerType.GROCY) },
            )
        }

        if (!isHomeAssistant || viewModel.isHomeAssistantSupported) {
            viewModel.error?.let { error ->
                segmentedGroup(key = "error") {
                    LoginErrorItem(error = error, index = 0, count = 1)
                }
            }
            // The primary action of the form, on the background like a dialog's buttons
            insetItem(key = "connect") {
                ConnectButton(
                    isLoggingIn = viewModel.isLoggingIn,
                    enabled = viewModel.canConnect,
                    onClick = viewModel::connect,
                )
            }
        }
    }
}

private fun LazyListScope.grocyGroup(
    viewModel: LoginManualViewModel,
    enabled: Boolean,
    onCreateKeyClick: () -> Unit,
) {
    segmentedGroup(key = "grocy", title = Res.string.login_manual_group_grocy) {
        AddressItem(
            state = viewModel.grocyUrl,
            label = stringResource(Res.string.login_manual_server_url),
            isInvalid = viewModel.grocyUrl.text.isNotBlank() &&
                viewModel.normalizedGrocyUrl == null,
            scheme = viewModel.scheme,
            onSchemeChange = viewModel::selectScheme,
            enabled = enabled,
            index = 0,
            count = 3,
        )
        SegmentedContainer(index = 1, count = 3) {
            SecretField(
                state = viewModel.apiKey,
                label = stringResource(Res.string.login_manual_api_key),
                enabled = enabled,
                imeAction = ImeAction.Done,
                onDone = viewModel::connect,
            )
        }
        LinkItem(
            title = stringResource(Res.string.login_manual_create_key),
            description = stringResource(Res.string.login_manual_create_key_description),
            icon = Res.drawable.ic_rounded_key,
            enabled = enabled && viewModel.normalizedGrocyUrl != null,
            index = 2,
            count = 3,
            onClick = onCreateKeyClick,
        )
    }
}

private fun LazyListScope.homeAssistantGroups(
    viewModel: LoginManualViewModel,
    enabled: Boolean,
    onCreateTokenClick: () -> Unit,
) {
    segmentedGroup(
        key = "home_assistant",
        title = Res.string.login_manual_type_home_assistant,
    ) {
        AddressItem(
            state = viewModel.homeAssistantUrl,
            label = stringResource(Res.string.login_manual_ha_url),
            hint = stringResource(Res.string.login_manual_ha_url_hint),
            isInvalid = viewModel.homeAssistantUrl.text.isNotBlank() &&
                viewModel.normalizedHomeAssistantUrl == null,
            scheme = viewModel.scheme,
            onSchemeChange = viewModel::selectScheme,
            enabled = enabled,
            index = 0,
            count = 3,
        )
        SegmentedContainer(index = 1, count = 3) {
            SecretField(
                state = viewModel.homeAssistantToken,
                label = stringResource(Res.string.login_manual_ha_token),
                enabled = enabled,
            )
        }
        LinkItem(
            title = stringResource(Res.string.login_manual_ha_create_token),
            description = stringResource(Res.string.login_manual_ha_create_token_description),
            icon = Res.drawable.ic_rounded_key,
            enabled = enabled && viewModel.normalizedHomeAssistantUrl != null,
            index = 2,
            count = 3,
            onClick = onCreateTokenClick,
        )
    }
    segmentedGroup(key = "addon", title = Res.string.login_manual_group_addon) {
        SegmentedContainer(index = 0, count = 2) {
            RoundedTextField(
                state = viewModel.addonSlug,
                label = { Text(stringResource(Res.string.login_manual_addon_slug)) },
                supportingText = { Text(stringResource(Res.string.login_manual_addon_slug_hint)) },
                enabled = enabled,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SegmentedContainer(index = 1, count = 2) {
            SecretField(
                state = viewModel.apiKey,
                label = stringResource(Res.string.login_manual_api_key),
                enabled = enabled,
                imeAction = ImeAction.Done,
                onDone = viewModel::connect,
            )
        }
    }
}

private fun LazyListScope.homeAssistantUnsupportedGroup(onUseGrocyClick: () -> Unit) {
    segmentedGroup(
        key = "home_assistant_unsupported",
        title = Res.string.login_manual_type_home_assistant,
    ) {
        SegmentedContainer(index = 0, count = 2) {
            Text(
                text = stringResource(Res.string.login_manual_ha_unsupported),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        LinkItem(
            title = stringResource(Res.string.login_manual_use_grocy),
            description = null,
            icon = Res.drawable.ic_rounded_dns,
            enabled = true,
            index = 1,
            count = 2,
            onClick = onUseGrocyClick,
        )
    }
}

/** Scheme buttons and address field in one item, the scheme applies to the address. */
@Composable
private fun AddressItem(
    state: TextFieldState,
    label: String,
    isInvalid: Boolean,
    scheme: ServerUrl.Scheme,
    onSchemeChange: (ServerUrl.Scheme) -> Unit,
    enabled: Boolean,
    index: Int,
    count: Int,
    hint: String? = null,
) {
    val invalidText = stringResource(Res.string.login_manual_url_invalid)
    val supportingText = if (isInvalid) invalidText else hint

    SegmentedContainer(index = index, count = count) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectedButtonGroup(
                options = ServerUrl.Scheme.entries,
                checked = scheme,
                onCheckedChange = onSchemeChange,
                enabled = enabled,
                // Protocol names, not translated
                label = { it.name.uppercase() },
                modifier = Modifier.fillMaxWidth(),
            )
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
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
private fun LinkItem(
    title: String,
    description: String?,
    icon: DrawableResource,
    enabled: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painter = painterResource(icon), contentDescription = null)
            }
        },
        supportingContent = description?.let { { Text(it) } },
        content = { Text(title) },
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
    val haptic = LocalHapticFeedback.current
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Button(
        onClick = {
            haptic.confirm()
            onClick()
        },
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
