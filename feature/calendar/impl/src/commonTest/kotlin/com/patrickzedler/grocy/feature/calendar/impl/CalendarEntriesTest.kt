package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.feature.batteries.api.BatteryTrackingRoute
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarEntriesTest {

    private val day = "2026-11-04"
    private val epochDay = epochDayOf(day)!!

    private fun event(kind: CalendarEventKind, objectId: Int? = 5) =
        CalendarEvent(kind, "Name", epochDay, objectId = objectId)

    @Test
    fun eventsOpenWhatTheyAreAbout() {
        assertEquals(TaskRoute(5, inList = false), routeOf(event(CalendarEventKind.TASK)))
        assertEquals(ChoreTrackingRoute(5, inList = false), routeOf(event(CalendarEventKind.CHORE)))
        assertEquals(
            BatteryTrackingRoute(5, inList = false),
            routeOf(event(CalendarEventKind.BATTERY)),
        )
        assertEquals(RecipeRoute(5), routeOf(event(CalendarEventKind.MEAL_PLAN_RECIPE)))
        assertEquals(MealPlanRoute(day), routeOf(event(CalendarEventKind.MEAL_PLAN_NOTE, null)))
    }

    @Test
    fun productsOpenTheirOverviewOverTheirDay() {
        val expected = CalendarRoute(CalendarView.DAY, day, productId = 5)

        assertEquals(expected, routeOf(event(CalendarEventKind.PRODUCT)))
        assertEquals(expected, routeOf(event(CalendarEventKind.MEAL_PLAN_PRODUCT)))
        assertEquals(5, event(CalendarEventKind.PRODUCT).productId)
        assertNull(event(CalendarEventKind.TASK).productId)
    }

    @Test
    fun onlyTheDayAndTheWeekAreTimeGrids() {
        assertEquals(
            setOf(CalendarView.DAY, CalendarView.WEEK),
            CalendarView.entries.filter { it.isTimeGrid }.toSet(),
        )
    }

    @Test
    fun aChipStandsAtItsTime() {
        assertEquals(
            listOf(60f, 600f),
            chipTops(listOf(60, 600), minuteHeight = 1f, chipHeight = 26f, totalHeight = 1440f),
        )
    }

    @Test
    fun chipsThatWouldOverlapFollowEachOther() {
        assertEquals(
            listOf(60f, 86f, 112f),
            chipTops(listOf(60, 60, 70), minuteHeight = 1f, chipHeight = 26f, totalHeight = 1440f),
        )
    }

    @Test
    fun chipsAtTheEndOfTheDayStayInside() {
        assertEquals(
            listOf(1388f, 1414f),
            chipTops(listOf(1430, 1439), minuteHeight = 1f, chipHeight = 26f, totalHeight = 1440f),
        )
    }

    @Test
    fun timesOfEventsBecomeMinutes() {
        assertEquals(615, minuteOfTime("10:15"))
        assertNull(minuteOfTime(null))
        assertNull(minuteOfTime("25:00"))
        assertNull(minuteOfTime("soon"))
    }

    @Test
    fun aPlaceOfLimitedRoomKeepsARowForMore() {
        assertEquals(3, shownChips(count = 3, limit = 3))
        assertEquals(2, shownChips(count = 5, limit = 3))
    }

    @Test
    fun monthRowsShareTheHeightButKeepRoomForAChip() {
        assertEquals(100.dp, monthRowHeight(508.dp, weeks = 5))
        assertEquals(72.dp, monthRowHeight(300.dp, weeks = 6))
    }

    @Test
    fun theGridScrollsToAnHourBeforeTheTime() {
        assertEquals(560, gridOffsetOf(minute = 11 * 60, hourHeight = 56f))
        assertEquals(0, gridOffsetOf(minute = 20, hourHeight = 56f))
    }
}
