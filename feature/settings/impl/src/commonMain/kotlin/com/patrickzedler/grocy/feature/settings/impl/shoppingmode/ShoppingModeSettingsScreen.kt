package com.patrickzedler.grocy.feature.settings.impl.shoppingmode

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.component.NumberStepper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingModeSettings
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.shopping_mode_auto_sync
import com.patrickzedler.grocy.core.resources.shopping_mode_auto_sync_hint
import com.patrickzedler.grocy.core.resources.shopping_mode_done_last
import com.patrickzedler.grocy.core.resources.shopping_mode_keep_screen_on
import com.patrickzedler.grocy.core.resources.shopping_mode_keep_screen_on_hint
import com.patrickzedler.grocy.core.resources.shopping_mode_settings
import com.patrickzedler.grocy.core.resources.shopping_mode_sync_interval
import com.patrickzedler.grocy.feature.settings.impl.SettingNumberItem
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import org.jetbrains.compose.resources.stringResource

/**
 * The shopping mode's settings: auto sync with its interval in seconds, then keeping the screen
 * on and the place of done items. Each change is saved at once.
 */
@Composable
internal fun ShoppingModeSettingsScreen(
    viewModel: ShoppingModeSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScaffold(
        title = stringResource(Res.string.shopping_mode_settings),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
    ) {
        // Empty for the moment until the stored settings are read
        settings?.let { shoppingModeSettings(viewModel, it) }
    }
}

private fun LazyListScope.shoppingModeSettings(
    viewModel: ShoppingModeSettingsViewModel,
    settings: ShoppingModeSettings,
) {
    segmentedGroup(key = "sync") {
        SegmentedSwitchItem(
            headline = stringResource(Res.string.shopping_mode_auto_sync),
            supporting = stringResource(Res.string.shopping_mode_auto_sync_hint),
            checked = settings.autoSync,
            onCheckedChange = viewModel::setAutoSync,
            index = 0,
            count = 2,
        )
        SettingNumberItem(
            label = stringResource(Res.string.shopping_mode_sync_interval),
            state = viewModel.syncInterval,
            stepper = NumberStepper(min = MinSyncInterval),
            error = viewModel.syncIntervalError,
            index = 1,
            count = 2,
            enabled = settings.autoSync,
        )
    }
    segmentedGroup(key = "display") {
        SegmentedSwitchItem(
            headline = stringResource(Res.string.shopping_mode_keep_screen_on),
            supporting = stringResource(Res.string.shopping_mode_keep_screen_on_hint),
            checked = settings.keepScreenOn,
            onCheckedChange = viewModel::setKeepScreenOn,
            index = 0,
            count = 2,
        )
        SegmentedSwitchItem(
            headline = stringResource(Res.string.shopping_mode_done_last),
            checked = settings.doneItemsLast,
            onCheckedChange = viewModel::setDoneItemsLast,
            index = 1,
            count = 2,
        )
    }
}

// The stepper counts in decimals
private val MinSyncInterval = ShoppingModeSettings.MIN_SYNC_INTERVAL_SECONDS.toDouble()
