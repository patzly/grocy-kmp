package com.patrickzedler.grocy.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Whether the user works with a mouse or trackpad right now, or with touch or a stylus. Follows
 * the pointer that was used last, so a laptop with touchscreen switches with the user's hand.
 */
@Stable
class PointerKindState(usesMouse: Boolean) {
    var usesMouse by mutableStateOf(usesMouse)
        internal set
}

/** The [PointerKindState] of the app, provided at its root. */
val LocalPointerKind = staticCompositionLocalOf { PointerKindState(usesMouse = false) }

/** Starts with what the device offers: the primary pointer of a browser, a PC with Android. */
@Composable
fun rememberPointerKindState(): PointerKindState {
    val usesMouse = prefersMouse()
    return remember { PointerKindState(usesMouse) }
}

/** Updates [state] with every pointer seen in this layout and its children. */
fun Modifier.trackPointerKind(state: PointerKindState): Modifier = pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            // Initial pass: children consuming the event (buttons, lists) must not hide it
            val event = awaitPointerEvent(PointerEventPass.Initial)
            // Only presses and wheel turns: browsers follow a tap with emulated mouse moves
            if (event.type == PointerEventType.Press || event.type == PointerEventType.Scroll) {
                event.changes.firstOrNull()?.let { change ->
                    usesMouse(change.type)?.let { state.usesMouse = it }
                }
            }
        }
    }
}

/** True for a mouse, false for touch and styluses, null when unknown. */
internal fun usesMouse(type: PointerType): Boolean? = when (type) {
    PointerType.Mouse -> true
    PointerType.Touch, PointerType.Stylus, PointerType.Eraser -> false
    else -> null
}

/** True when the device's main pointer is a mouse or trackpad. */
@Composable
internal expect fun prefersMouse(): Boolean
