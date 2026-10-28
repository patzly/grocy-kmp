package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.core.model.text.steppedDecimal
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_decrease
import com.patrickzedler.grocy.core.resources.action_increase
import com.patrickzedler.grocy.core.resources.ic_rounded_add
import com.patrickzedler.grocy.core.resources.ic_rounded_remove
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The values the buttons of a [NumberStepperField] step within; null is unbounded. */
@Immutable
data class NumberStepper(val min: Double? = null, val max: Double? = null) {
    companion object {
        /** For amounts that must be above zero, e.g. 1 can not go down to 0. */
        val Positive = NumberStepper(min = Double.MIN_VALUE)
    }
}

/**
 * A number field with Grocy's number picker buttons after it: minus and plus as narrow filled
 * icon buttons in a standard button group, so a value can be changed by one without the
 * keyboard. [field] fills the width
 * the buttons leave; its [error] shows below both like the field's supporting text, so the
 * buttons stay centered on the field when it grows with a wrapped label, and so does a
 * [supportingText] while there is no error.
 */
@Composable
fun NumberStepperField(
    state: TextFieldState,
    stepper: NumberStepper,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
    field: @Composable (Modifier) -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().extendToEnd(EndOverlap),
            horizontalArrangement = Arrangement.spacedBy(StepperSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            field(Modifier.weight(1f))
            NumberStepperButtons(state = state, stepper = stepper, enabled = enabled)
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(SupportingTextPadding)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(SupportingTextPadding),
            )
        }
    }
}

/**
 * Lets the content reach [overlap] into the padding at the end, where only the transparent part
 * of a touch target lands.
 */
private fun Modifier.extendToEnd(overlap: Dp): Modifier = layout { measurable, constraints ->
    val extra = overlap.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra,
        ),
    )
    layout(placeable.width - extra, placeable.height) {
        // Relative, so the extra width lies at the end in right to left layouts too
        placeable.placeRelative(0, 0)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NumberStepperButtons(
    state: TextFieldState,
    stepper: NumberStepper,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val separator = platformDecimalSeparator()
    val text = state.text.toString()
    val decreased = steppedDecimal(text, -1, stepper.min, stepper.max, separator)
    val increased = steppedDecimal(text, 1, stepper.min, stepper.max, separator)
    val buttons = listOf(
        StepButton(
            painter = painterResource(Res.drawable.ic_rounded_remove),
            description = stringResource(Res.string.action_decrease),
            result = decreased,
        ),
        StepButton(
            painter = painterResource(Res.drawable.ic_rounded_add),
            description = stringResource(Res.string.action_increase),
            result = increased,
        ),
    )

    ButtonGroup(
        // Two buttons always fit, nothing ever moves into a menu
        overflowIndicator = {},
        modifier = modifier,
    ) {
        buttons.forEach { button ->
            val isEnabled = enabled && button.result != null
            animatedItem(
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(button.description) },
                        enabled = isEnabled,
                        onClick = {
                            menuState.dismiss()
                            button.result?.let(state::setTextAndPlaceCursorAtEnd)
                        },
                    )
                },
            ) { itemModifier, interactionSource ->
                StepIconButton(
                    button = button,
                    enabled = isEnabled,
                    onClick = { button.result?.let(state::setTextAndPlaceCursorAtEnd) },
                    modifier = itemModifier,
                    interactionSource = interactionSource,
                )
            }
        }
    }
}

private class StepButton(val painter: Painter, val description: String, val result: String?)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepIconButton(
    button: StepButton,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    val haptic = LocalHapticFeedback.current

    HapticTooltipBox(text = button.description, modifier = modifier) {
        FilledIconButton(
            onClick = {
                haptic.confirm()
                onClick()
            },
            enabled = enabled,
            modifier = smallIconButtonSize(IconButtonWidthOption.Narrow)
                .clickCursor(enabled = enabled),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            shapes = IconButtonDefaults.shapes(),
            interactionSource = interactionSource,
        ) {
            Icon(painter = button.painter, contentDescription = button.description)
        }
    }
}

// The 12.dp of Material's button groups to the field and to the item's end as well; the buttons
// take no room for their touch targets in the group, so only the item's padding needs reaching
private val StepperSpacing = 12.dp
private val EndOverlap = 4.dp

// Material's supporting text padding of text fields, which it keeps internal
private val SupportingTextPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp)
