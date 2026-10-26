package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** An item of an overflow menu; a disabled one shows that its action is not possible now. */
@Immutable
class AppBarMenuItem(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * The narrow three dots button at the end of a top app bar and its menu: one expressive menu
 * group per entry of [groups], empty groups are left out.
 */
@Composable
fun AppBarOverflowMenu(groups: List<List<AppBarMenuItem>>) {
    val shownGroups = groups.filter { it.isNotEmpty() }
    if (shownGroups.isEmpty()) return
    var showMenu by remember { mutableStateOf(false) }

    AppBarIconButton(
        painter = painterResource(Res.drawable.ic_rounded_more_vert),
        contentDescription = stringResource(Res.string.action_more_options),
        onClick = { showMenu = true },
        narrow = true,
    )
    OverflowMenuPopup(
        expanded = showMenu,
        groups = shownGroups,
        onDismissRequest = { showMenu = false },
    )
}

/**
 * The standard narrow three dots button at the end of a list item and its menu with the
 * actions on the item's object, like [AppBarOverflowMenu]. In a button group, pass the
 * group's `animateWidth` as [modifier] with the same [interactionSource].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListItemOverflowMenu(
    groups: List<List<AppBarMenuItem>>,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val shownGroups = groups.filter { it.isNotEmpty() }
    if (shownGroups.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    val description = stringResource(Res.string.action_more_options)

    // A box of its own, so the menu opens at the button; the button takes the width it is given
    Box(modifier, propagateMinConstraints = true) {
        HapticTooltipBox(text = description) {
            IconButton(
                onClick = {
                    haptic.confirm()
                    showMenu = true
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(
                        IconButtonDefaults.smallContainerSize(
                            IconButtonDefaults.IconButtonWidthOption.Narrow,
                        ),
                    )
                    .clickCursor(),
                shapes = IconButtonDefaults.shapes(),
                interactionSource = interactionSource,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_more_vert),
                    contentDescription = description,
                )
            }
        }
        OverflowMenuPopup(
            expanded = showMenu,
            groups = shownGroups,
            onDismissRequest = { showMenu = false },
        )
    }
}

@Composable
private fun OverflowMenuPopup(
    expanded: Boolean,
    groups: List<List<AppBarMenuItem>>,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            dropdownMenuAnchorPosition = MenuAnchorPosition.Below,
            offset = DpOffset(x = (-8).dp, y = 0.dp),
        ),
    ) {
        // The popup inherits the touch target size of where it opens: the trailing slot of a list
        // item lowers it by its padding, which shrank the items below the 48dp of other menus
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides MenuTouchTarget) {
            MenuGroups(groups, haptic, onDismissRequest)
        }
    }
}

/** The groups of a menu, children of the popup's column, so they share its width. */
@Composable
private fun MenuGroups(
    groups: List<List<AppBarMenuItem>>,
    haptic: HapticFeedback,
    onDismissRequest: () -> Unit,
) {
    groups.forEachIndexed { groupIndex, items ->
        if (groupIndex > 0) Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = groupIndex, count = groups.size),
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item.text) },
                    modifier = Modifier.clickCursor(item.enabled),
                    enabled = item.enabled,
                    onClick = {
                        haptic.confirm()
                        onDismissRequest()
                        item.onClick()
                    },
                    shape = menuItemShape(
                        index = index,
                        count = items.size,
                        groupIndex = groupIndex,
                        groupCount = groups.size,
                    ),
                )
            }
        }
    }
}

/** Material's minimum touch target, which menu items take as their height too. */
val MenuTouchTarget = 48.dp

/**
 * Shape of the item at [index] of [count] items in group [groupIndex] of [groupCount] groups of
 * a menu, so its ripple follows the group's corners: large corners only where the group has
 * them (top of the first group, bottom of the last one), small ones elsewhere. Material's own
 * shapes look at the group alone; its shape for a single item even has small corners all round.
 */
@Composable
fun menuItemShape(index: Int, count: Int, groupIndex: Int = 0, groupCount: Int = 1): Shape {
    val leading = MenuDefaults.itemShape(index = 0, count = 2).shape
    val middle = MenuDefaults.itemShape(index = 1, count = 3).shape
    val trailing = MenuDefaults.itemShape(index = 1, count = 2).shape
    if (leading !is CornerBasedShape || middle !is CornerBasedShape ||
        trailing !is CornerBasedShape
    ) {
        return MenuDefaults.itemShape(index = index, count = count).shape
    }
    val top = if (index == 0 && groupIndex == 0) leading else middle
    val bottom = if (index == count - 1 && groupIndex == groupCount - 1) trailing else middle
    return top.copy(bottomStart = bottom.bottomStart, bottomEnd = bottom.bottomEnd)
}
