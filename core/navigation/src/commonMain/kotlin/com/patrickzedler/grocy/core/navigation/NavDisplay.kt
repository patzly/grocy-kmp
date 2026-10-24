package com.patrickzedler.grocy.core.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

private const val SMALLER_SCALE = 0.9f
private const val LARGER_SCALE = 1.1f

@Composable
fun GrocyNavDisplay(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<NavKey>.() -> Unit,
) {
    val scaleSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

    // Forward: the new screen grows in, the old one grows out towards the user
    val forwardTransition = remember(fadeSpec, scaleSpec) {
        scaleFadeTransition(
            enterScale = SMALLER_SCALE,
            exitScale = LARGER_SCALE,
            fadeSpec = fadeSpec,
            scaleSpec = scaleSpec,
        )
    }
    // Back: the mirrored movement
    val backwardTransition = remember(fadeSpec, scaleSpec) {
        scaleFadeTransition(
            enterScale = LARGER_SCALE,
            exitScale = SMALLER_SCALE,
            fadeSpec = fadeSpec,
            scaleSpec = scaleSpec,
        )
    }

    // A back the browser animated already (its back gesture preview) must not animate twice
    var skipPopTransition by remember { mutableStateOf(false) }
    BrowserBackIntegration(navigator, onUserAgentTransition = { skipPopTransition = true })
    LaunchedEffect(skipPopTransition) {
        if (skipPopTransition) {
            // The pop starts in the next frame, afterwards transitions animate again
            withFrameNanos {}
            withFrameNanos {}
            skipPopTransition = false
        }
    }

    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = navigator::goBack,
        entryDecorators = listOf(
            // Keeps rememberSaveable state per entry
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes each ViewModel to its entry, cleared when the entry leaves the back stack
            rememberViewModelStoreNavEntryDecorator(),
        ),
        // Lists and their details side by side where the window is wide enough
        sceneStrategies = listOf(rememberListDetailStrategy()),
        entryProvider = entryProvider { entries() },
        transitionSpec = { forwardTransition },
        popTransitionSpec = { if (skipPopTransition) NoTransition else backwardTransition },
        predictivePopTransitionSpec = { backwardTransition },
    )
}

private val NoTransition = EnterTransition.None togetherWith ExitTransition.None

private fun scaleFadeTransition(
    enterScale: Float,
    exitScale: Float,
    fadeSpec: FiniteAnimationSpec<Float>,
    scaleSpec: FiniteAnimationSpec<Float>,
): ContentTransform {
    val enter = fadeIn(animationSpec = fadeSpec) +
            scaleIn(initialScale = enterScale, animationSpec = scaleSpec)
    val exit = fadeOut(animationSpec = fadeSpec) +
            scaleOut(targetScale = exitScale, animationSpec = scaleSpec)
    return enter togetherWith exit
}
