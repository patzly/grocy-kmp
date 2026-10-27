package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// Where icon buttons are: on their own, an item of a button group (it sets their width) or its
// overflow indicator
private enum class GroupPlace { None, Item, OverflowIndicator }

private val LocalGroupPlace = staticCompositionLocalOf { GroupPlace.None }

/**
 * Adds [content] to a standard button group as an item that grows while pressed and squeezes
 * its neighbours: [content] puts the modifier on its outermost layout and passes the
 * interaction source to its button. The icon buttons of this module inside it
 * ([smallIconButtonSize]) take exactly the width the group gives them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ButtonGroupScope.animatedItem(
    menuContent: @Composable (ButtonGroupMenuState) -> Unit = {},
    content: @Composable (Modifier, MutableInteractionSource) -> Unit,
) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            CompositionLocalProvider(LocalGroupPlace provides GroupPlace.Item) {
                content(Modifier.animateWidth(interactionSource), interactionSource)
            }
        },
        menuContent = menuContent,
    )
}

/**
 * Wraps the overflow indicator of a button group: its icon buttons ([smallIconButtonSize]) take
 * no room for their touch targets either, so the group's spacing lies between visible buttons.
 * Screen readers only find it once the group showed it: a group whose buttons fit never
 * measures it, and the web still announced it as an empty button (Android leaves out what is
 * not placed).
 */
@Composable
fun ButtonGroupOverflowIndicator(content: @Composable () -> Unit) {
    Box(Modifier.then(HiddenUntilMeasuredElement)) {
        CompositionLocalProvider(LocalGroupPlace provides GroupPlace.OverflowIndicator, content)
    }
}

private data object HiddenUntilMeasuredElement : ModifierNodeElement<HiddenUntilMeasuredNode>() {
    override fun create() = HiddenUntilMeasuredNode()

    override fun update(node: HiddenUntilMeasuredNode) = Unit
}

private class HiddenUntilMeasuredNode :
    Modifier.Node(), LayoutAwareModifierNode, SemanticsModifierNode {
    private var isMeasured = false

    override val shouldClearDescendantSemantics: Boolean
        get() = !isMeasured

    override fun onRemeasured(size: IntSize) {
        val measured = size.width > 0 && size.height > 0
        if (measured != isMeasured) {
            isMeasured = measured
            invalidateSemantics()
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (!isMeasured) hideFromAccessibility()
    }
}

/**
 * The size of a small icon button of [width]. On its own it keeps room for its 48.dp touch
 * target around it, like Material's default. As item of a button group ([animatedItem]) it
 * takes only its container, so the group's 12.dp lie between the visible buttons, and fills
 * the width the group gives it: a tooltip around the button would let it keep its own width
 * while the group animates the item's ([ButtonGroupOverflowIndicator] keeps its own width).
 * Its touch target still reaches 48.dp, Compose's hit test extends small targets, and where
 * two overlap in the gap the nearer button gets the touch.
 */
@Composable
fun smallIconButtonSize(width: IconButtonWidthOption = IconButtonWidthOption.Uniform): Modifier {
    val size = IconButtonDefaults.smallContainerSize(width)
    return when (LocalGroupPlace.current) {
        GroupPlace.None -> Modifier.minimumInteractiveComponentSize().size(size)
        GroupPlace.Item -> Modifier.fillMaxWidth().size(size)
        // The group measures it with the space left, which it must not fill
        GroupPlace.OverflowIndicator -> Modifier.size(size)
    }
}

/**
 * The room a small icon button of [width] keeps for its touch target on each side when it is on
 * its own: button groups keep it at their outer edges, so their buttons sit where single
 * buttons would.
 */
fun touchTargetRoom(width: IconButtonWidthOption = IconButtonWidthOption.Uniform): Dp =
    ((MinimumTouchTarget - IconButtonDefaults.smallContainerSize(width).width) / 2)
        .coerceAtLeast(0.dp)

private val MinimumTouchTarget = 48.dp
