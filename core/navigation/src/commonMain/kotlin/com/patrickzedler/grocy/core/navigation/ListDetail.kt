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
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
 * item is chosen. Narrower windows show list and details one after the other.
 *
 * @param sceneKey the same for the list and its details, different for every list.
 */
fun listPaneMetadata(
    sceneKey: Any,
    placeholder: @Composable () -> Unit,
): Map<String, Any> =
    ListDetailSceneStrategy.listPane(sceneKey = sceneKey, detailPlaceholder = { placeholder() }) +
        (LIST_PANE_KEY to true)

// Marks lists for the pane background, Material's own keys are internal
internal const val LIST_PANE_KEY = "com.patrickzedler.grocy.listPane"

// Marks details, so a detail opened from another one can return to it
internal const val DETAIL_PANE_KEY = "com.patrickzedler.grocy.detailPane"

// Marks details whose back leaves their list too
internal const val BACK_LEAVES_SCENE_KEY = "com.patrickzedler.grocy.backLeavesScene"

/**
 * Entry metadata of the details of the list with the same [sceneKey]. A detail opened from
 * another one ([Navigator.navigate]) takes its place beside the list, back returns to it.
 * With [backLeavesScene] back leaves the list as well, for a detail another screen opened
 * together with its list ([Navigator.navigateAll]).
 */
fun detailPaneMetadata(sceneKey: Any, backLeavesScene: Boolean = false): Map<String, Any> =
    ListDetailSceneStrategy.detailPane(sceneKey = sceneKey) +
        (DETAIL_PANE_KEY to true) +
        (BACK_LEAVES_SCENE_KEY to backLeavesScene)

/**
 * Lets back from a detail opened from another detail return to that one: Material's scene goes
 * back until its panes change, which left the list with both details at once. Back from a
 * detail marked `backLeavesScene` leaves the whole scene instead.
 */
internal class NestedDetailSceneStrategy(
    private val strategy: SceneStrategy<NavKey>,
) : SceneStrategy<NavKey> {

    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val scene = with(strategy) { calculateScene(entries) } ?: return null
        val last = entries.lastOrNull()
        if (last?.metadata[BACK_LEAVES_SCENE_KEY] == true && last in scene.entries) {
            val first = entries.indexOfFirst { it in scene.entries }
            return BackToScene(scene, entries.take(first))
        }
        val lastTwo = entries.takeLast(2)
        val isNested = lastTwo.size == 2 &&
            lastTwo.all { it.metadata[DETAIL_PANE_KEY] == true && it in scene.entries }
        return if (isNested) BackToScene(scene, entries.dropLast(1)) else scene
    }
}

/** [scene] whose back shows [previousEntries] instead of what Material's scene would. */
internal class BackToScene(
    val scene: Scene<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
) : Scene<NavKey> by scene {

    override fun equals(other: Any?): Boolean = other is BackToScene &&
        other.scene == scene && other.previousEntries == previousEntries

    override fun hashCode(): Int = 31 * scene.hashCode() + previousEntries.hashCode()
}

/**
 * Whether the current window shows details beside their list, so the list marks the item shown
 * there and the details need no back button of their own; the same rule the list-detail scene
 * follows.
 */
@Composable
fun showsDetailBesideList(): Boolean =
    calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).maxHorizontalPartitions > 1

/**
 * [this] without moving the focus into the pane that becomes the destination: in browsers, where
 * Compose lets clickable items take the focus also without keyboard, it greyed out the first item
 * of the list (the focus state layer) after a reload, until the first click. The focus stays
 * where the user acted, a new pane is a Tab away. `copy` would turn auto focus back on.
 */
internal fun PaneScaffoldDirective.withoutAutoFocus() = PaneScaffoldDirective(
    maxHorizontalPartitions = maxHorizontalPartitions,
    horizontalPartitionSpacerSize = horizontalPartitionSpacerSize,
    maxVerticalPartitions = maxVerticalPartitions,
    verticalPartitionSpacerSize = verticalPartitionSpacerSize,
    defaultPanePreferredWidth = defaultPanePreferredWidth,
    defaultPanePreferredHeight = defaultPanePreferredHeight,
    excludedBounds = excludedBounds,
    shouldAutoFocusCurrentDestination = false,
)

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
 * split half and half with Material's drag handle between them; back from a detail opened from
 * another detail returns to that one.
 */
@Composable
internal fun rememberListDetailStrategy(): SceneStrategy<NavKey> {
    val expansionState = rememberPaneExpansionState(
        anchors = PaneAnchors,
        initialAnchoredIndex = 0,
    )
    val strategy = rememberListDetailSceneStrategy<NavKey>(
        directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
            .withoutAutoFocus(),
        paneExpansionState = expansionState,
        paneExpansionDragHandle = { state -> PaneDragHandle(state) },
    )
    return remember(strategy) { NestedDetailSceneStrategy(strategy) }
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
        val isListDetail = scene.entries.any { it.metadata[LIST_PANE_KEY] == true }
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
            // A window that shrinks below the drag handle's margins (headless browsers resize to
            // one pixel for screenshots) is measured once before the scene turns single pane,
            // and Material's scaffold throws when it clamps the handle into that width
            Box(Modifier.atLeastWidth(MinTwoPaneWidth)) {
                scene.content()
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other is PaneBackgroundScene && other.scene == scene

    override fun hashCode(): Int = scene.hashCode()
}

/** Fills the incoming size, but never narrower than [width] (centered, overflowing then). */
private fun Modifier.atLeastWidth(width: Dp): Modifier = layout { measurable, constraints ->
    val minWidth = width.roundToPx()
    val measureWidth = maxOf(constraints.maxWidth, minWidth)
    val placeable = measurable.measure(
        Constraints.fixed(measureWidth, constraints.maxHeight),
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeRelative((constraints.maxWidth - placeable.width) / 2, 0)
    }
}

private val MinTwoPaneWidth = 48.dp

@Composable
internal fun rememberPaneBackgroundDecorator(): SceneDecoratorStrategy<NavKey> {
    val isTwoPane = showsDetailBesideList()
    return remember(isTwoPane) { PaneBackgroundDecorator(isTwoPane) }
}
