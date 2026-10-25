@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.patrickzedler.grocy.feature.masterdata.impl.related

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.ListItemOverflowMenu
import com.patrickzedler.grocy.core.design.component.MenuTouchTarget
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
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
    val menuSource = remember { MutableInteractionSource() }

    ButtonGroup(
        overflowIndicator = { menuState ->
            NarrowIconButton(
                painter = painterResource(Res.drawable.ic_rounded_more_vert),
                description = stringResource(Res.string.action_more_options),
                onClick = { if (menuState.isShowing) menuState.dismiss() else menuState.show() },
            )
        },
        horizontalArrangement = ButtonGroupDefaults.HorizontalArrangement,
    ) {
        buttons.forEach { button ->
            customItem(
                buttonGroupContent = {
                    val source = remember { MutableInteractionSource() }
                    NarrowIconButton(
                        painter = button.painter,
                        description = button.description,
                        onClick = button.onClick,
                        enabled = button.enabled,
                        modifier = Modifier.animateWidth(source),
                        interactionSource = source,
                    )
                },
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
            )
        }
        customItem(
            buttonGroupContent = {
                ListItemOverflowMenu(
                    groups = menuGroups,
                    modifier = Modifier.animateWidth(menuSource),
                    interactionSource = menuSource,
                )
            },
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
        )
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
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(
                    IconButtonDefaults.smallContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Narrow,
                    ),
                )
                .clickCursor(enabled = enabled),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painter, contentDescription = description)
        }
    }
}
