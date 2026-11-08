package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DropdownMenuPopupPositionProvider
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive floating toolbar with the screen's main action as FAB at its end, for
 * the floatingToolbar slot of [SegmentedListScreen]. It hides while the list scrolls down.
 *
 * [content] holds [FloatingToolbarIconButton]s.
 */
@Composable
fun FloatingToolbarWithFab(
    scrollBehavior: FloatingToolbarScrollBehavior,
    fabPainter: Painter,
    fabDescription: String,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    HorizontalFloatingToolbar(
        // Centered on the bottom edge it stays expanded, the scroll behavior hides it as a whole
        expanded = true,
        floatingActionButton = {
            HapticTooltipBox(text = fabDescription) {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = {
                        haptic.confirm()
                        onFabClick()
                    },
                    modifier = Modifier.clickCursor(),
                ) {
                    Icon(painter = fabPainter, contentDescription = fabDescription)
                }
            }
        },
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        scrollBehavior = scrollBehavior,
        content = content,
    )
}

/** An action of a [FloatingToolbarWithFab], with tooltip and haptic feedback. */
@Composable
fun FloatingToolbarIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(text = contentDescription, modifier = modifier) {
        IconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = Modifier.clickCursor(enabled = enabled),
            enabled = enabled,
        ) {
            Icon(painter = painter, contentDescription = contentDescription)
        }
    }
}

/**
 * The menu of a button in a [FloatingToolbarWithFab], right above the toolbar. Place it in a Box
 * around the button.
 */
@Composable
fun FloatingToolbarMenu(
    expanded: Boolean,
    groups: List<List<AppBarMenuItem>>,
    onDismissRequest: () -> Unit,
) {
    val density = LocalDensity.current
    MenuPopup(
        expanded = expanded,
        groups = groups,
        onDismissRequest = onDismissRequest,
        positionProvider = remember(density) { AboveToolbarPositionProvider(density) },
    )
}

/**
 * Directly above the button. Material's own provider measures its 48.dp margin from a window
 * height without the system bars, but its positions include the status bar, so below a high
 * status bar it pushed the menu away from the toolbar.
 */
private class AboveToolbarPositionProvider(
    private val density: Density,
) : DropdownMenuPopupPositionProvider {
    override val transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = with(density) {
        menuPositionAboveAnchor(
            anchor = anchorBounds,
            windowWidth = windowSize.width,
            menu = popupContentSize,
            isRtl = layoutDirection == LayoutDirection.Rtl,
            distance = FloatingToolbarMenuDistance.roundToPx(),
            sideMargin = MenuSideMargin.roundToPx(),
            topMargin = MenuTopMargin.roundToPx(),
        )
    }
}

/**
 * Where a menu above [anchor] goes: its bottom [distance] above the anchor, starting at the
 * anchor's start, within the window's sides, and below [topMargin] if it is too high to fit.
 */
internal fun menuPositionAboveAnchor(
    anchor: IntRect,
    windowWidth: Int,
    menu: IntSize,
    isRtl: Boolean,
    distance: Int,
    sideMargin: Int,
    topMargin: Int,
): IntOffset {
    val start = if (isRtl) anchor.right - menu.width else anchor.left
    val lastX = windowWidth - sideMargin - menu.width
    return IntOffset(
        x = when {
            lastX < sideMargin -> (windowWidth - menu.width) / 2
            else -> start.coerceIn(sideMargin, lastX)
        },
        y = (anchor.top - distance - menu.height).coerceAtLeast(topMargin),
    )
}

// From the button's touch target: the 8.dp of toolbar above it, then the 8.dp to the menu
private val FloatingToolbarMenuDistance = 16.dp
private val MenuSideMargin = 8.dp
private val MenuTopMargin = 48.dp
