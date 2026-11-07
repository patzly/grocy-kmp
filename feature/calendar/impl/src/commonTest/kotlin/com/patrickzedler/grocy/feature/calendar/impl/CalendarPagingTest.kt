package com.patrickzedler.grocy.feature.calendar.impl

import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarPagingTest {

    private val today = epochDayOf("2026-11-07")!!

    private fun state(vararg events: CalendarEvent) = CalendarUiState(
        view = CalendarView.LIST,
        anchor = today,
        today = today,
        firstDayOfWeek = 1,
        config = ServerConfig(),
        settings = UserSettings(),
        eventsByDay = events.groupBy { it.epochDay },
    )

    private fun event(day: Long, time: String? = null, name: String = "Event") =
        CalendarEvent(CalendarEventKind.TASK, name, day, time = time)

    @Test
    fun aPageShowsTheDayItsStepsAwayFromTheBase() {
        assertEquals(today + 3, dayOfPage(CalendarView.DAY, today, StartPage + 3))
        assertEquals(today - 7, dayOfPage(CalendarView.WEEK, today, StartPage - 1))
        assertEquals("2026-12-07", isoDateOf(dayOfPage(CalendarView.MONTH, today, StartPage + 1)))
    }

    @Test
    fun aDayIsOnThePageThatShowsIt() {
        val nextMonth = epochDayOf("2026-12-20")!!

        assertEquals(StartPage + 1, pageOfDay(CalendarView.MONTH, today, nextMonth, 1))
        assertEquals(StartPage + 2, pageOfDay(CalendarView.WEEK, today, today + 9, 1))
        assertEquals(StartPage - 4, pageOfDay(CalendarView.DAY, today, today - 4, 1))
    }

    @Test
    fun aSettledPageNeverTurnsTheCalendarAgain() {
        listOf(CalendarView.DAY, CalendarView.WEEK, CalendarView.MONTH).forEach { view ->
            (-30..30).forEach { steps ->
                val day = dayOfPage(view, today, StartPage + steps)

                assertEquals(StartPage + steps, pageOfDay(view, today, day, 1), "$view $steps")
            }
        }
    }

    @Test
    fun theListHasTodayAndTheDaysWithEventsBelowTheirWeeks() {
        val rows = listRowsOf(state(event(today - 6), event(today + 3)), nowMinute = 600)

        assertEquals(
            listOf("week-${today - 12}", "day-${today - 6}", "week-${today - 5}", "day-$today",
                "week-${today + 2}", "day-${today + 3}"),
            rows.map { it.key },
        )
    }

    @Test
    fun theNowLineFollowsWholeDaysAndWhatTookPlace() {
        val events = listOf(event(today), event(today, "09:00"), event(today, "14:00"))

        assertEquals(2, nowIndexOf(events, nowMinute = 600))
        assertEquals(1, nowIndexOf(events, nowMinute = 300))
        assertEquals(3, nowIndexOf(events, nowMinute = 900))
    }

    @Test
    fun theListScrollsToTheWeekOfTheFirstDayFromTheShownOne() {
        val rows = listRowsOf(state(event(today - 6), event(today + 3)), nowMinute = 600)

        assertEquals(2, listIndexOf(rows, today))
        assertEquals(4, listIndexOf(rows, today + 1))
        assertEquals(5, listIndexOf(rows, today + 100))
    }
}
