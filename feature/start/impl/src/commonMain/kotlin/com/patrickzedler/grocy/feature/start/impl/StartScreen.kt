package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_name
import com.patrickzedler.grocy.core.resources.ic_rounded_database
import com.patrickzedler.grocy.core.resources.masterdata_title
import com.patrickzedler.grocy.core.ui.AppMenuDialogs
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.core.ui.rememberAppMenu
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The sections of the start screen, in their order. */
internal enum class StartSection(val title: StringResource, val icon: DrawableResource) {
    MASTER_DATA(Res.string.masterdata_title, Res.drawable.ic_rounded_database),
}

/**
 * The first screen after login; grows with the app's sections, master data so far. Beside the
 * sections' pages ([isBesideDetail]) it is the start [ScreenPane.List] of the layout and its
 * sections a single choice marking the [shownSection].
 */
@Composable
internal fun StartScreen(
    viewModel: StartViewModel,
    isBesideDetail: Boolean,
    shownSection: StartSection?,
    onSectionClick: (StartSection) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appMenu = rememberAppMenu(onSettingsClick = onSettingsClick, onAboutClick = onAboutClick)
    AppMenuDialogs(appMenu)

    RefreshErrorEffect(
        error = refresh.error,
        hasData = true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )

    StartContent(
        isRefreshing = refresh.isRefreshing,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        isBesideDetail = isBesideDetail,
        shownSection = shownSection,
        onSectionClick = onSectionClick,
        menuItems = appMenu.items,
        modifier = modifier,
    )
}

@Composable
private fun StartContent(
    isRefreshing: Boolean,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    isBesideDetail: Boolean,
    shownSection: StartSection?,
    onSectionClick: (StartSection) -> Unit,
    menuItems: List<AppBarMenuItem>,
    modifier: Modifier = Modifier,
) {
    SegmentedListScreen(
        title = stringResource(Res.string.app_name),
        modifier = modifier,
        menuItems = menuItems,
        pane = ScreenPane.List.takeIf { isBesideDetail },
        snackbarHostState = snackbarHostState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        segmentedGroup(key = "sections") {
            val sections = StartSection.entries
            sections.forEachIndexed { index, section ->
                SectionItem(
                    section = section,
                    index = index,
                    count = sections.size,
                    onClick = { onSectionClick(section) },
                    selected = if (isBesideDetail) shownSection == section else null,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionItem(
    section: StartSection,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    selected: Boolean?,
) {
    val haptic = LocalHapticFeedback.current
    val click = {
        haptic.confirm()
        onClick()
    }
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors = SegmentedListDefaults.colors()
    val modifier = Modifier.fillMaxWidth().clickCursor()
    val leading = @Composable {
        LeadingTrailingContentWrapper {
            Icon(painterResource(section.icon), contentDescription = null)
        }
    }
    val title = @Composable { Text(stringResource(section.title)) }

    // Beside a page a single choice marking it, without page a plain item (no radio semantics)
    if (selected != null) {
        SegmentedListItem(
            selected = selected,
            onClick = click,
            shapes = shapes,
            colors = colors,
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leading,
            content = title,
        )
    } else {
        SegmentedListItem(
            onClick = click,
            shapes = shapes,
            colors = colors,
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leading,
            content = title,
        )
    }
}

@Preview
@Composable
private fun StartScreenPreview() {
    GrocyTheme {
        StartContent(
            isRefreshing = false,
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            isBesideDetail = false,
            shownSection = null,
            onSectionClick = {},
            menuItems = emptyList(),
        )
    }
}
