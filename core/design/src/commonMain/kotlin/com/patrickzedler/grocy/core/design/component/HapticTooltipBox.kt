package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds

/**
 * Plain tooltip that opens on long press (with vibration), mouse hover and keyboard focus, but
 * not on the focus a plain click gives a button in browsers.
 *
 * The tooltip does not replace the content description of the wrapped element, set both.
 * [modifier] goes on an outer box, because TooltipBox places it on an inner one where parent data
 * such as `Modifier.align` would get lost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    position: TooltipPosition = TooltipPosition.Above,
    spacingBetweenTooltipAndAnchor: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis.milliseconds
    val baseState = rememberTooltipState()
    val pressTracker = remember { TooltipPressTracker() }

    val state = remember(baseState, haptic, longPressTimeout) {
        object : TooltipState by baseState {
            override suspend fun show(mutatePriority: MutatePriority) {
                when (pressTracker.triggerOf(mutatePriority, longPressTimeout)) {
                    TooltipTrigger.PointerFocus -> return
                    TooltipTrigger.LongPress ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TooltipTrigger.MouseHover, TooltipTrigger.KeyboardFocus -> Unit
                }
                baseState.show(mutatePriority)
            }
        }
    }

    Box(
        // Watches presses before the anchor sees them, without taking them away
        modifier = modifier.pointerInput(pressTracker) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressTracker.onPress()
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                pressTracker.onRelease()
            }
        },
        propagateMinConstraints = true,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = when (position) {
                    TooltipPosition.Above -> TooltipAnchorPosition.Above
                    TooltipPosition.Below -> TooltipAnchorPosition.Below
                },
                spacingBetweenTooltipAndAnchor = spacingBetweenTooltipAndAnchor,
            ),
            tooltip = {
                PlainTooltip {
                    Text(text)
                }
            },
            state = state,
            content = content,
        )
    }
}

/** Side of the anchor a [HapticTooltipBox] shows its tooltip on. */
enum class TooltipPosition { Above, Below }
