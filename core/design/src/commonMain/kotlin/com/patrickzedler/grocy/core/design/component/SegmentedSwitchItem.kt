package com.patrickzedler.grocy.core.design.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState

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

    // A clickable item, not the checkable one: that one tints the whole item when checked
    SegmentedListItem(
        onClick = {
            haptic.performHapticFeedback(
                if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
            )
            onCheckedChange(!checked)
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
        enabled = enabled,
        colors = SegmentedListDefaults.colors(),
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        },
    ) {
        Text(headline)
    }
}
