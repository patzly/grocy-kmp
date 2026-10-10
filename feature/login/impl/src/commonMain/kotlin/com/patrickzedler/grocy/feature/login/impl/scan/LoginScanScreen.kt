package com.patrickzedler.grocy.feature.login.impl.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.FormattedText
import com.patrickzedler.grocy.core.design.component.InsetLazyColumn
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.insetItem
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.login_scan_explanation
import com.patrickzedler.grocy.core.resources.login_scan_manual
import com.patrickzedler.grocy.core.resources.login_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoginScanScreen(
    onBackClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.login_title)) },
                navigationIcon = { NavigateBackButton(onClick = onBackClick) },
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FormattedText(
                        text = stringResource(Res.string.login_scan_explanation),
                        // FormattedText adds the bottom distance after its last paragraph itself
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    )
                }
            }
            insetItem {
                ScannerArea()
            }
            insetItem {
                val buttonHeight = ButtonDefaults.MediumContainerHeight

                FilledTonalButton(
                    onClick = onManualEntryClick,
                    shapes = ButtonDefaults.shapesFor(buttonHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
                    modifier = modifier
                        .fillMaxWidth()
                        .heightIn(min = buttonHeight),
                ) {
                    Text(stringResource(Res.string.login_scan_manual))
                }
            }
        }
    }
}

/** Placeholder for the camera preview, which arrives with the scanner. */
@Composable
private fun ScannerArea(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Preview
@Composable
private fun LoginScanPreview() {
    GrocyTheme {
        LoginScanScreen(
            onBackClick = {},
            onManualEntryClick = {},
        )
    }
}
