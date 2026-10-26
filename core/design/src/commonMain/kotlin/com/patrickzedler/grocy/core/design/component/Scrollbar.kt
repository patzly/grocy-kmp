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
 * [trackInset] keeps the thumb away from the top and bottom (and rounded corners there),
 * [endInset] from the end edge; a negative one draws it in the padding beside this node.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: ScrollableState,
    trackInset: Dp = ScrollbarTrackInset,
    endInset: Dp = ScrollbarEndInset,
): Modifier {
    val indicatorState = state.scrollIndicatorState ?: return this
    return nonInteractiveScrollbar(
        state = indicatorState,
        orientation = Orientation.Vertical,
        thickness = ScrollbarThickness,
        mainAxisTrackInset = trackInset,
        crossAxisTrackInset = endInset,
    )
}

internal val ScrollbarThickness = 4.dp

// As far from the top and bottom as lists start and end, which also clears the large corners
// of pane surfaces and menu groups at the end inset
private val ScrollbarTrackInset = 16.dp
private val ScrollbarEndInset = 4.dp
