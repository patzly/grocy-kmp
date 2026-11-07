package com.patrickzedler.grocy.feature.calendar.impl

import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.calendar.calendarDayAfter
import com.patrickzedler.grocy.core.model.calendar.calendarStepsBetween
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK

// The pages of a view reach far enough either way for anyone, from the middle on
internal const val PageCount = Int.MAX_VALUE
internal const val StartPage = PageCount / 2

/** The day [page] of [view] stands on, the pages counted from [base] on [StartPage]. */
internal fun dayOfPage(view: CalendarView, base: Long, page: Int): Long =
    calendarDayAfter(view, base, page - StartPage)

/** The page of [view] that shows [epochDay], the pages counted from [base] on [StartPage]. */
internal fun pageOfDay(view: CalendarView, base: Long, epochDay: Long, firstDayOfWeek: Int): Int =
    StartPage + calendarStepsBetween(view, base, epochDay, firstDayOfWeek)

/** A row of the list view. */
internal sealed interface ListRow {
    val key: String

    /** The heading of a week, "Nov 9 – 15", like Google Calendar's schedule. */
    data class Week(val start: Long) : ListRow {
        val end: Long
            get() = start + DAYS_PER_WEEK - 1
        override val key: String
            get() = "week-$start"
    }

    /** A day with its events; today's with the line of the current time before [nowIndex]. */
    data class Day(val day: CalendarDay, val nowIndex: Int?) : ListRow {
        override val key: String
            get() = "day-${day.epochDay}"
    }
}

/**
 * Grocy's list going on like Google Calendar's schedule: every day with events and today, also
 * without any, below the headings of their weeks; today has the line of the current time
 * ([nowMinute]) after what took place already.
 */
internal fun listRowsOf(state: CalendarUiState, nowMinute: Int): List<ListRow> {
    val days = (state.eventsByDay.keys + state.today).sorted()
    var week: Long? = null
    return buildList {
        days.forEach { epochDay ->
            val start = state.weekStartOf(epochDay)
            if (start != week) {
                add(ListRow.Week(start))
                week = start
            }
            val day = state.dayOf(epochDay)
            val nowIndex = if (day.isToday) nowIndexOf(day.events, nowMinute) else null
            add(ListRow.Day(day, nowIndex))
        }
    }
}

/** How many of [events] of today come before the line of [nowMinute]: whole days and the past. */
internal fun nowIndexOf(events: List<CalendarEvent>, nowMinute: Int): Int =
    events.count { (minuteOfTime(it.time) ?: -1) <= nowMinute }

/**
 * The row the list scrolls to for [epochDay]: the heading of the week of the first day from
 * [epochDay] on, so the day shows below it; the end of the list after the last day.
 */
internal fun listIndexOf(rows: List<ListRow>, epochDay: Long): Int {
    val day = rows.indexOfFirst { it is ListRow.Day && it.day.epochDay >= epochDay }
    if (day < 0) return (rows.size - 1).coerceAtLeast(0)
    return if (day > 0 && rows[day - 1] is ListRow.Week) day - 1 else day
}

/** The day a row stands for, what the list's subtitle tells. */
internal val ListRow.epochDay: Long
    get() = when (this) {
        is ListRow.Week -> start
        is ListRow.Day -> day.epochDay
    }
