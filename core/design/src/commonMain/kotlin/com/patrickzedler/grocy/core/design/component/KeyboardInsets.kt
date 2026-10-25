package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * The software keyboard as window insets: `WindowInsets.ime` on Android, animated with the
 * keyboard; on the web the part of the app the browser keyboard covers, without animation.
 *
 * Apply it as padding of the scrolling container, not as content padding: only a shrinking
 * viewport keeps the focused text field in view.
 */
expect val WindowInsets.Companion.keyboard: WindowInsets
    @Composable get

/**
 * Height of the part of a container ending at [containerBottom] that a keyboard starting at
 * [keyboardTop] covers, both in CSS pixels; [keyboardTop] is null while no keyboard is shown.
 */
internal fun keyboardOverlap(containerBottom: Double, keyboardTop: Double?): Float =
    if (keyboardTop == null) 0f else (containerBottom - keyboardTop).coerceAtLeast(0.0).toFloat()
