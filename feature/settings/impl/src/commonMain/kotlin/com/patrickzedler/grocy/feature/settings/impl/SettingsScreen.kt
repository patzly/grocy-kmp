package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.patrickzedler.grocy.core.data.log.LogSource
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_barcode_scanner
import com.patrickzedler.grocy.core.resources.ic_rounded_build
import com.patrickzedler.grocy.core.resources.ic_rounded_design_services
import com.patrickzedler.grocy.core.resources.ic_rounded_host
import com.patrickzedler.grocy.core.resources.ic_rounded_network_manage
import com.patrickzedler.grocy.core.resources.ic_rounded_notifications
import com.patrickzedler.grocy.core.resources.ic_rounded_reset_settings
import com.patrickzedler.grocy.core.resources.ic_rounded_settings_backup_restore
import com.patrickzedler.grocy.core.resources.ic_rounded_tune
import com.patrickzedler.grocy.core.resources.settings_backup
import com.patrickzedler.grocy.core.resources.settings_backup_description
import com.patrickzedler.grocy.core.resources.settings_behavior
import com.patrickzedler.grocy.core.resources.settings_general
import com.patrickzedler.grocy.core.resources.settings_grocy
import com.patrickzedler.grocy.core.resources.settings_network
import com.patrickzedler.grocy.core.resources.settings_notifications
import com.patrickzedler.grocy.core.resources.settings_reset
import com.patrickzedler.grocy.core.resources.settings_reset_description
import com.patrickzedler.grocy.core.resources.settings_scanner
import com.patrickzedler.grocy.core.resources.settings_server
import com.patrickzedler.grocy.core.resources.title_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The pages of the settings, in the order of the overview. */
internal enum class SettingsCategory(
    val title: StringResource,
    val icon: DrawableResource,
) {
    GENERAL(
        title = Res.string.settings_general,
        icon = Res.drawable.ic_rounded_tune,
    ),
    SERVER(
        title = Res.string.settings_server,
        icon = Res.drawable.ic_rounded_host,
    ),
    GROCY(
        title = Res.string.settings_grocy,
        icon = Res.drawable.ic_rounded_build,
    ),
    NETWORK(
        title = Res.string.settings_network,
        icon = Res.drawable.ic_rounded_network_manage,
    ),
    BEHAVIOR(
        title = Res.string.settings_behavior,
        icon = Res.drawable.ic_rounded_design_services,
    ),
    SCANNER(
        title = Res.string.settings_scanner,
        icon = Res.drawable.ic_rounded_barcode_scanner,
    ),
    NOTIFICATIONS(
        title = Res.string.settings_notifications,
        icon = Res.drawable.ic_rounded_notifications,
    ),
}

/**
 * The categories of the settings. Beside the pages ([isBesideDetail]) they are a single choice
 * marking the [shownCategory] and the list is the start [ScreenPane.List] of the layout.
 */
@Composable
internal fun SettingsScreen(
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    isBesideDetail: Boolean,
    shownCategory: SettingsCategory?,
    onBackClick: () -> Unit,
    onCategoryClick: (SettingsCategory) -> Unit,
    onBackupClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(
        title = stringResource(Res.string.title_settings),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = ScreenPane.List.takeIf { isBesideDetail },
    ) {
        settingsOverview(
            isBesideDetail = isBesideDetail,
            shownCategory = shownCategory,
            onCategoryClick = onCategoryClick,
            onBackupClick = onBackupClick,
            onResetClick = onResetClick,
        )
    }
}

private fun LazyListScope.settingsOverview(
    isBesideDetail: Boolean,
    shownCategory: SettingsCategory?,
    onCategoryClick: (SettingsCategory) -> Unit,
    onBackupClick: () -> Unit,
    onResetClick: () -> Unit,
) {
    segmentedGroup(key = "categories") {
        val colors = SegmentedListDefaults.colors()
        val categories = SettingsCategory.entries
        categories.forEachIndexed { index, category ->
            SettingsItem(
                title = stringResource(category.title),
                description = null,
                icon = category.icon,
                index = index,
                count = categories.size,
                colors = colors,
                onClick = { onCategoryClick(category) },
                selected = if (isBesideDetail) shownCategory == category else null,
            )
        }
    }

    segmentedGroup(key = "data") {
        val colors = SegmentedListDefaults.colors()
        SettingsItem(
            title = stringResource(Res.string.settings_backup),
            description = stringResource(Res.string.settings_backup_description),
            icon = Res.drawable.ic_rounded_settings_backup_restore,
            index = 0,
            count = 2,
            colors = colors,
            onClick = onBackupClick,
        )
        SettingsItem(
            title = stringResource(Res.string.settings_reset),
            description = stringResource(Res.string.settings_reset_description),
            icon = Res.drawable.ic_rounded_reset_settings,
            index = 1,
            count = 2,
            colors = colors,
            onClick = onResetClick,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsItem(
    title: String,
    description: String?,
    icon: DrawableResource,
    index: Int,
    count: Int,
    colors: ListItemColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
) {
    val haptic = LocalHapticFeedback.current
    val click = {
        haptic.confirm()
        onClick()
    }
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val leading = @Composable {
        LeadingTrailingContentWrapper {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
    }
    val supporting: (@Composable () -> Unit)? = description?.let { { Text(it) } }

    // Beside a page a single choice marking it, without page a plain item (no radio semantics)
    if (selected != null) {
        SegmentedListItem(
            selected = selected,
            onClick = click,
            shapes = shapes,
            colors = colors,
            modifier = modifier.fillMaxWidth().clickCursor(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leading,
            supportingContent = supporting,
            content = { Text(title) },
        )
    } else {
        SegmentedListItem(
            onClick = click,
            shapes = shapes,
            colors = colors,
            modifier = modifier.fillMaxWidth().clickCursor(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leading,
            supportingContent = supporting,
            content = { Text(title) },
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    GrocyTheme {
        SettingsScaffoldContent(
            title = stringResource(Res.string.title_settings),
            menuActions = SettingsMenuActions(
                onAboutClick = {},
                onLogClick = {},
                logSource = LogSource.LOGCAT,
            ),
            onBackClick = {},
            onHelpClick = {},
            onFeedbackClick = {},
        ) {
            settingsOverview(
                isBesideDetail = false,
                shownCategory = null,
                onCategoryClick = {},
                onBackupClick = {},
                onResetClick = {},
            )
        }
    }
}
