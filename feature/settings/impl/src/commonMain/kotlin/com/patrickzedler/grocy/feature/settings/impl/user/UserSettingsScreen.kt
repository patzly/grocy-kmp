package com.patrickzedler.grocy.feature.settings.impl.user

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_box
import com.patrickzedler.grocy.core.resources.ic_rounded_cloud_sync
import com.patrickzedler.grocy.core.resources.settings_stock
import com.patrickzedler.grocy.core.resources.settings_user
import com.patrickzedler.grocy.core.resources.settings_user_synced
import com.patrickzedler.grocy.core.resources.settings_user_synced_hint
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffoldContent
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The server's feature flags, which hide the pages of switched off features like Grocy. */
internal class UserSettingsViewModel(val serverConfig: StateFlow<ServerConfig?>) : ViewModel()

/**
 * Grocy's settings of the user, a page per area like the user menu of its web frontend; above
 * them a note that the server keeps them for the user.
 */
@Composable
internal fun UserSettingsScreen(
    viewModel: UserSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    onStockSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val config by viewModel.serverConfig.collectAsStateWithLifecycle()

    SettingsScaffold(
        title = stringResource(Res.string.settings_user),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
    ) {
        // Shown while the configuration is unknown, Grocy's default has the stock on
        userSettings(
            showsStock = config?.stock != false,
            onStockSettingsClick = onStockSettingsClick,
        )
    }
}

private fun LazyListScope.userSettings(
    showsStock: Boolean,
    onStockSettingsClick: () -> Unit,
) {
    segmentedGroup(key = "synced") {
        SyncedItem()
    }
    if (showsStock) {
        segmentedGroup(key = "pages") {
            PageItem(
                title = stringResource(Res.string.settings_stock),
                icon = Res.drawable.ic_rounded_box,
                index = 0,
                count = 1,
                onClick = onStockSettingsClick,
            )
        }
    }
}

/** Not a setting: says that Grocy keeps these settings for the user, not this device. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncedItem() {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { PageIcon(Res.drawable.ic_rounded_cloud_sync) },
        supportingContent = { Text(stringResource(Res.string.settings_user_synced_hint)) },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = SegmentedListDefaults.colors(),
        content = { Text(stringResource(Res.string.settings_user_synced)) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageItem(
    title: String,
    icon: DrawableResource,
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
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { PageIcon(icon) },
        content = { Text(title) },
    )
}

@Composable
private fun PageIcon(icon: DrawableResource) {
    LeadingTrailingContentWrapper {
        Icon(painter = painterResource(icon), contentDescription = null)
    }
}

@Preview
@Composable
private fun UserSettingsPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.settings_user),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            userSettings(showsStock = true, onStockSettingsClick = {})
        }
    }
}
