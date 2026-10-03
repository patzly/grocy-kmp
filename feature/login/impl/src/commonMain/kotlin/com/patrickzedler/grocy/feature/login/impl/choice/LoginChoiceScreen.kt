package com.patrickzedler.grocy.feature.login.impl.choice

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.VerticalButtonGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.login_choice_demo_server
import com.patrickzedler.grocy.core.resources.login_choice_own_server
import com.patrickzedler.grocy.core.resources.login_choice_subtitle
import com.patrickzedler.grocy.core.resources.login_choice_title
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginChoiceScreen(
    viewModel: LoginChoiceViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginChoiceContent(
        uiState = uiState,
        onOwnServerClick = {},
        onDemoServerClick = viewModel::loginWithDemoServer,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginChoiceContent(
    uiState: LoginChoiceUiState,
    onOwnServerClick: () -> Unit,
    onDemoServerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonHeight = ButtonDefaults.LargeContainerHeight
    val buttonContentPadding = ButtonDefaults.contentPaddingFor(buttonHeight)
    val layoutDirection = LocalLayoutDirection.current
    val buttonContentPaddingHorizontal = PaddingValues(
        start = buttonContentPadding.calculateStartPadding(layoutDirection),
        top = 0.dp,
        end = buttonContentPadding.calculateEndPadding(layoutDirection),
        bottom = 0.dp,
    )
    val ownServerInteractionSource = remember { MutableInteractionSource() }
    val demoServerInteractionSource = remember { MutableInteractionSource() }

    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                // Keeps the buttons at a readable width on large browser windows
                modifier = Modifier.widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.login_choice_title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.login_choice_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(48.dp))

                VerticalButtonGroup(
                    overflowIndicator = {},
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = onOwnServerClick,
                                shapes = ButtonDefaults.shapesFor(buttonHeight),
                                contentPadding = buttonContentPaddingHorizontal,
                                // Enabled with the manual and QR code login screens
                                enabled = false,
                                interactionSource = ownServerInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = buttonHeight)
                                    .animateHeight(ownServerInteractionSource),
                            ) {
                                Text(
                                    text = stringResource(Res.string.login_choice_own_server),
                                    style = ButtonDefaults.textStyleFor(buttonHeight),
                                )
                            }
                        },
                        menuContent = {},
                    )

                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = onDemoServerClick,
                                shapes = ButtonDefaults.shapesFor(buttonHeight),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    disabledContentColor =
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledContainerColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                ),
                                contentPadding = buttonContentPaddingHorizontal,
                                enabled = !uiState.isLoggingIn,
                                interactionSource = demoServerInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = buttonHeight)
                                    .animateHeight(demoServerInteractionSource),
                            ) {
                                if (uiState.isLoggingIn) {
                                    LoadingIndicator(modifier = Modifier.size(48.dp))
                                } else {
                                    Text(
                                        text = stringResource(Res.string.login_choice_demo_server),
                                        style = ButtonDefaults.textStyleFor(buttonHeight),
                                    )
                                }
                            }
                        },
                        menuContent = {},
                    )
                }

                uiState.error?.let { error ->
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = error.message(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        // Screen readers announce the error as soon as it appears
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoginChoicePreview() {
    GrocyTheme {
        LoginChoiceContent(
            uiState = LoginChoiceUiState(error = LoginError.Unreachable),
            onOwnServerClick = {},
            onDemoServerClick = {},
        )
    }
}
