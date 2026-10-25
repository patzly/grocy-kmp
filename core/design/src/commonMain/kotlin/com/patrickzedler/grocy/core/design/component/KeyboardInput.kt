package com.patrickzedler.grocy.core.design.component

import androidx.compose.runtime.Composable

/**
 * Tells whether the last input was a key press rather than a pointer, read when focus moves:
 * only then it came from the keyboard. Screens that open focus their first button by themselves.
 */
@Composable
internal expect fun rememberKeyboardInput(): () -> Boolean
