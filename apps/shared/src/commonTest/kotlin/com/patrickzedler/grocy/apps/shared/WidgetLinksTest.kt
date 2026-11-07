package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetLinksTest {

    private val day = epochDayOf("2026-11-07")!!

    @Test
    fun taskLinksOpenTheTasksAndTheirFormOnItsOwn() {
        assertEquals(TasksRoute, WidgetLinks.routeOfLink(WidgetLinks.tasks()))
        assertEquals(TaskRoute(inList = false), WidgetLinks.routeOfLink(WidgetLinks.newTask()))
        assertEquals(TaskRoute(5, inList = false), WidgetLinks.routeOfLink(WidgetLinks.task(5)))
    }

    @Test
    fun calendarLinksOpenItsViewOnTheDay() {
        assertEquals(
            CalendarRoute(CalendarView.MONTH, "2026-11-07"),
            WidgetLinks.routeOfLink(WidgetLinks.calendar(CalendarView.MONTH, day)),
        )
    }

    @Test
    fun eventLinksOpenWhatTheEventIsAbout() {
        val product = CalendarEvent(CalendarEventKind.PRODUCT, "Milk", day, objectId = 2)
        val task = CalendarEvent(CalendarEventKind.TASK, "Mow", day, objectId = 4)
        val recipe = CalendarEvent(CalendarEventKind.MEAL_PLAN_RECIPE, "Soup", day, objectId = 3)
        val note = CalendarEvent(CalendarEventKind.MEAL_PLAN_NOTE, "Guests", day)

        assertEquals(
            CalendarRoute(CalendarView.DAY, "2026-11-07", productId = 2),
            WidgetLinks.routeOfLink(WidgetLinks.event(product)),
        )
        assertEquals(TaskRoute(4, inList = false), WidgetLinks.routeOfLink(WidgetLinks.event(task)))
        assertEquals(RecipeRoute(3), WidgetLinks.routeOfLink(WidgetLinks.event(recipe)))
        assertEquals(MealPlanRoute("2026-11-07"), WidgetLinks.routeOfLink(WidgetLinks.event(note)))
    }

    @Test
    fun anythingElseOpensNothing() {
        listOf(
            "",
            "settings",
            "task/abc",
            "task/1/2",
            "tasks/1",
            "calendar/YEAR/2026-11-07",
            "calendar/DAY/someday",
            "event/PRODUCT/2026-11-07",
            "event/MEAL_PLAN_RECIPE/2026-11-07/x",
        ).forEach { assertNull(WidgetLinks.routeOfLink(it), it) }
    }
}
