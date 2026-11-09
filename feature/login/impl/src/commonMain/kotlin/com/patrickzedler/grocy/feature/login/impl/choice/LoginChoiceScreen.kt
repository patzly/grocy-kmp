package com.patrickzedler.grocy.feature.login.impl.choice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.patrickzedler.grocy.core.design.component.GrocyLogo
import com.patrickzedler.grocy.core.design.component.InsetLazyColumn
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.verticalScrollbar
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_dns
import com.patrickzedler.grocy.core.resources.ic_rounded_history
import com.patrickzedler.grocy.core.resources.ic_rounded_science
import com.patrickzedler.grocy.core.resources.login_choice_demo_server
import com.patrickzedler.grocy.core.resources.login_choice_demo_server_description
import com.patrickzedler.grocy.core.resources.login_choice_own_server
import com.patrickzedler.grocy.core.resources.login_choice_own_server_description
import com.patrickzedler.grocy.core.resources.login_choice_subtitle
import com.patrickzedler.grocy.core.resources.login_choice_title
import com.patrickzedler.grocy.core.resources.sessions_saved
import com.patrickzedler.grocy.core.resources.sessions_saved_description
import com.patrickzedler.grocy.core.ui.sessions.SessionsDialog
import com.patrickzedler.grocy.core.ui.sessions.SessionsViewModel
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.LoginErrorItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginChoiceScreen(
    viewModel: LoginChoiceViewModel,
    sessionsViewModel: SessionsViewModel,
    onOwnServerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessions by sessionsViewModel.sessions.collectAsStateWithLifecycle()
    var isSessionsDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isSessionsDialogOpen) {
        SessionsDialog(
            sessions = sessions,
            current = null,
            switching = sessionsViewModel.switching,
            error = sessionsViewModel.error,
            // Saving switches the app to the start screen, see GrocyApp
            onSessionClick = sessionsViewModel::switchTo,
            onRemoveSession = sessionsViewModel::remove,
            onDismissRequest = {
                isSessionsDialogOpen = false
                sessionsViewModel.onErrorShown()
            },
        )
    }

    LoginChoiceContent(
        uiState = uiState,
        hasSessions = sessions.isNotEmpty(),
        onOwnServerClick = onOwnServerClick,
        onDemoServerClick = viewModel::loginWithDemoServer,
        onSessionsClick = { isSessionsDialogOpen = true },
        modifier = modifier,
    )
}

@Composable
private fun LoginChoiceContent(
    uiState: LoginChoiceUiState,
    hasSessions: Boolean,
    onOwnServerClick: () -> Unit,
    onDemoServerClick: () -> Unit,
    onSessionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = uiState.error
    val sessionsCount = if (hasSessions) 1 else 0
    val count = 2 + sessionsCount + if (error != null) 1 else 0

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        val listState = rememberLazyListState()
        InsetLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            state = listState,
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
                if (hasSessions) {
                    ChoiceItem(
                        title = stringResource(Res.string.sessions_saved),
                        description = stringResource(Res.string.sessions_saved_description),
                        icon = Res.drawable.ic_rounded_history,
                        index = 0,
                        count = count,
                        enabled = !uiState.isLoggingIn,
                        onClick = onSessionsClick,
                    )
                }
                ChoiceItem(
                    title = stringResource(Res.string.login_choice_own_server),
                    description = stringResource(Res.string.login_choice_own_server_description),
                    icon = Res.drawable.ic_rounded_dns,
                    index = sessionsCount,
                    count = count,
                    enabled = !uiState.isLoggingIn,
                    onClick = onOwnServerClick,
                )
                ChoiceItem(
                    title = stringResource(Res.string.login_choice_demo_server),
                    description = stringResource(Res.string.login_choice_demo_server_description),
                    icon = Res.drawable.ic_rounded_science,
                    index = sessionsCount + 1,
                    count = count,
                    enabled = !uiState.isLoggingIn,
                    isLoading = uiState.isLoggingIn,
                    onClick = onDemoServerClick,
                )
                if (error != null) {
                    LoginErrorItem(error = error, index = sessionsCount + 2, count = count)
                }
            }
        }
        // Between the system bars, where the list shows its content
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScrollbar(listState),
        )
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
        GrocyLogo(size = LOGO_SIZE)
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
            hasSessions = true,
            onOwnServerClick = {},
            onDemoServerClick = {},
            onSessionsClick = {},
        )
    }
}

@Preview
@Composable
private fun LoginChoiceErrorPreview() {
    GrocyTheme {
        LoginChoiceContent(
            uiState = LoginChoiceUiState(error = LoginError.Unreachable),
            hasSessions = false,
            onOwnServerClick = {},
            onDemoServerClick = {},
            onSessionsClick = {},
        )
    }
}
