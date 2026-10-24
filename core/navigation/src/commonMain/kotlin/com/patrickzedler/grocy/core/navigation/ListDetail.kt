@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.patrickzedler.grocy.core.navigation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import com.patrickzedler.grocy.core.design.component.clickCursor
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Entry metadata of a list whose [DetailRoute]s open beside it where two panes are the standard
 * (Material's expanded window width and wider); [placeholder] fills the detail pane until an
 * item is chosen. Narrower windows show list and details one after the other.
 *
 * @param sceneKey the same for the list and its details, different for every list.
 */
fun listPaneMetadata(sceneKey: Any, placeholder: @Composable () -> Unit): Map<String, Any> =
    ListDetailSceneStrategy.listPane(sceneKey = sceneKey, detailPlaceholder = { placeholder() })

/** Entry metadata of the details of the list with the same [sceneKey]. */
fun detailPaneMetadata(sceneKey: Any): Map<String, Any> =
    ListDetailSceneStrategy.detailPane(sceneKey = sceneKey)

/**
 * Whether the current window shows details beside their list, so the list marks the item shown
 * there and the details need no back button of their own; the same rule the list-detail scene
 * follows.
 */
@Composable
fun showsDetailBesideList(): Boolean =
    calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions > 1

/** Where the drag handle between list and details snaps to, half and half first. */
internal val PaneAnchors = listOf(
    PaneExpansionAnchor.Proportion(1f / 2),
    PaneExpansionAnchor.Proportion(1f / 3),
    PaneExpansionAnchor.Proportion(2f / 3),
)

/** The anchors in their order on the screen, from the start edge. */
internal val PaneAnchorsInOrder = PaneAnchors.sortedBy { it.proportion }

/**
 * The anchor an arrow key moves the drag handle to from [current], null at the edge; [towardsEnd]
 * is the reading direction's end.
 */
internal fun neighbourAnchor(
    current: PaneExpansionAnchor?,
    towardsEnd: Boolean,
): PaneExpansionAnchor.Proportion? {
    val index = PaneAnchorsInOrder.indexOf(current)
    if (index == -1) return PaneAnchors.first()
    return PaneAnchorsInOrder.getOrNull(if (towardsEnd) index + 1 else index - 1)
}

/**
 * Material's list-detail scene with its default pane rules of the current window, its panes
 * split half and half with Material's drag handle between them.
 */
@Composable
internal fun rememberListDetailStrategy(): SceneStrategy<NavKey> {
    val expansionState = rememberPaneExpansionState(
        anchors = PaneAnchors,
        initialAnchoredIndex = 0,
    )
    return rememberListDetailSceneStrategy(
        paneExpansionState = expansionState,
        paneExpansionDragHandle = { state -> PaneDragHandle(state) },
    )
}

/**
 * Material's vertical drag handle: drags, the screen reader's action and the arrow keys move it
 * between the anchors, each snap ticks.
 */
@Composable
private fun ThreePaneScaffoldScope.PaneDragHandle(state: PaneExpansionState) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LaunchedEffect(state) {
        // Not for the initial anchor
        snapshotFlow { state.currentAnchor }.drop(1).collect {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }

    VerticalDragHandle(
        modifier = Modifier
            .paneExpansionDraggable(
                state = state,
                minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                interactionSource = interactionSource,
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                val towardsEnd = when (event.key) {
                    Key.DirectionRight -> !isRtl
                    Key.DirectionLeft -> isRtl
                    else -> return@onKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) {
                    neighbourAnchor(state.currentAnchor, towardsEnd)?.let { anchor ->
                        scope.launch { state.animateTo(anchor) }
                    }
                }
                true
            }
            .clickCursor(),
        interactionSource = interactionSource,
    )
}
