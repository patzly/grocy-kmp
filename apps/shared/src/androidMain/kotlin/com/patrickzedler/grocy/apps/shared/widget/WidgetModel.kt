package com.patrickzedler.grocy.apps.shared.widget

import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.calendarMonthWeeks
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.addMonths
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.firstDayOfMonth
import com.patrickzedler.grocy.core.model.text.lastDayOfMonth

/**
 * Everything the widgets show, as stored: [today] is the day they show as today, [completing]
 * the tasks whose "Mark task as completed" is under way.
 */
internal data class WidgetData(
    val isLoggedIn: Boolean,
    val config: ServerConfig,
    val settings: UserSettings,
    val tasks: List<Task>,
    val events: List<CalendarEvent>,
    val appearance: AppearanceSettings,
    val today: Long,
    val completing: Set<Int>,
)

/** A task still to do, [dueDay] its due date. */
internal data class WidgetTask(
    val id: Int,
    val name: String,
    val dueDay: Long?,
    val isOverdue: Boolean,
    val isCompleting: Boolean,
)

/**
 * The tasks to do like Grocy's tasks page sorts them by default, by due date, those without one
 * after them; at most [limit], a widget is no place for hundreds.
 */
internal fun widgetTasksOf(
    tasks: List<Task>,
    today: Long,
    completing: Set<Int>,
    limit: Int = MAX_ROWS,
): List<WidgetTask> = tasks
    .filter { !it.done }
    .map { task ->
        val day = task.dueDate?.let(::epochDayOf)
        WidgetTask(
            id = task.id,
            name = task.name,
            dueDay = day,
            isOverdue = day != null && day < today,
            isCompleting = task.id in completing,
        )
    }
    .sortedWith(
        compareBy<WidgetTask> { it.dueDay == null }
            .thenBy { it.dueDay }
            .thenBy { it.name.lowercase() },
    )
    .take(limit)

/** A row of the schedule widget. */
internal sealed interface ScheduleRow {
    /** The heading of a day; today's is the widget's title. */
    data class Day(val epochDay: Long, val isToday: Boolean) : ScheduleRow

    data class Event(val event: CalendarEvent) : ScheduleRow

    /** Today has nothing. */
    data object NothingToday : ScheduleRow
}

/**
 * Today and the days after it that have events, each with its events, like Google's schedule
 * widget; today always, also without events. At most [limit] events, the rest is in the app.
 */
internal fun scheduleRowsOf(
    events: List<CalendarEvent>,
    today: Long,
    limit: Int = MAX_ROWS,
): List<ScheduleRow> {
    val byDay = events.filter { it.epochDay >= today }.take(limit).groupBy { it.epochDay }
    return buildList {
        add(ScheduleRow.Day(today, isToday = true))
        val todays = byDay[today].orEmpty()
        if (todays.isEmpty()) add(ScheduleRow.NothingToday)
        todays.forEach { add(ScheduleRow.Event(it)) }
        byDay.keys.filter { it > today }.sorted().forEach { day ->
            add(ScheduleRow.Day(day, isToday = false))
            byDay.getValue(day).forEach { add(ScheduleRow.Event(it)) }
        }
    }
}

/** A day of the month widget; [isOutside] of the month it shows. */
internal data class MonthCell(
    val epochDay: Long,
    val isToday: Boolean,
    val isOutside: Boolean,
    val events: List<CalendarEvent>,
)

/** The weeks of the month of [anchor] with the days of other months that fill them. */
internal fun monthCellsOf(
    anchor: Long,
    today: Long,
    firstDayOfWeek: Int,
    events: List<CalendarEvent>,
): List<List<MonthCell>> {
    val first = firstDayOfMonth(anchor)
    val last = lastDayOfMonth(anchor)
    val byDay = events.groupBy { it.epochDay }
    return calendarMonthWeeks(anchor, firstDayOfWeek).map { start ->
        (0 until DAYS_PER_WEEK).map { offset ->
            val day = start + offset
            MonthCell(
                epochDay = day,
                isToday = day == today,
                isOutside = day < first || day > last,
                events = byDay[day].orEmpty(),
            )
        }
    }
}

/**
 * The month the month widget shows: [offset] months from today's, as the user stepped on
 * [steppedOn]. On a later day it shows today's month again, like Google's widget.
 */
internal fun shownMonthOf(offset: Int, steppedOn: Long?, today: Long): Long =
    addMonths(firstDayOfMonth(today), shownMonthOffset(offset, steppedOn, today))

/** The steps away from today's month that still count on [today]. */
internal fun shownMonthOffset(offset: Int, steppedOn: Long?, today: Long): Int =
    if (steppedOn == today) offset else 0

/** How many events fit below a day's number in a cell [cellHeight] dp high, at most four. */
internal fun monthChipPlaces(cellHeight: Float): Int =
    ((cellHeight - MONTH_DAY_HEIGHT) / (MONTH_CHIP_HEIGHT + MONTH_CHIP_GAP)).toInt()
        .coerceIn(0, MAX_MONTH_CHIPS)

/** How many of [count] events a cell with [places] shows as chips, keeping one place for dots. */
internal fun shownMonthChips(count: Int, places: Int): Int =
    if (count <= places) count else (places - 1).coerceAtLeast(0)

/**
 * [name] with spaces that never break, so a chip too narrow cuts the line like Google's widgets
 * instead of breaking it after the first word.
 */
internal fun unbroken(name: String): String = name.replace(' ', ' ')

// The month widget's cell: day number, then chips with a gap above each
internal const val MONTH_DAY_HEIGHT = 22f
internal const val MONTH_CHIP_HEIGHT = 16f
internal const val MONTH_CHIP_GAP = 2f
private const val MAX_MONTH_CHIPS = 4
private const val MAX_ROWS = 100
