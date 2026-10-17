package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.AppBarIconButton
import com.patrickzedler.grocy.core.design.component.LeadingContentWrapper
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_name
import com.patrickzedler.grocy.core.resources.ic_rounded_database
import com.patrickzedler.grocy.core.resources.ic_rounded_info
import com.patrickzedler.grocy.core.resources.ic_rounded_settings
import com.patrickzedler.grocy.core.resources.masterdata_title
import com.patrickzedler.grocy.core.resources.title_about
import com.patrickzedler.grocy.core.resources.title_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The first screen after login; grows with the app's sections, master data so far. */
@Composable
internal fun StartScreen(
    onMasterDataClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListScreen(
        title = stringResource(Res.string.app_name),
        modifier = modifier,
        actions = {
            AppBarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_settings),
                contentDescription = stringResource(Res.string.title_settings),
                onClick = onSettingsClick,
            )
            AppBarIconButton(
                painter = painterResource(Res.drawable.ic_rounded_info),
                contentDescription = stringResource(Res.string.title_about),
                onClick = onAboutClick,
            )
        },
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingContentWrapper {
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
        StartScreen(onMasterDataClick = {}, onSettingsClick = {}, onAboutClick = {})
    }
}
