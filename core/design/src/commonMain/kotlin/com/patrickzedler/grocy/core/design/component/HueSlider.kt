package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.patrickzedler.grocy.core.design.SEED_CHROMA
import com.patrickzedler.grocy.core.design.SEED_TONE

/** Steps of 18 degrees, enough to tell the colors apart without endless fiddling. */
private const val HUE_STEPS = 19
private const val HUE_STEP_SIZE = 360f / (HUE_STEPS + 1)

private val TrackHeight = 24.dp
private val TrackCornerSize = 8.dp

/** The hues as the theme turns them into colors, so the track shows what the user gets. */
private val HueBrush = Brush.linearGradient(
    (0..360 step 2).map { Hct.from(it.toDouble(), SEED_CHROMA, SEED_TONE).toColor() },
)

/**
 * Slider for the seed hue (0 to 360 degrees) of the theme on a rainbow track. Disabled, it
 * shows the plain disabled track, e.g. while dynamic colors are on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val sliderState = rememberSliderState(value = hue, steps = HUE_STEPS, trackRange = 0f..360f)
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val stepTracker = remember { SliderStepTracker(hue, HUE_STEP_SIZE) }
    // A change from elsewhere (restore, reset) moves the thumb too. The stored hue arrives after
    // the thumb moved on, so while the user moves it, it would jump back and tick twice.
    LaunchedEffect(hue) {
        if (!isDragged && !isPressed) {
            sliderState.value = hue
            stepTracker.reset(hue)
        }
    }

    Box(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        if (enabled) {
            // Only the rainbow below the thumb and the gaps around it, drawn as white mask
            Slider(
                state = sliderState,
                modifier = Modifier.clickCursor(),
                onValueChange = {},
                interactionSource = interactionSource,
                track = { state ->
                    SliderDefaults.Track(
                        sliderState = state,
                        trackCornerSize = TrackCornerSize,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White,
                        ),
                        modifier = Modifier
                            .height(TrackHeight)
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = HueBrush, blendMode = BlendMode.SrcIn)
                            },
                    )
                },
            )
        }

        val disabledTrack = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        val disabledTick = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        Slider(
            state = sliderState,
            modifier = Modifier.clickCursor(enabled = enabled),
            enabled = enabled,
            onValueChange = {
                // Drags report every move, a tick belongs only to a new step
                if (stepTracker.moveTo(it)) {
                    sliderState.value = it
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onHueChange(it)
                }
            },
            interactionSource = interactionSource,
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    enabled = enabled,
                    trackCornerSize = TrackCornerSize,
                    drawStopIndicator = null,
                    colors = if (enabled) {
                        // The rainbow below shows through, only the ticks stay visible
                        SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            activeTickColor = Color.White,
                            inactiveTickColor = Color.White,
                        )
                    } else {
                        SliderDefaults.colors(
                            disabledActiveTrackColor = disabledTrack,
                            disabledInactiveTrackColor = disabledTrack,
                            disabledActiveTickColor = disabledTick,
                            disabledInactiveTickColor = disabledTick,
                        )
                    },
                    modifier = Modifier.height(TrackHeight),
                )
            },
        )
    }
}
