package com.patrickzedler.grocy.core.design.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

/** Duration and curve of Android's own keyboard animation, so both platforms move alike. */
private val KeyboardAnimationSpec = tween<Float>(
    durationMillis = 285,
    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)

/** Part of the app container the keyboard covers, in CSS pixels (which are dp in Compose). */
private val keyboardOverlap = MutableStateFlow(0f)

actual val WindowInsets.Companion.keyboard: WindowInsets
    @Composable get() {
        val target by keyboardOverlap.collectAsState()
        val overlap = animateFloatAsState(
            targetValue = target,
            animationSpec = KeyboardAnimationSpec,
            label = "keyboard",
        )
        return remember(overlap) { AnimatedKeyboardInsets(overlap) }
    }

/** Reads the animated overlap only during layout, so the animation never recomposes. */
private class AnimatedKeyboardInsets(private val overlapDp: State<Float>) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection) = 0
    override fun getTop(density: Density) = 0
    override fun getRight(density: Density, layoutDirection: LayoutDirection) = 0
    override fun getBottom(density: Density) = with(density) { overlapDp.value.dp.roundToPx() }
}

/**
 * Tracks the browser keyboard over the element [viewportContainerId], the container of the
 * Compose viewport. Call it once before the viewport is created.
 */
fun observeBrowserKeyboard(viewportContainerId: String) {
    observeKeyboard(viewportContainerId) { containerBottom, keyboardTop ->
        keyboardOverlap.value = keyboardOverlap(
            containerBottom = containerBottom,
            keyboardTop = keyboardTop.takeUnless { it.isNaN() },
        )
    }
}

/**
 * Chromium browsers report the keyboard through the VirtualKeyboard API, which also keeps the
 * page from resizing, so the app animates the change itself. Other browsers fall back to the
 * visual viewport, which shrinks above the keyboard. The keyboard top is NaN while it is hidden.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(containerId, callback) => {
        const keyboard = navigator.virtualKeyboard;
        const viewport = window.visualViewport;
        const report = () => {
            const container = document.getElementById(containerId);
            if (!container) return;
            let keyboardTop = NaN;
            if (keyboard) {
                const rect = keyboard.boundingRect;
                if (rect.height > 0) keyboardTop = rect.y;
            } else if (viewport && viewport.scale <= 1.01) {
                keyboardTop = viewport.offsetTop + viewport.height;
            }
            callback(container.getBoundingClientRect().bottom, keyboardTop);
        };
        if (keyboard) {
            keyboard.overlaysContent = true;
            keyboard.addEventListener('geometrychange', report);
        } else if (viewport) {
            viewport.addEventListener('resize', report);
            viewport.addEventListener('scroll', report);
        }
        window.addEventListener('resize', report);
        report();
    }""",
)
private external fun observeKeyboard(
    containerId: String,
    callback: (containerBottom: Double, keyboardTop: Double) -> Unit,
)
