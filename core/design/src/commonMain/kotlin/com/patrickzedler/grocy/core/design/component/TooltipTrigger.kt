package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.MutatePriority
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Why a tooltip wants to show. */
internal enum class TooltipTrigger {
    LongPress,
    MouseHover,
    KeyboardFocus,

    /** Focus a tap or click gave the anchor, e.g. in browsers: no reason for a tooltip. */
    PointerFocus,
}

/**
 * Tells the causes of a tooltip apart, which TooltipState only reports as two priorities: hover
 * as user input, long press and focus both as preventing user input. Focus that comes with a
 * press shorter than a long press is a plain tap or click, which must not show a tooltip.
 */
internal class TooltipPressTracker(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var pressStart: TimeMark? = null
    private var release: TimeMark? = null

    fun onPress() {
        pressStart = timeSource.markNow()
        release = null
    }

    fun onRelease() {
        release = timeSource.markNow()
    }

    fun triggerOf(mutatePriority: MutatePriority, longPressTimeout: Duration): TooltipTrigger {
        if (mutatePriority == MutatePriority.UserInput) return TooltipTrigger.MouseHover
        val start = pressStart ?: return TooltipTrigger.KeyboardFocus
        val released = release
        return when {
            // Still pressed: a long press once the timeout has passed (the show comes right then)
            released == null -> if (start.elapsedNow() >= longPressTimeout * LONG_PRESS_TOLERANCE) {
                TooltipTrigger.LongPress
            } else {
                TooltipTrigger.PointerFocus
            }
            // Focus right after a tap or click
            released.elapsedNow() < CLICK_FOCUS_WINDOW -> TooltipTrigger.PointerFocus
            else -> TooltipTrigger.KeyboardFocus
        }
    }

    private companion object {
        const val LONG_PRESS_TOLERANCE = 0.9
        val CLICK_FOCUS_WINDOW = 500.milliseconds
    }
}
