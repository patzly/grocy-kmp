package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.CalendarEventKind
import com.patrickzedler.grocy.core.model.calendar.CalendarView
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.calendar.impl.routeOf
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute
import kotlinx.coroutines.flow.Flow

/** What the home screen widgets of Android show and change, all of it read from the database. */
interface WidgetDependencies {
    val connection: Flow<ServerConnection?>
    val appearance: Flow<AppearanceSettings>
    val masterDataRepository: MasterDataRepository
    val tasksRepository: TasksRepository
    val calendarRepository: CalendarRepository
}

/**
 * The places of the app a widget opens, as the text its intent carries. Only these few exist, so
 * another app that starts the app with such a text cannot open anything else.
 */
internal object WidgetLinks {
    fun tasks(): String = TASKS

    fun newTask(): String = TASK

    fun task(id: Int): String = "$TASK/$id"

    fun calendar(view: CalendarView, epochDay: Long): String =
        "$CALENDAR/${view.name}/${isoDateOf(epochDay)}"

    fun event(event: CalendarEvent): String =
        "$EVENT/${event.kind.name}/${isoDateOf(event.epochDay)}/${event.recipeId ?: ""}"

    /** The screen [link] opens, null for anything a widget does not create. */
    fun routeOfLink(link: String): Route? {
        val parts = link.split('/')
        return when (parts.first()) {
            TASKS -> TasksRoute.takeIf { parts.size == 1 }
            TASK -> when (parts.size) {
                1 -> TaskRoute(inList = false)
                2 -> parts[1].toIntOrNull()?.let { TaskRoute(it, inList = false) }
                else -> null
            }
            CALENDAR -> {
                val view = CalendarView.entries.find { it.name == parts.getOrNull(1) }
                val day = parts.getOrNull(2)?.let(::epochDayOf)
                if (parts.size == 3 && view != null && day != null) {
                    CalendarRoute(view, isoDateOf(day))
                } else {
                    null
                }
            }
            EVENT -> eventRouteOf(parts)
            else -> null
        }
    }

    private fun eventRouteOf(parts: List<String>): Route? {
        if (parts.size != 4) return null
        val kind = CalendarEventKind.entries.find { it.name == parts[1] } ?: return null
        val day = epochDayOf(parts[2]) ?: return null
        val recipeId = parts[3].ifEmpty { null }?.let { it.toIntOrNull() ?: return null }
        return routeOf(CalendarEvent(kind, name = "", epochDay = day, recipeId = recipeId))
    }

    private const val TASKS = "tasks"
    private const val TASK = "task"
    private const val CALENDAR = "calendar"
    private const val EVENT = "event"
}
