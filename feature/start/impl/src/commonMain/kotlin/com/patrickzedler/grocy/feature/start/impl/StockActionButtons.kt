package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.patrickzedler.grocy.core.design.component.ButtonGroupOverflowIndicator
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.MenuTouchTarget
import com.patrickzedler.grocy.core.design.component.animatedItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.smallIconButtonSize
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.ic_rounded_add_shopping_cart
import com.patrickzedler.grocy.core.resources.ic_rounded_fact_check
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import com.patrickzedler.grocy.core.resources.ic_rounded_restaurant
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_basket
import com.patrickzedler.grocy.core.resources.ic_rounded_swap_horiz
import com.patrickzedler.grocy.core.resources.shopping_list_add_item
import com.patrickzedler.grocy.core.resources.stock_consume
import com.patrickzedler.grocy.core.resources.stock_inventory
import com.patrickzedler.grocy.core.resources.stock_purchase
import com.patrickzedler.grocy.core.resources.stock_transfer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Grocy's stock pages the stock overview item leads to directly, in Grocy's menu order. */
internal enum class StockAction(
    val title: StringResource,
    val icon: DrawableResource,
    /** Grocy's transfer needs locations. */
    val isAvailable: (ServerConfig) -> Boolean = { true },
) {
    PURCHASE(Res.string.stock_purchase, Res.drawable.ic_rounded_shopping_basket),
    CONSUME(Res.string.stock_consume, Res.drawable.ic_rounded_restaurant),
    TRANSFER(
        Res.string.stock_transfer,
        Res.drawable.ic_rounded_swap_horiz,
        { it.stockLocationTracking },
    ),
    INVENTORY(Res.string.stock_inventory, Res.drawable.ic_rounded_fact_check),
}

/**
 * The [actions] as a standard button group of wide tonal icon buttons, the pressed one widens;
 * where the item is too narrow the last ones move into the menu of a tonal overflow button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StockActionButtons(
    actions: List<StockAction>,
    onClick: (StockAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupOverflowIndicator { OverflowButton(menuState) }
        },
        modifier = modifier,
    ) {
        actions.forEachIndexed { index, action ->
            animatedItem(
                menuContent = { menuState ->
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides MenuTouchTarget,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(action.title)) },
                            onClick = {
                                haptic.confirm()
                                menuState.dismiss()
                                onClick(action)
                            },
                            modifier = Modifier.clickCursor(),
                            leadingIcon = {
                                Icon(painterResource(action.icon), contentDescription = null)
                            },
                            shape = MenuDefaults.itemShape(index, actions.size).shape,
                        )
                    }
                },
            ) { itemModifier, interactionSource ->
                ActionButton(action, onClick, itemModifier, interactionSource)
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: StockAction,
    onClick: (StockAction) -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val haptic = LocalHapticFeedback.current
    val label = stringResource(action.title)

    HapticTooltipBox(text = label, modifier = modifier) {
        FilledTonalIconButton(
            onClick = {
                haptic.confirm()
                onClick(action)
            },
            modifier = smallIconButtonSize(IconButtonWidthOption.Wide).clickCursor(),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = painterResource(action.icon), contentDescription = label)
        }
    }
}

/**
 * Grocy's "Add item" of the shopping list as quick add, a wide tonal icon button like the stock
 * actions; the cart with a plus tells it apart from opening the list.
 */
@Composable
internal fun ShoppingListAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val label = stringResource(Res.string.shopping_list_add_item)

    HapticTooltipBox(text = label, modifier = modifier) {
        FilledTonalIconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            modifier = smallIconButtonSize(IconButtonWidthOption.Wide).clickCursor(),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_add_shopping_cart),
                contentDescription = label,
            )
        }
    }
}

@Composable
private fun OverflowButton(menuState: ButtonGroupMenuState) {
    val haptic = LocalHapticFeedback.current
    val description = stringResource(Res.string.action_more_options)

    HapticTooltipBox(text = description) {
        FilledTonalIconButton(
            onClick = {
                haptic.confirm()
                if (menuState.isShowing) menuState.dismiss() else menuState.show()
            },
            modifier = smallIconButtonSize().clickCursor(),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_more_vert),
                contentDescription = description,
            )
        }
    }
}
