package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A [LazyColumn] that applies symmetric horizontal padding based on window insets
 * and limits the width of its items to a maximum of 640.dp.
 */
@Composable
fun InsetLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalBasePadding: Dp = 16.dp,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val insetPadding = remember(contentPadding, safePadding, horizontalBasePadding) {
        InsetContentPadding(contentPadding, safePadding, horizontalBasePadding)
    }

    CompositionLocalProvider(LocalInsetPadding provides insetPadding) {
        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = insetPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
    }
}

/** The horizontal padding of the surrounding [InsetLazyColumn], for [edgeToEdgeItem]s. */
private val LocalInsetPadding = staticCompositionLocalOf<PaddingValues> { PaddingValues(0.dp) }

/**
 * Horizontal padding of [horizontalBasePadding] plus the larger side of [safePadding] on both
 * sides, vertical padding from [contentPadding]. Values are read during layout, so animated
 * insets (e.g. the keyboard) relayout the list without recomposing it.
 */
internal class InsetContentPadding(
    private val contentPadding: PaddingValues,
    private val safePadding: PaddingValues,
    private val horizontalBasePadding: Dp,
) : PaddingValues {

    private val horizontalPadding: Dp
        get() = maxOf(
            safePadding.calculateLeftPadding(LayoutDirection.Ltr),
            safePadding.calculateRightPadding(LayoutDirection.Ltr),
        ) + horizontalBasePadding

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = horizontalPadding

    override fun calculateTopPadding(): Dp = contentPadding.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = horizontalPadding

    override fun calculateBottomPadding(): Dp = contentPadding.calculateBottomPadding()
}

/**
 * Vertical content padding of a screen list: [top] above, [bottomInsets] plus [spacing] below.
 * [bottomInsets] is read during layout, so animated insets only relayout the list.
 */
internal class ScreenContentPadding(
    private val top: Dp,
    private val bottomInsets: PaddingValues,
    private val spacing: Dp,
) : PaddingValues {

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = 0.dp

    override fun calculateTopPadding(): Dp = top

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = 0.dp

    override fun calculateBottomPadding(): Dp = bottomInsets.calculateBottomPadding() + spacing
}

/**
 * An item in an [InsetLazyColumn] that is automatically constrained to 640.dp. With [animate]
 * (needs [key]) it fades in and out and moves with Compose's default spring.
 */
fun LazyListScope.insetItem(
    key: Any? = null,
    contentType: Any? = null,
    maxWidth: Dp = 640.dp,
    animate: Boolean = false,
    content: @Composable LazyItemScope.() -> Unit,
) {
    item(key = key, contentType = contentType) {
        Box(
            modifier = Modifier
                .then(if (animate) Modifier.animateItem() else Modifier)
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}

/**
 * An item of an [InsetLazyColumn] that spans the column's whole width, e.g. a row that scrolls
 * sideways up to the screen's edges. [content] gets the horizontal padding that lines it up with
 * the [insetItem]s of [maxWidth], to apply inside its scrolling.
 */
fun LazyListScope.edgeToEdgeItem(
    key: Any? = null,
    contentType: Any? = null,
    maxWidth: Dp = 640.dp,
    content: @Composable LazyItemScope.(contentPadding: PaddingValues) -> Unit,
) {
    item(key = key, contentType = contentType) {
        val insetPadding = LocalInsetPadding.current
        val layoutDirection = LocalLayoutDirection.current
        val start = insetPadding.calculateStartPadding(layoutDirection)
        val end = insetPadding.calculateEndPadding(layoutDirection)
        BoxWithConstraints(
            modifier = Modifier.layout { measurable, constraints ->
                // Out of the column's padding, back to its edges
                val startPx = start.roundToPx()
                val width = constraints.maxWidth + startPx + end.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width),
                )
                layout(constraints.maxWidth, placeable.height) {
                    placeable.placeRelative(-startPx, 0)
                }
            },
        ) {
            val innerWidth = this.maxWidth - start - end
            val side = (innerWidth - minOf(maxWidth, innerWidth)) / 2
            content(PaddingValues(start = start + side, end = end + side))
        }
    }
}

/**
 * A row of an [edgeToEdgeItem] that scrolls sideways, e.g. filter chips: centered on the screen
 * while it fits between the column's own padding, otherwise starting at [contentPadding] (lined
 * up with the items) and scrolling up to the screen's edges.
 */
@Composable
fun EdgeToEdgeRow(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val edgePadding = LocalInsetPadding.current
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val viewportWidth = constraints.maxWidth
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0))
                    fun PaddingValues.start() = calculateStartPadding(layoutDirection).roundToPx()
                    fun PaddingValues.end() = calculateEndPadding(layoutDirection).roundToPx()
                    val placement = edgeToEdgeRowPlacement(
                        rowWidth = placeable.width,
                        viewportWidth = viewportWidth,
                        edgeStart = edgePadding.start(),
                        edgeEnd = edgePadding.end(),
                        contentStart = contentPadding.start(),
                        contentEnd = contentPadding.end(),
                    )
                    layout(placement.width, placeable.height) {
                        // Relative, so right to left layouts start at the right edge
                        placeable.placeRelative(placement.start, 0)
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            content = content,
        )
    }
}

/** Where an [EdgeToEdgeRow] lies in its scrolling: its scrolled [width] and its [start]. */
internal data class EdgeToEdgeRowPlacement(val width: Int, val start: Int)

/**
 * Centers a row of [rowWidth] in the viewport while it fits between the edge paddings, else
 * puts it after [contentStart] with [contentEnd] after it, so it scrolls past both edges.
 */
internal fun edgeToEdgeRowPlacement(
    rowWidth: Int,
    viewportWidth: Int,
    edgeStart: Int,
    edgeEnd: Int,
    contentStart: Int,
    contentEnd: Int,
): EdgeToEdgeRowPlacement =
    if (edgeStart + rowWidth + edgeEnd <= viewportWidth) {
        EdgeToEdgeRowPlacement(
            width = viewportWidth,
            start = edgeStart + (viewportWidth - edgeStart - edgeEnd - rowWidth) / 2,
        )
    } else {
        EdgeToEdgeRowPlacement(width = contentStart + rowWidth + contentEnd, start = contentStart)
    }

/**
 * Multiple items in an [InsetLazyColumn] that are automatically constrained to 640.dp. With
 * [animate] (needs [key]) they fade in and out and move to new places with Compose's default
 * spring, e.g. between groups.
 */
fun <T> LazyListScope.insetItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    maxWidth: Dp = 640.dp,
    animate: Boolean = false,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    items(
        items = items,
        key = key,
        contentType = contentType,
        itemContent = { item ->
            Box(
                modifier = Modifier
                    .then(if (animate) Modifier.animateItem() else Modifier)
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                itemContent(item)
            }
        }
    )
}
