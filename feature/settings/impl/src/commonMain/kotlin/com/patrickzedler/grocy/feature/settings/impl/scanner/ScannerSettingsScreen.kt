package com.patrickzedler.grocy.feature.settings.impl.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.tryOpenUri
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_vending_key
import com.patrickzedler.grocy.core.resources.ic_rounded_key
import com.patrickzedler.grocy.core.resources.msg_unlock_description
import com.patrickzedler.grocy.core.resources.scanner_engine
import com.patrickzedler.grocy.core.resources.scanner_engine_ml_kit
import com.patrickzedler.grocy.core.resources.scanner_engine_ml_kit_description
import com.patrickzedler.grocy.core.resources.scanner_engine_ml_kit_locked
import com.patrickzedler.grocy.core.resources.scanner_engine_zxing
import com.patrickzedler.grocy.core.resources.scanner_engine_zxing_description
import com.patrickzedler.grocy.core.resources.scanner_unlock_ml_kit
import com.patrickzedler.grocy.core.resources.settings_scanner
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The scanner's engine as a single choice, saved at once. A locked ML Kit stays visible and
 * disabled, with the way to unlock it below.
 */
@Composable
internal fun ScannerSettingsScreen(
    viewModel: ScannerSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val activeEngine by viewModel.activeEngine.collectAsStateWithLifecycle()
    val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val appVendingKey = stringResource(Res.string.app_vending_key)

    // The user comes back from Google Play with the key installed
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshUnlockState()
        onPauseOrDispose {}
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_scanner),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
    ) {
        // Empty for the moment until the stored engine is read
        activeEngine?.let { active ->
            scannerSettings(
                engines = viewModel.engines,
                activeEngine = active,
                isSelectable = { viewModel.isSelectable(it, unlockState) },
                showsUnlockHint = viewModel.showsUnlockHint(unlockState),
                onEngineClick = viewModel::selectEngine,
                onUnlockClick = { uriHandler.tryOpenUri(appVendingKey) },
            )
        }
    }
}

private fun LazyListScope.scannerSettings(
    engines: List<ScannerEngine>,
    activeEngine: ScannerEngine,
    isSelectable: (ScannerEngine) -> Boolean,
    showsUnlockHint: Boolean,
    onEngineClick: (ScannerEngine) -> Unit,
    onUnlockClick: () -> Unit,
) {
    segmentedGroup(key = "engine", title = Res.string.scanner_engine) {
        ScannerEngineChoices(
            engines = engines,
            activeEngine = activeEngine,
            isSelectable = isSelectable,
            onEngineClick = onEngineClick,
        )
    }
    if (showsUnlockHint) {
        segmentedGroup(key = "unlock") {
            UnlockItem(onClick = onUnlockClick)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScannerEngineChoices(
    engines: List<ScannerEngine>,
    activeEngine: ScannerEngine,
    isSelectable: (ScannerEngine) -> Boolean,
    onEngineClick: (ScannerEngine) -> Unit,
) {
    // Announced together as one choice
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        engines.forEachIndexed { index, engine ->
            ScannerEngineItem(
                engine = engine,
                selected = engine == activeEngine,
                enabled = isSelectable(engine),
                index = index,
                count = engines.size,
                onClick = { onEngineClick(engine) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScannerEngineItem(
    engine: ScannerEngine,
    selected: Boolean,
    enabled: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedListItem(
        selected = selected,
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(enabled = enabled),
        enabled = enabled,
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                // The item handles the click, the radio button only shows the choice
                RadioButton(selected = selected, enabled = enabled, onClick = null)
            }
        },
        supportingContent = { Text(stringResource(engine.description(enabled))) },
        content = { Text(stringResource(engine.title)) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UnlockItem(onClick: () -> Unit) {
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
                    painter = painterResource(Res.drawable.ic_rounded_key),
                    contentDescription = null,
                )
            }
        },
        supportingContent = { Text(stringResource(Res.string.msg_unlock_description)) },
        content = { Text(stringResource(Res.string.scanner_unlock_ml_kit)) },
    )
}

private val ScannerEngine.title
    get() = when (this) {
        ScannerEngine.ZXING -> Res.string.scanner_engine_zxing
        ScannerEngine.ML_KIT -> Res.string.scanner_engine_ml_kit
    }

private fun ScannerEngine.description(isSelectable: Boolean) = when (this) {
    ScannerEngine.ZXING -> Res.string.scanner_engine_zxing_description
    ScannerEngine.ML_KIT -> if (isSelectable) {
        Res.string.scanner_engine_ml_kit_description
    } else {
        Res.string.scanner_engine_ml_kit_locked
    }
}

@Preview
@Composable
private fun ScannerSettingsLockedPreview() {
    ScannerSettingsPreview(isMlKitUnlocked = false)
}

@Preview
@Composable
private fun ScannerSettingsUnlockedPreview() {
    ScannerSettingsPreview(isMlKitUnlocked = true)
}

@Composable
private fun ScannerSettingsPreview(isMlKitUnlocked: Boolean) {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_scanner),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            scannerSettings(
                engines = ScannerEngine.entries,
                activeEngine = ScannerEngine.ZXING,
                isSelectable = { it == ScannerEngine.ZXING || isMlKitUnlocked },
                showsUnlockHint = !isMlKitUnlocked,
                onEngineClick = {},
                onUnlockClick = {},
            )
        }
    }
}
