package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.MutatePriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

class TooltipPressTrackerTest {

    private val time = TestTimeSource()
    private val tracker = TooltipPressTracker(time)
    private val longPressTimeout = 400.milliseconds

    private fun trigger(priority: MutatePriority = MutatePriority.PreventUserInput) =
        tracker.triggerOf(priority, longPressTimeout)

    @Test
    fun hoverIsMouseHover() {
        assertEquals(TooltipTrigger.MouseHover, trigger(MutatePriority.UserInput))
    }

    @Test
    fun focusWithoutAnyPressIsKeyboardFocus() {
        assertEquals(TooltipTrigger.KeyboardFocus, trigger())
    }

    @Test
    fun focusAtTheStartOfAPressIsAClick() {
        tracker.onPress()
        time += 20.milliseconds

        assertEquals(TooltipTrigger.PointerFocus, trigger())
    }

    @Test
    fun focusRightAfterAClickIsTheClick() {
        tracker.onPress()
        time += 80.milliseconds
        tracker.onRelease()
        time += 10.milliseconds

        assertEquals(TooltipTrigger.PointerFocus, trigger())
    }

    @Test
    fun showAfterTheTimeoutWhilePressedIsALongPress() {
        tracker.onPress()
        time += 400.milliseconds

        assertEquals(TooltipTrigger.LongPress, trigger())
    }

    @Test
    fun focusLongAfterTheLastClickIsKeyboardFocus() {
        tracker.onPress()
        tracker.onRelease()
        time += 2_000.milliseconds

        assertEquals(TooltipTrigger.KeyboardFocus, trigger())
    }
}
