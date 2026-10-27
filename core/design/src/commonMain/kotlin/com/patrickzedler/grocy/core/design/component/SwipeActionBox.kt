package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * An action of [SwipeActionBox]: [onSwipe] runs when the item is let go beyond the threshold.
 * A disabled action cannot be swiped to. Swiping is a gesture, so the action must also be
 * reachable otherwise, e.g. in the item's menu and as its custom accessibility action.
 * [containerColor] and [contentColor] tell the actions of both sides apart, primary and
 * onPrimary by default.
 */
@Immutable
class SwipeAction(
    val icon: Painter,
    val enabled: Boolean = true,
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
    val onSwipe: () -> Unit,
)

/**
 * Swipes [content] sideways to run an action, like Gmail's list: a rounded primary surface
 * opens beside the item and uncovers the action's icon, which rests near the screen's edge
 * until the surface's middle reaches it and then stays in the middle. Passing the threshold
 * ticks. Unlike Gmail the item is not swiped away: letting it go beyond the threshold runs the
 * action and the item springs back, below the threshold it springs back without one.
 *
 * @param startAction opened at the start edge, swiping towards the end.
 * @param endAction opened at the end edge, swiping towards the start.
 */
@Composable
fun SwipeActionBox(
    startAction: SwipeAction?,
    endAction: SwipeAction?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val motionScheme = MaterialTheme.motionScheme
    val returnSpec = motionScheme.defaultSpatialSpec<Float>()
    val currentStart by rememberUpdatedState(startAction?.takeIf { it.enabled })
    val currentEnd by rememberUpdatedState(endAction?.takeIf { it.enabled })

    var width by remember { mutableIntStateOf(0) }
    // Towards the end positive, in both layout directions
    var offset by remember { mutableFloatStateOf(0f) }
    var returnJob by remember { mutableStateOf<Job?>(null) }
    val threshold = with(density) { swipeThreshold(width.toFloat(), SwipeThreshold.toPx()) }
    val isArmed by remember(threshold) { derivedStateOf { abs(offset) >= threshold } }
    var wasArmed by remember { mutableStateOf(false) }

    LaunchedEffect(isArmed) {
        // Only while dragging: the item passes the threshold again while it springs back
        if (isArmed != wasArmed && returnJob?.isActive != true) {
            haptic.performHapticFeedback(
                if (isArmed) {
                    HapticFeedbackType.GestureThresholdActivate
                } else {
                    HapticFeedbackType.SegmentTick
                },
            )
        }
        wasArmed = isArmed
    }

    val draggableState = rememberDraggableState { delta ->
        offset = swipeOffset(
            current = offset,
            delta = delta,
            width = width.toFloat(),
            canSwipeTowardsEnd = currentStart != null,
            canSwipeTowardsStart = currentEnd != null,
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                reverseDirection = isRtl,
                onDragStarted = { returnJob?.cancel() },
                onDragStopped = {
                    if (abs(offset) >= threshold) {
                        val action = if (offset > 0f) currentStart else currentEnd
                        action?.let {
                            haptic.confirm()
                            it.onSwipe()
                        }
                    }
                    returnJob = scope.launch {
                        animate(offset, 0f, animationSpec = returnSpec) { value, _ ->
                            offset = value
                        }
                    }
                },
            ),
    ) {
        val action = when {
            offset > 0f -> startAction
            offset < 0f -> endAction
            else -> null
        }
        if (action != null) {
            SwipeActionSurface(
                action = action,
                revealed = with(density) { (abs(offset).toDp() - SwipeActionGap) },
                atStart = offset > 0f,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(Modifier.offset { IntOffset(offset.roundToInt(), 0) }) {
            content()
        }
    }
}

/** The opened surface beside the item, as wide as the item moved minus the gap. */
@Composable
private fun SwipeActionSurface(
    action: SwipeAction,
    revealed: Dp,
    atStart: Boolean,
    modifier: Modifier = Modifier,
) {
    if (revealed <= 0.dp) return
    Box(modifier) {
        Box(
            Modifier
                .align(if (atStart) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight()
                .layout { measurable, constraints ->
                    val surfaceWidth = revealed.roundToPx().coerceAtMost(constraints.maxWidth)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = surfaceWidth, maxWidth = surfaceWidth),
                    )
                    layout(surfaceWidth, placeable.height) { placeable.place(0, 0) }
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(action.containerColor.takeOrElse { MaterialTheme.colorScheme.primary }),
        ) {
            Icon(
                painter = action.icon,
                // The item's menu and accessibility actions name it
                contentDescription = null,
                tint = action.contentColor.takeOrElse { MaterialTheme.colorScheme.onPrimary },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val surfaceWidth = revealed.toPx()
                        val iconSize = SwipeIconSize.toPx()
                        val start = swipeIconStart(
                            surfaceWidth = surfaceWidth,
                            iconSize = iconSize,
                            edgeInset = SwipeIconInset.toPx(),
                            surfaceAtStart = atStart,
                        )
                        IntOffset(start.roundToInt(), 0)
                    }
                    // Full size in a narrower surface, which uncovers it instead of shrinking it
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .size(SwipeIconSize),
            )
        }
    }
}

/**
 * The new offset after a drag by [delta]: only towards sides with an action, at most the
 * item's [width].
 */
internal fun swipeOffset(
    current: Float,
    delta: Float,
    width: Float,
    canSwipeTowardsEnd: Boolean,
    canSwipeTowardsStart: Boolean,
): Float {
    val max = if (canSwipeTowardsEnd) width else 0f
    val min = if (canSwipeTowardsStart) -width else 0f
    return (current + delta).coerceIn(min, max)
}

/** How far the item must move to run an action: [preferred], but at most 40 % of [width]. */
internal fun swipeThreshold(width: Float, preferred: Float): Float =
    if (width <= 0f) preferred else minOf(preferred, width * MAX_THRESHOLD_FRACTION)

/**
 * Start of the icon inside the opened surface: [edgeInset] from the screen's edge, where the
 * surface opens from ([surfaceAtStart]: the start edge), so the growing surface uncovers it
 * without moving it, and in the surface's middle once that is farther from the edge.
 */
internal fun swipeIconStart(
    surfaceWidth: Float,
    iconSize: Float,
    edgeInset: Float,
    surfaceAtStart: Boolean,
): Float {
    val middle = (surfaceWidth - iconSize) / 2f
    return if (surfaceAtStart) {
        maxOf(edgeInset, middle)
    } else {
        minOf(surfaceWidth - edgeInset - iconSize, middle)
    }
}

private const val MAX_THRESHOLD_FRACTION = 0.4f

// Beyond Material's touch slop and a casual flick, within reach of a thumb
private val SwipeThreshold = 96.dp

// Between the opened surface and the item, like Gmail's
private val SwipeActionGap = 4.dp
private val SwipeIconSize = 24.dp
// From the screen's edge, like the icons of list items
private val SwipeIconInset = 16.dp
