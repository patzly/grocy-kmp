package com.patrickzedler.grocy.feature.login.impl.choice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.InsetLazyColumn
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_dns
import com.patrickzedler.grocy.core.resources.ic_rounded_home
import com.patrickzedler.grocy.core.resources.ic_rounded_science
import com.patrickzedler.grocy.core.resources.login_choice_demo_server
import com.patrickzedler.grocy.core.resources.login_choice_demo_server_description
import com.patrickzedler.grocy.core.resources.login_choice_own_server
import com.patrickzedler.grocy.core.resources.login_choice_own_server_description
import com.patrickzedler.grocy.core.resources.login_choice_subtitle
import com.patrickzedler.grocy.core.resources.login_choice_title
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.LoginErrorItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginChoiceScreen(
    viewModel: LoginChoiceViewModel,
    onOwnServerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginChoiceContent(
        uiState = uiState,
        onOwnServerClick = onOwnServerClick,
        onDemoServerClick = viewModel::loginWithDemoServer,
        modifier = modifier,
    )
}

@Composable
private fun LoginChoiceContent(
    uiState: LoginChoiceUiState,
    onOwnServerClick: () -> Unit,
    onDemoServerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = uiState.error
    val count = if (error != null) 3 else 2

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        InsetLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + SegmentedListDefaults.GroupSpacing,
                bottom = padding.calculateBottomPadding() + SegmentedListDefaults.GroupSpacing,
            ),
            // Centered on large screens, scrollable on small ones and in landscape
            verticalArrangement = Arrangement.spacedBy(
                SegmentedListDefaults.GroupSpacing,
                Alignment.CenterVertically,
            ),
        ) {
            insetItem(key = "header") {
                LoginChoiceHeader()
            }
            segmentedGroup(key = "choices") {
                ChoiceItem(
                    title = stringResource(Res.string.login_choice_own_server),
                    description = stringResource(Res.string.login_choice_own_server_description),
                    icon = Res.drawable.ic_rounded_dns,
                    index = 0,
                    count = count,
                    enabled = !uiState.isLoggingIn,
                    onClick = onOwnServerClick,
                )
                ChoiceItem(
                    title = stringResource(Res.string.login_choice_demo_server),
                    description = stringResource(Res.string.login_choice_demo_server_description),
                    icon = Res.drawable.ic_rounded_science,
                    index = 1,
                    count = count,
                    enabled = !uiState.isLoggingIn,
                    isLoading = uiState.isLoggingIn,
                    onClick = onDemoServerClick,
                )
                if (error != null) {
                    LoginErrorItem(error = error, index = 2, count = count)
                }
            }
        }
    }
}

/** Logo, welcome and subtitle, the only screen part placed directly on the background. */
@Composable
private fun LoginChoiceHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Placeholder until the app logo exists
        Icon(
            painter = painterResource(Res.drawable.ic_rounded_home),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(LOGO_SIZE),
        )
        Text(
            text = stringResource(Res.string.login_choice_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 24.dp)
                .semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.login_choice_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private val LOGO_SIZE = 100.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceItem(
    title: String,
    description: String,
    icon: DrawableResource,
    index: Int,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
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
        modifier = modifier.fillMaxWidth().clickCursor(enabled = enabled),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painter = painterResource(icon), contentDescription = null)
            }
        },
        trailingContent = if (isLoading) {
            {
                LeadingTrailingContentWrapper {
                    LoadingIndicator(modifier = Modifier.size(40.dp))
                }
            }
        } else {
            null
        },
        supportingContent = { Text(description) },
        content = { Text(title) },
    )
}

@Preview
@Composable
private fun LoginChoicePreview() {
    GrocyTheme {
        LoginChoiceContent(
            uiState = LoginChoiceUiState(),
            onOwnServerClick = {},
            onDemoServerClick = {},
        )
    }
}

@Preview
@Composable
private fun LoginChoiceErrorPreview() {
    GrocyTheme {
        LoginChoiceContent(
            uiState = LoginChoiceUiState(error = LoginError.Unreachable),
            onOwnServerClick = {},
            onDemoServerClick = {},
        )
    }
}
