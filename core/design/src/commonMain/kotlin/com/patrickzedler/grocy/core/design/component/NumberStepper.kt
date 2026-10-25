package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * A number field with Grocy's number picker buttons after it: minus and plus as round filled
 * icon buttons, so a value can be changed by one without the keyboard. [field] fills the width
 * the buttons leave.
 */
@Composable
fun NumberStepperField(
    state: TextFieldState,
    stepper: NumberStepper,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    field: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(StepperSpacing),
        verticalAlignment = Alignment.Top,
    ) {
        field(Modifier.weight(1f))
        NumberStepperButtons(
            state = state,
            stepper = stepper,
            enabled = enabled,
            // Centered on the field, not on its supporting text below
            modifier = Modifier.padding(top = FieldButtonOffset),
        )
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
        horizontalArrangement = Arrangement.spacedBy(ButtonSpacing),
    ) {
        buttons.forEach { button ->
            val isEnabled = enabled && button.result != null
            customItem(
                buttonGroupContent = {
                    val source = remember { MutableInteractionSource() }
                    StepIconButton(
                        button = button,
                        enabled = isEnabled,
                        onClick = { button.result?.let(state::setTextAndPlaceCursorAtEnd) },
                        modifier = Modifier.animateWidth(source),
                        interactionSource = source,
                    )
                },
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
            )
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
            modifier = Modifier.clickCursor(enabled = enabled),
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

// Round 40.dp buttons in 48.dp touch targets leave 4.dp on each side: these spacings make the
// visible gaps the 12.dp of Material's button groups, to the field as well
private val ButtonSpacing = 4.dp
private val StepperSpacing = 8.dp

// Material's text field is 56.dp high, the buttons' touch targets 48.dp
private val FieldButtonOffset = 4.dp
