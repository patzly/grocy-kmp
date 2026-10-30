@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.objects

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm

/**
 * An item of a list whose objects open as details. Beside the details ([selected] not null) it
 * is a single choice that marks the object shown there, otherwise a plain clickable item.
 */
@Composable
internal fun ListDetailItem(
    selected: Boolean?,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val click = {
        haptic.confirm()
        onClick()
    }
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    if (selected != null) {
        SegmentedListItem(
            selected = selected,
            onClick = click,
            shapes = shapes,
            colors = SegmentedListDefaults.colors(),
            modifier = modifier.clickCursor(),
            leadingContent = leadingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = content,
        )
    } else {
        SegmentedListItem(
            onClick = click,
            shapes = shapes,
            colors = SegmentedListDefaults.colors(),
            modifier = modifier.clickCursor(),
            leadingContent = leadingContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            trailingContent = trailingContent,
            content = content,
        )
    }
}
