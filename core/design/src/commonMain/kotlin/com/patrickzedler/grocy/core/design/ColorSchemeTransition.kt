package com.patrickzedler.grocy.core.design

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp

/**
 * [target], blended over from the scheme shown before whenever [transitionKey] changes (light or
 * dark, contrast, color source, variant), so the whole screen fades into its new look. Other
 * changes, e.g. the hue while its slider moves, apply right away: a transition per slider step
 * would pile up animations.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun animateColorScheme(target: ColorScheme, transitionKey: Any): ColorScheme {
    val currentTarget by rememberUpdatedState(target)
    val progress = remember { Animatable(1f) }
    var animatedKey by remember { mutableStateOf(transitionKey) }
    var start by remember { mutableStateOf(target) }
    // What the last frame showed, the start of the next transition
    val shown = remember { ShownScheme(target) }
    val spec = remember { MotionScheme.expressive().slowEffectsSpec<Float>() }

    LaunchedEffect(transitionKey) {
        if (transitionKey == animatedKey) return@LaunchedEffect
        start = shown.scheme
        animatedKey = transitionKey
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
    }

    val scheme = when {
        // The new key's first frame, before the transition starts: keep the old look
        transitionKey != animatedKey -> shown.scheme
        progress.value < 1f -> lerp(start, currentTarget, progress.value)
        else -> target
    }
    SideEffect { shown.scheme = scheme }
    return scheme
}

private class ShownScheme(var scheme: ColorScheme)

/** Every color of the scheme between [start] and [stop]. */
internal fun lerp(start: ColorScheme, stop: ColorScheme, fraction: Float): ColorScheme = stop.copy(
    primary = lerp(start.primary, stop.primary, fraction),
    onPrimary = lerp(start.onPrimary, stop.onPrimary, fraction),
    primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, fraction),
    onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, fraction),
    inversePrimary = lerp(start.inversePrimary, stop.inversePrimary, fraction),
    secondary = lerp(start.secondary, stop.secondary, fraction),
    onSecondary = lerp(start.onSecondary, stop.onSecondary, fraction),
    secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, fraction),
    onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, fraction),
    tertiary = lerp(start.tertiary, stop.tertiary, fraction),
    onTertiary = lerp(start.onTertiary, stop.onTertiary, fraction),
    tertiaryContainer = lerp(start.tertiaryContainer, stop.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(start.onTertiaryContainer, stop.onTertiaryContainer, fraction),
    background = lerp(start.background, stop.background, fraction),
    onBackground = lerp(start.onBackground, stop.onBackground, fraction),
    surface = lerp(start.surface, stop.surface, fraction),
    onSurface = lerp(start.onSurface, stop.onSurface, fraction),
    surfaceVariant = lerp(start.surfaceVariant, stop.surfaceVariant, fraction),
    onSurfaceVariant = lerp(start.onSurfaceVariant, stop.onSurfaceVariant, fraction),
    surfaceTint = lerp(start.surfaceTint, stop.surfaceTint, fraction),
    inverseSurface = lerp(start.inverseSurface, stop.inverseSurface, fraction),
    inverseOnSurface = lerp(start.inverseOnSurface, stop.inverseOnSurface, fraction),
    error = lerp(start.error, stop.error, fraction),
    onError = lerp(start.onError, stop.onError, fraction),
    errorContainer = lerp(start.errorContainer, stop.errorContainer, fraction),
    onErrorContainer = lerp(start.onErrorContainer, stop.onErrorContainer, fraction),
    outline = lerp(start.outline, stop.outline, fraction),
    outlineVariant = lerp(start.outlineVariant, stop.outlineVariant, fraction),
    scrim = lerp(start.scrim, stop.scrim, fraction),
    surfaceBright = lerp(start.surfaceBright, stop.surfaceBright, fraction),
    surfaceDim = lerp(start.surfaceDim, stop.surfaceDim, fraction),
    surfaceContainer = lerp(start.surfaceContainer, stop.surfaceContainer, fraction),
    surfaceContainerHigh = lerp(start.surfaceContainerHigh, stop.surfaceContainerHigh, fraction),
    surfaceContainerHighest =
        lerp(start.surfaceContainerHighest, stop.surfaceContainerHighest, fraction),
    surfaceContainerLow = lerp(start.surfaceContainerLow, stop.surfaceContainerLow, fraction),
    surfaceContainerLowest =
        lerp(start.surfaceContainerLowest, stop.surfaceContainerLowest, fraction),
    primaryFixed = lerp(start.primaryFixed, stop.primaryFixed, fraction),
    primaryFixedDim = lerp(start.primaryFixedDim, stop.primaryFixedDim, fraction),
    onPrimaryFixed = lerp(start.onPrimaryFixed, stop.onPrimaryFixed, fraction),
    onPrimaryFixedVariant = lerp(start.onPrimaryFixedVariant, stop.onPrimaryFixedVariant, fraction),
    secondaryFixed = lerp(start.secondaryFixed, stop.secondaryFixed, fraction),
    secondaryFixedDim = lerp(start.secondaryFixedDim, stop.secondaryFixedDim, fraction),
    onSecondaryFixed = lerp(start.onSecondaryFixed, stop.onSecondaryFixed, fraction),
    onSecondaryFixedVariant =
        lerp(start.onSecondaryFixedVariant, stop.onSecondaryFixedVariant, fraction),
    tertiaryFixed = lerp(start.tertiaryFixed, stop.tertiaryFixed, fraction),
    tertiaryFixedDim = lerp(start.tertiaryFixedDim, stop.tertiaryFixedDim, fraction),
    onTertiaryFixed = lerp(start.onTertiaryFixed, stop.onTertiaryFixed, fraction),
    onTertiaryFixedVariant =
        lerp(start.onTertiaryFixedVariant, stop.onTertiaryFixedVariant, fraction),
)
