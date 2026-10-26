package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material's scrollbar for the vertical scrolling of [state], drawn over the end edge of this
 * node: no visible track, only a thin round thumb in Material's outline at 70 %, shown while
 * scrolling.
 * Apply it before the scrolling modifier, so it spans the viewport and not the content.
 * The thumb keeps the same distance from the top and bottom as from the end edge; rounded
 * corners of a clipping parent (panes, menus) cover it there. [endInset] moves it away from this
 * node's end edge, a negative one into the padding beside this node.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: ScrollableState,
    endInset: Dp = ScrollbarInset,
): Modifier {
    val indicatorState = state.scrollIndicatorState ?: return this
    return nonInteractiveScrollbar(
        state = indicatorState,
        orientation = Orientation.Vertical,
        thickness = ScrollbarThickness,
        mainAxisTrackInset = ScrollbarInset,
        crossAxisTrackInset = endInset,
    )
}

internal val ScrollbarThickness = 4.dp

// From the edges of the scrolling area
internal val ScrollbarInset = 4.dp
