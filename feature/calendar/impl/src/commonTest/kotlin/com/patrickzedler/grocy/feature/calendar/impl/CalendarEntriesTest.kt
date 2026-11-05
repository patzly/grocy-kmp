package com.patrickzedler.grocy.feature.calendar.impl

import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarEntriesTest {

    private val day = "2026-11-04"
    private val epochDay = epochDayOf(day)!!

    private fun event(kind: CalendarEventKind, recipeId: Int? = null) =
        CalendarEvent(kind, "Name", epochDay, recipeId = recipeId)

    @Test
    fun eventsLeadWhereGrocyLinksThem() {
        assertEquals(StockOverviewRoute, routeOf(event(CalendarEventKind.PRODUCT)))
        assertEquals(TasksRoute, routeOf(event(CalendarEventKind.TASK)))
        assertEquals(ChoresOverviewRoute, routeOf(event(CalendarEventKind.CHORE)))
        assertEquals(BatteriesOverviewRoute, routeOf(event(CalendarEventKind.BATTERY)))
        assertEquals(RecipeRoute(5), routeOf(event(CalendarEventKind.MEAL_PLAN_RECIPE, 5)))
        assertEquals(MealPlanRoute(day), routeOf(event(CalendarEventKind.MEAL_PLAN_NOTE)))
        assertEquals(MealPlanRoute(day), routeOf(event(CalendarEventKind.MEAL_PLAN_PRODUCT)))
    }

    @Test
    fun smallScreensStartWithTheDayLikeGrocy() {
        assertEquals(CalendarView.DAY, defaultCalendarView(767.dp))
        assertEquals(CalendarView.MONTH, defaultCalendarView(768.dp))
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
    fun monthRowsShareTheWindowButKeepRoomForAChip() {
        assertEquals(162.dp, monthRowHeight(1000.dp, weeks = 5))
        assertEquals(72.dp, monthRowHeight(500.dp, weeks = 6))
    }

    @Test
    fun theDaysStayBelowTheAppBarOnceTheyStick() {
        assertEquals(0, stickyShift(offset = 40, padding = 200))
        assertEquals(0, stickyShift(offset = 0, padding = 200))
        assertEquals(50, stickyShift(offset = -50, padding = 200))
        assertEquals(200, stickyShift(offset = -500, padding = 200))
    }
}
