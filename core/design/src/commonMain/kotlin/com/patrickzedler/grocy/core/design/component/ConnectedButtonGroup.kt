package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Single choice between [options] as connected toggle buttons. Options that do not fit move
 * into an overflow menu.
 */
@Composable
fun <T> ConnectedButtonGroup(
    options: List<T>,
    checked: T,
    onCheckedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (T) -> String = { it.toString() },
) {
    if (options.isEmpty()) return

    ButtonGroup(
        overflowIndicator = { menuState ->
            OverflowIndicator(menuState = menuState, enabled = enabled)
        },
        modifier = modifier,
        expandedRatio = 0f,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.fastForEachIndexed { index, option ->
            val isSelected = option == checked

            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { if (!isSelected) onCheckedChange(option) },
                        enabled = enabled,
                        shapes = connectedShapes(index = index, count = options.size),
                    ) {
                        Text(
                            text = label(option),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) onCheckedChange(option)
                            menuState.dismiss()
                        },
                        text = { Text(label(option)) },
                        shapes = MenuDefaults.itemShape(index, options.size),
                        enabled = enabled,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun connectedShapes(index: Int, count: Int): ToggleButtonShapes =
    when {
        count == 1 -> ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight)
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }

@Composable
private fun OverflowIndicator(
    menuState: ButtonGroupMenuState,
    enabled: Boolean,
) {
    val contentDescription = stringResource(Res.string.action_more_options)

    HapticTooltipBox(text = contentDescription) {
        FilledIconButton(
            onClick = { if (menuState.isShowing) menuState.dismiss() else menuState.show() },
            enabled = enabled,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.smallContainerSize()),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_more_vert),
                contentDescription = contentDescription,
            )
        }
    }
}
