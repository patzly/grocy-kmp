package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * The hand cursor over something clickable, as web pages show it. Compose keeps the arrow
 * everywhere, so every button, item, chip and toggle applies this. Android keeps the arrow,
 * its convention for buttons (also with a mouse on ChromeOS), the hand is for links there.
 */
fun Modifier.clickCursor(enabled: Boolean = true): Modifier =
    if (enabled && usesHandCursor) pointerHoverIcon(PointerIcon.Hand) else this

/** True where clickable things show the hand cursor: in browsers. */
internal expect val usesHandCursor: Boolean
