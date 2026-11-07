package com.patrickzedler.grocy.apps.shared.widget

import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.epochDayOf
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetModelTest {

    private val today = epochDayOf("2026-11-05")!!

    private fun event(day: String, name: String = "Event") =
        CalendarEvent(CalendarEventKind.TASK, name, epochDayOf(day)!!)

    @Test
    fun theTaskListShowsTasksToDoByDueDateWithTheUndatedLast() {
        val tasks = listOf(
            Task(1, "Someday"),
            Task(2, "Next week", dueDate = "2026-11-12"),
            Task(3, "Done", dueDate = "2026-11-01", done = true),
            Task(4, "Overdue", dueDate = "2026-11-01"),
        )

        val shown = widgetTasksOf(tasks, today)

        assertEquals(listOf("Overdue", "Next week", "Someday"), shown.map { it.name })
        assertTrue(shown[0].isOverdue)
        assertFalse(shown[1].isOverdue)
    }

    @Test
    fun theTaskListShowsAtMostItsLimit() {
        val tasks = (1..5).map { Task(it, "Task $it") }

        assertEquals(3, widgetTasksOf(tasks, today, limit = 3).size)
    }

    @Test
    fun theScheduleStartsTodayAlsoWithoutEvents() {
        val rows = scheduleRowsOf(listOf(event("2026-11-04"), event("2026-11-07")), today)

        assertEquals(
            listOf(
                ScheduleRow.Day(today, isToday = true),
                ScheduleRow.NothingToday,
                ScheduleRow.Day(epochDayOf("2026-11-07")!!, isToday = false),
                ScheduleRow.Event(event("2026-11-07")),
            ),
            rows,
        )
    }

    @Test
    fun theScheduleShowsTodaysEventsBelowTheTitleAndStopsAtItsLimit() {
        val events = listOf(
            event("2026-11-05", "A"),
            event("2026-11-05", "B"),
            event("2026-11-06", "C"),
        )

        val rows = scheduleRowsOf(events, today, limit = 2)

        assertEquals(
            listOf(
                ScheduleRow.Day(today, isToday = true),
                ScheduleRow.Event(events[0]),
                ScheduleRow.Event(events[1]),
            ),
            rows,
        )
    }

    @Test
    fun theMonthFillsItsWeeksWithTheDaysAround() {
        // November 2026 from Monday on: Oct 26 to Dec 6
        val events = listOf(event("2026-11-07"))

        val weeks = monthCellsOf(today, today, firstDayOfWeek = 1, events = events)


        assertEquals(6, weeks.size)
        assertEquals(epochDayOf("2026-10-26"), weeks.first().first().epochDay)
        assertEquals(epochDayOf("2026-12-06"), weeks.last().last().epochDay)
        assertTrue(weeks.first().first().isOutside)
        val days = weeks.flatten()
        assertEquals(listOf(today), days.filter { it.isToday }.map { it.epochDay })
        assertEquals(1, days.single { it.epochDay == epochDayOf("2026-11-07") }.events.size)
    }

    @Test
    fun aSteppedMonthHoldsForTheDayAndReturnsToTodaysAfterwards() {
        assertEquals(epochDayOf("2027-01-01"), shownMonthOf(offset = 2, steppedOn = today, today))
        val tomorrow = today + 1
        assertEquals(epochDayOf("2026-11-01"), shownMonthOf(offset = 2, steppedOn = today, tomorrow))
        assertEquals(epochDayOf("2026-11-01"), shownMonthOf(offset = 0, steppedOn = null, today))
    }

    @Test
    fun aDayHasAsManyPlacesAsFitUpToFour() {
        assertEquals(0, monthChipPlaces(cellHeight = 30f))
        assertEquals(2, monthChipPlaces(cellHeight = 60f))
        assertEquals(4, monthChipPlaces(cellHeight = 400f))
    }

    @Test
    fun aFullDayKeepsOnePlaceForTheDots() {
        assertEquals(2, shownMonthChips(count = 2, places = 2))
        assertEquals(1, shownMonthChips(count = 3, places = 2))
        assertEquals(0, shownMonthChips(count = 3, places = 0))
    }

    @Test
    fun thePickerShowsTasksAndEventsOfEveryKindAroundToday() {
        val names = SampleNames(
            "Mow", "Seeds", "Water", "Descale", "Milk", "Vacuum", "Pasta", "Lunch", "Smoke",
        )

        val data = sampleWidgetData(today, names, AppearanceSettings())

        assertTrue(data.isLoggedIn)
        assertEquals(4, widgetTasksOf(data.tasks, today).size)
        assertTrue(widgetTasksOf(data.tasks, today).first().isOverdue)
        assertEquals(
            CalendarEventKind.entries.toSet() - setOf(
                CalendarEventKind.MEAL_PLAN_NOTE,
                CalendarEventKind.MEAL_PLAN_PRODUCT,
            ),
            data.events.map { it.kind }.toSet(),
        )
        assertTrue(scheduleRowsOf(data.events, today)[1] is ScheduleRow.Event)
    }

    @Test
    fun chipNamesNeverBreakAfterTheirFirstWord() {
        assertEquals("St. Martin's Day", unbroken("St. Martin's Day"))
    }

    @Test
    fun theNextMidnightIsInTheDevicesZone() {
        val berlin = TimeZone.getTimeZone("Europe/Berlin")

        val midnight = ZonedDateTime.of(2026, 11, 6, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"))

        assertEquals(midnight.toInstant().toEpochMilli(), nextMidnightMillis(today, berlin))
    }

    @Test
    fun manifestDeclaresTheWidgetsAndWhatKeepsThemCurrent() {
        val manifest = File("src/androidMain/AndroidManifest.xml").readText()

        listOf(
            "widget.TaskListWidgetReceiver",
            "widget.MonthWidgetReceiver",
            "widget.ScheduleWidgetReceiver",
            "widget.WidgetDayReceiver",
            "@xml/widget_tasks_info",
            "@xml/widget_month_info",
            "@xml/widget_schedule_info",
            "android.appwidget.action.APPWIDGET_UPDATE",
            "android.intent.action.LOCALE_CHANGED",
        ).forEach { assertTrue(it in manifest, "$it is missing") }
    }

    @Test
    fun widgetsAreKeptCurrentByTheAppNotByTheSystemsTimer() {
        listOf("tasks", "month", "schedule").forEach { name ->
            val info = File("src/androidMain/res/xml/widget_${name}_info.xml").readText()

            assertTrue("""android:updatePeriodMillis="0"""" in info, name)
        }
    }
}
