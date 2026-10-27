@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.related

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.ButtonGroupOverflowIndicator
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.MenuTouchTarget
import com.patrickzedler.grocy.core.design.component.animatedItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.design.component.touchTargetRoom
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A narrow icon button of a list item, e.g. Grocy's consume button of a stock entry. */
internal class ItemButton(
    val painter: Painter,
    val description: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * The quick actions of a list item and its menu as a standard button group of narrow buttons,
 * so the item keeps room for its texts; where the item is too narrow the buttons move into the
 * group's own menu.
 */
@Composable
internal fun ItemButtons(
    buttons: List<ItemButton>,
    menuGroups: List<List<AppBarMenuItem>>,
) {
    val haptic = LocalHapticFeedback.current

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupOverflowIndicator {
                NarrowIconButton(
                    painter = painterResource(Res.drawable.ic_rounded_more_vert),
                    description = stringResource(Res.string.action_more_options),
                    onClick = {
                        if (menuState.isShowing) menuState.dismiss() else menuState.show()
                    },
                )
            }
        },
        // The item's padding plus the room single buttons keep for their touch targets
        modifier = Modifier.padding(horizontal = touchTargetRoom(IconButtonWidthOption.Narrow)),
    ) {
        buttons.forEach { button ->
            animatedItem(
                menuContent = { menuState ->
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides MenuTouchTarget,
                    ) {
                        DropdownMenuItem(
                            text = { Text(button.description) },
                            enabled = button.enabled,
                            modifier = Modifier.clickCursor(enabled = button.enabled),
                            onClick = {
                                haptic.confirm()
                                menuState.dismiss()
                                button.onClick()
                            },
                        )
                    }
                },
            ) { itemModifier, interactionSource ->
                NarrowIconButton(
                    painter = button.painter,
                    description = button.description,
                    onClick = button.onClick,
                    enabled = button.enabled,
                    modifier = itemModifier,
                    interactionSource = interactionSource,
                )
            }
        }
        animatedItem(
            menuContent = { menuState ->
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides MenuTouchTarget,
                ) {
                    menuGroups.flatten().forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.text) },
                            modifier = Modifier.clickCursor(),
                            onClick = {
                                haptic.confirm()
                                menuState.dismiss()
                                item.onClick()
                            },
                        )
                    }
                }
            },
        ) { itemModifier, interactionSource ->
            ListItemOverflowMenu(
                groups = menuGroups,
                modifier = itemModifier,
                interactionSource = interactionSource,
            )
        }
    }
}

/** A standard narrow icon button with its tooltip. */
@Composable
private fun NarrowIconButton(
    painter: Painter,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(text = description, modifier = modifier) {
        IconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            enabled = enabled,
            modifier = smallIconButtonSize(IconButtonWidthOption.Narrow)
                .clickCursor(enabled = enabled),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painter, contentDescription = description)
        }
    }
}
