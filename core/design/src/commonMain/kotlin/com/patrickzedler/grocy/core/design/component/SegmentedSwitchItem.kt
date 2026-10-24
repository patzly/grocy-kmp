package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * A segmented item that toggles a setting with a switch at its end; the whole item is the
 * touch target.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedSwitchItem(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current

    // The checkable item takes the selected shape and color of the segmented defaults while on
    SegmentedListItem(
        checked = checked,
        onCheckedChange = { value ->
            // Not ToggleOn/ToggleOff: many devices barely vibrate for on and not at all for off
            haptic.confirm()
            onCheckedChange(value)
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        // Announced as switch, not as the checkbox of the checkable item
        modifier = modifier
            .semantics { role = Role.Switch }
            .clickCursor(enabled = enabled),
        enabled = enabled,
        colors = SegmentedListDefaults.colors(),
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = {
            LeadingTrailingContentWrapper {
                Switch(checked = checked, onCheckedChange = null, enabled = enabled)
            }
        },
    ) {
        Text(headline)
    }
}
