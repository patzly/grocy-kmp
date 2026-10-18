package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Keeps the leading or trailing content of a SegmentedListItem (icon, switch, picture) from
 * sticking to the top edge when the item switches to top alignment for multiple lines.
 */
@Composable
fun LeadingTrailingContentWrapper(
    content: @Composable () -> Unit,
) {
    Box(Modifier.padding(vertical = 11.dp)) {
        content()
    }
}
