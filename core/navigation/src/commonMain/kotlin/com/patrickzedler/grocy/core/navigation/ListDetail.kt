@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.patrickzedler.grocy.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.patrickzedler.grocy.core.design.component.clickCursor
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Entry metadata of a list whose [DetailRoute]s open beside it where two panes are the standard
 * (Material's expanded window width and wider); [placeholder] fills the detail pane until an
 * item is chosen. Without [placeholder] the list fills the window alone until a detail opens,
 * like the settings' categories. Narrower windows show list and details one after the other.
 *
 * @param sceneKey the same for the list and its details, different for every list.
 */
fun listPaneMetadata(
    sceneKey: Any,
    placeholder: (@Composable () -> Unit)?,
): Map<String, Any> = if (placeholder == null) {
    ListDetailSceneStrategy.listPane(sceneKey = sceneKey) +
        (LIST_PANE_KEY to true) + (LIST_ALONE_KEY to true)
} else {
    ListDetailSceneStrategy.listPane(sceneKey = sceneKey, detailPlaceholder = { placeholder() }) +
        (LIST_PANE_KEY to true)
}

// Marks lists for the pane background, Material's own keys are internal
internal const val LIST_PANE_KEY = "com.patrickzedler.grocy.listPane"

// Marks lists without placeholder, which stay alone while no detail is open
internal const val LIST_ALONE_KEY = "com.patrickzedler.grocy.listAlone"

/**
 * Leaves a list without placeholder alone on the screen while it is on top; with a detail on
 * top [strategy] lays out both, the list beside the detail.
 */
internal class ListAloneSceneStrategy(
    private val strategy: SceneStrategy<NavKey>,
) : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        if (entries.lastOrNull()?.metadata?.get(LIST_ALONE_KEY) == true) return null
        val scene = with(strategy) { calculateScene(entries) } ?: return null
        val hasListAlone = scene.entries.any { it.metadata[LIST_ALONE_KEY] == true }
        // Back from the list with its detail returns to the list alone, also in the browser's
        // history: Material's scene would go back to what lay below the list
        return if (hasListAlone) ListBesideDetailScene(scene, entries.dropLast(1)) else scene
    }
}

/** [scene] of a list without placeholder beside its detail; back shows the list alone. */
internal class ListBesideDetailScene(
    val scene: Scene<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> by scene {

    override fun equals(other: Any?): Boolean = other is ListBesideDetailScene &&
        other.scene == scene && other.previousEntries == previousEntries

    override fun hashCode(): Int = 31 * scene.hashCode() + previousEntries.hashCode()
}

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
 * split half and half with Material's drag handle between them; lists without placeholder stay
 * alone until a detail opens.
 */
@Composable
internal fun rememberListDetailStrategy(): SceneStrategy<NavKey> {
    val expansionState = rememberPaneExpansionState(
        anchors = PaneAnchors,
        initialAnchoredIndex = 0,
    )
    val strategy = rememberListDetailSceneStrategy<NavKey>(
        paneExpansionState = expansionState,
        paneExpansionDragHandle = { state -> PaneDragHandle(state) },
    )
    return remember(strategy) { ListAloneSceneStrategy(strategy) }
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

/**
 * Lays the panes of a list-detail scene on surfaceContainerHighest, the space between them
 * included, while [isTwoPane]; scenes of one pane keep the screens' own background.
 */
internal class PaneBackgroundDecorator(
    private val isTwoPane: Boolean,
) : SceneDecoratorStrategy<NavKey> {

    override fun SceneDecoratorStrategyScope<NavKey>.decorateScene(
        scene: Scene<NavKey>,
    ): Scene<NavKey> {
        // A list without placeholder alone on the screen keeps the screens' background
        val isListAlone = scene.entries.singleOrNull()?.metadata?.get(LIST_ALONE_KEY) == true
        val isListDetail = scene.entries.any { it.metadata[LIST_PANE_KEY] == true } && !isListAlone
        return if (isTwoPane && isListDetail && scene !is OverlayScene) {
            PaneBackgroundScene(scene)
        } else {
            scene
        }
    }
}

/** [scene] on the panes' background; equal to [scene], so its transitions stay the same. */
internal class PaneBackgroundScene(val scene: Scene<NavKey>) : Scene<NavKey> by scene {

    override val content: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            scene.content()
        }
    }

    override fun equals(other: Any?): Boolean =
        other is PaneBackgroundScene && other.scene == scene

    override fun hashCode(): Int = scene.hashCode()
}

@Composable
internal fun rememberPaneBackgroundDecorator(): SceneDecoratorStrategy<NavKey> {
    val isTwoPane = showsDetailBesideList()
    return remember(isTwoPane) { PaneBackgroundDecorator(isTwoPane) }
}
