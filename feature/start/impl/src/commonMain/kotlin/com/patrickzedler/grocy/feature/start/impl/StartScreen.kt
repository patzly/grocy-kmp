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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The first screen after login; grows with the app's sections, master data so far. */
@Composable
internal fun StartScreen(
    viewModel: StartViewModel,
    onMasterDataClick: () -> Unit,
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
        onMasterDataClick = onMasterDataClick,
        menuItems = appMenu.items,
        modifier = modifier,
    )
}

@Composable
private fun StartContent(
    isRefreshing: Boolean,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onMasterDataClick: () -> Unit,
    menuItems: List<AppBarMenuItem>,
    modifier: Modifier = Modifier,
) {
    SegmentedListScreen(
        title = stringResource(Res.string.app_name),
        modifier = modifier,
        menuItems = menuItems,
        snackbarHostState = snackbarHostState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        segmentedGroup(key = "sections") {
            SectionItem(
                title = stringResource(Res.string.masterdata_title),
                onClick = onMasterDataClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionItem(
    title: String,
    onClick: () -> Unit,
) {
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
                Icon(painterResource(Res.drawable.ic_rounded_database), contentDescription = null)
            }
        },
    ) {
        Text(title)
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
            onMasterDataClick = {},
            menuItems = emptyList(),
        )
    }
}
