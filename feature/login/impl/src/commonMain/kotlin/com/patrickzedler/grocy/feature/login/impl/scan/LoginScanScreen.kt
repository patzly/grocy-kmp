package com.patrickzedler.grocy.feature.login.impl.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.NavigateBackButton
import com.patrickzedler.grocy.core.design.component.SegmentedContainer
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SegmentedTexts
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_edit_note
import com.patrickzedler.grocy.core.resources.login_scan_checking
import com.patrickzedler.grocy.core.resources.login_scan_explanation
import com.patrickzedler.grocy.core.resources.login_scan_manual
import com.patrickzedler.grocy.core.resources.login_scan_manual_description
import com.patrickzedler.grocy.core.resources.login_title
import com.patrickzedler.grocy.core.scanner.BarcodeFormat
import com.patrickzedler.grocy.core.scanner.BarcodeScanner
import com.patrickzedler.grocy.feature.login.impl.LoginError
import com.patrickzedler.grocy.feature.login.impl.LoginErrorItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginScanScreen(
    viewModel: LoginScanViewModel,
    onBackClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScanContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onManualEntryClick = onManualEntryClick,
        modifier = modifier,
        scanner = { shape ->
            BarcodeScanner(
                onBarcodeScanned = viewModel::onBarcodeScanned,
                formats = BarcodeFormat.QrCode,
                shape = shape,
                paused = uiState.isLoggingIn,
            )
        },
    )
}

/** [scanner] is a slot, so previews show the layout without a camera. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginScanContent(
    uiState: LoginScanUiState,
    onBackClick: () -> Unit,
    onManualEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
    scanner: @Composable (shape: Shape) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val error = uiState.error
    val hasStatus = uiState.isLoggingIn || error != null
    val count = if (hasStatus) 3 else 2

    SegmentedListScreen(
        title = stringResource(Res.string.login_title),
        modifier = modifier,
        navigationIcon = { NavigateBackButton(onClick = onBackClick) },
    ) {
        segmentedGroup(key = "scanner") {
            scanner(SegmentedListDefaults.shape(index = 0, count = count))
            SegmentedContainer(index = 1, count = count) {
                SegmentedTexts(supporting = stringResource(Res.string.login_scan_explanation))
            }
            when {
                uiState.isLoggingIn -> CheckingItem(index = 2, count = count)
                error != null -> LoginErrorItem(error = error, index = 2, count = count)
            }
        }
        segmentedGroup(key = "manual") {
            SegmentedListItem(
                onClick = {
                    haptic.confirm()
                    onManualEntryClick()
                },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                colors = SegmentedListDefaults.colors(),
                enabled = !uiState.isLoggingIn,
                modifier = Modifier.fillMaxWidth().clickCursor(enabled = !uiState.isLoggingIn),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = {
                    LeadingTrailingContentWrapper {
                        Icon(
                            painter = painterResource(Res.drawable.ic_rounded_edit_note),
                            contentDescription = null,
                        )
                    }
                },
                supportingContent = {
                    Text(stringResource(Res.string.login_scan_manual_description))
                },
                content = { Text(stringResource(Res.string.login_scan_manual)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CheckingItem(
    index: Int,
    count: Int,
) {
    SegmentedContainer(index = index, count = count) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            LoadingIndicator(modifier = Modifier.size(32.dp))
            Text(
                text = stringResource(Res.string.login_scan_checking),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** Stands in for the camera in previews. */
@Composable
private fun PreviewScanner(shape: Shape) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(shape)
            .background(SegmentedListDefaults.containerColor),
    )
}

@Preview
@Composable
private fun LoginScanPreview() {
    GrocyTheme {
        LoginScanContent(
            uiState = LoginScanUiState(),
            onBackClick = {},
            onManualEntryClick = {},
            scanner = { PreviewScanner(it) },
        )
    }
}

@Preview
@Composable
private fun LoginScanCheckingPreview() {
    GrocyTheme {
        LoginScanContent(
            uiState = LoginScanUiState(isLoggingIn = true),
            onBackClick = {},
            onManualEntryClick = {},
            scanner = { PreviewScanner(it) },
        )
    }
}

@Preview
@Composable
private fun LoginScanErrorPreview() {
    GrocyTheme {
        LoginScanContent(
            uiState = LoginScanUiState(error = LoginError.CalendarCode),
            onBackClick = {},
            onManualEntryClick = {},
            scanner = { PreviewScanner(it) },
        )
    }
}
