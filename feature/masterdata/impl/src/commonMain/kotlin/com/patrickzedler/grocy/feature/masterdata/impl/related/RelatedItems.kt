@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.related

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SupportingLines
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm

/**
 * An object that belongs to the form's object, e.g. a barcode of a product: a tap edits it,
 * its menu holds [menuItems] such as delete.
 */
@Composable
internal fun RelatedObjectItem(
    supportingLines: List<String>,
    menuItems: List<AppBarMenuItem>,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        modifier = Modifier.fillMaxWidth().clickCursor(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = if (supportingLines.isEmpty()) {
            null
        } else {
            { SupportingLines(supportingLines) }
        },
        trailingContent = { ListItemOverflowMenu(groups = listOf(menuItems)) },
        content = content,
    )
}

/** An action at the end of a group, e.g. "Add" below the barcodes. */
@Composable
internal fun GroupActionItem(
    text: String,
    icon: Painter,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().clickCursor(enabled = enabled),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper { Icon(icon, contentDescription = null) }
        },
        supportingContent = supporting?.let { { Text(it) } },
        content = { Text(text) },
    )
}
