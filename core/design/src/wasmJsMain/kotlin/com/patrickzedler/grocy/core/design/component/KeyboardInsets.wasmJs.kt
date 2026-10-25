package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

/** Part of the app container the keyboard covers, in CSS pixels (which are dp in Compose). */
private val keyboardOverlap = MutableStateFlow(0f)

actual val WindowInsets.Companion.keyboard: WindowInsets
    @Composable get() {
        val overlap = keyboardOverlap.collectAsState()
        return remember(overlap) { BrowserKeyboardInsets(overlap) }
    }

/** Reads the overlap only during layout, so a change relayouts without recomposing. */
private class BrowserKeyboardInsets(private val overlapDp: State<Float>) : WindowInsets {
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
 * Browsers that follow the viewport's `interactive-widget=resizes-content` (Chromium, Firefox)
 * shrink the page above the keyboard themselves, so nothing is covered and the app just lays
 * out anew, which keeps the focused field in view. Safari only shrinks the visual viewport;
 * the part of the app below it is reported at once. Browsers expose no keyboard animation to
 * follow: an own one never matched the keyboard's and let the page background flash above it.
 * The keyboard top is NaN while the page is zoomed, where the visual viewport says nothing
 * about the keyboard.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(containerId, callback) => {
        const viewport = window.visualViewport;
        const report = () => {
            const container = document.getElementById(containerId);
            if (!container) return;
            let keyboardTop = NaN;
            if (viewport && viewport.scale <= 1.01) {
                keyboardTop = viewport.offsetTop + viewport.height;
            }
            callback(container.getBoundingClientRect().bottom, keyboardTop);
        };
        if (viewport) {
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
