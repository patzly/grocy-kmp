package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.MutatePriority
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Plain tooltip that vibrates when it opens through a long press.
 *
 * The tooltip does not replace the content description of the wrapped element, set both.
 */
@Composable
fun HapticTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    spacingBetweenTooltipAndAnchor: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val baseState = rememberTooltipState()

    val state = remember(baseState, haptic) {
        object : TooltipState by baseState {
            override suspend fun show(mutatePriority: MutatePriority) {
                // UserInput is mouse hover, which should not vibrate
                if (mutatePriority != MutatePriority.UserInput) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                baseState.show(mutatePriority)
            }
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = positioning,
            spacingBetweenTooltipAndAnchor = spacingBetweenTooltipAndAnchor,
        ),
        tooltip = {
            PlainTooltip {
                Text(text)
            }
        },
        state = state,
        modifier = modifier,
        content = content,
    )
}
