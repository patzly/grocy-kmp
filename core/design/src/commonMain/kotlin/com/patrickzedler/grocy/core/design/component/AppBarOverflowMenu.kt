package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Immutable
class AppBarMenuItem(
    val text: String,
    val onClick: () -> Unit,
)

/** The narrow three dots button at the end of a top app bar and its menu of [items]. */
@Composable
fun AppBarOverflowMenu(items: List<AppBarMenuItem>) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    AppBarIconButton(
        painter = painterResource(Res.drawable.ic_rounded_more_vert),
        contentDescription = stringResource(Res.string.action_more_options),
        onClick = { showMenu = true },
        narrow = true,
    )

    DropdownMenuPopup(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            dropdownMenuAnchorPosition = MenuAnchorPosition.Below,
            offset = DpOffset(x = (-8).dp, y = 0.dp),
        ),
    ) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item.text) },
                    onClick = {
                        haptic.confirm()
                        showMenu = false
                        item.onClick()
                    },
                    shape = menuItemShape(index = index, count = items.size),
                )
            }
        }
    }
}

/**
 * Shape of the item at [index] of [count] items in a menu group. Material's shape for a single
 * item is the one of middle items with small corners; a single item gets the top corners of a
 * leading and the bottom corners of a trailing item instead, so its ripple fills the group.
 */
@Composable
fun menuItemShape(index: Int, count: Int): Shape {
    if (count != 1) return MenuDefaults.itemShape(index = index, count = count).shape
    val leading = MenuDefaults.itemShape(index = 0, count = 2).shape
    val trailing = MenuDefaults.itemShape(index = 1, count = 2).shape
    if (leading !is CornerBasedShape || trailing !is CornerBasedShape) return leading
    return leading.copy(bottomStart = trailing.bottomStart, bottomEnd = trailing.bottomEnd)
}
