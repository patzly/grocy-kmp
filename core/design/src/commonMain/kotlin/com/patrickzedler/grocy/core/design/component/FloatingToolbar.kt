package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback

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
