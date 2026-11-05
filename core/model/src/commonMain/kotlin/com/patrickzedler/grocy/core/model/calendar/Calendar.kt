package com.patrickzedler.grocy.core.model.calendar

import com.patrickzedler.grocy.core.model.User
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import com.patrickzedler.grocy.core.model.masterdata.Battery
import com.patrickzedler.grocy.core.model.masterdata.Product
import com.patrickzedler.grocy.core.model.masterdata.StockAmount
import com.patrickzedler.grocy.core.model.recipes.DAYS_PER_WEEK
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.tasks.Task
import com.patrickzedler.grocy.core.model.text.addMonths
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.firstDayOfMonth
import com.patrickzedler.grocy.core.model.text.lastDayOfMonth
import com.patrickzedler.grocy.core.model.text.timeOfDayOf
import com.patrickzedler.grocy.core.model.text.weekdayOf
import com.patrickzedler.grocy.core.model.text.yearOfDay
import kotlinx.serialization.Serializable

/** Grocy's "Configure colors" of the calendar: a user setting per kind of events. */
enum class CalendarColorSetting(
    val key: String,
    val valueOf: (UserSettings) -> String,
    val isAvailable: (ServerConfig) -> Boolean,
) {
    PRODUCTS(
        "calendar_color_products",
        { it.calendarColorProducts },
        { it.stock && it.stockBestBeforeDateTracking },
    ),
    TASKS("calendar_color_tasks", { it.calendarColorTasks }, { it.tasks }),
    CHORES("calendar_color_chores", { it.calendarColorChores }, { it.chores }),
    BATTERIES("calendar_color_batteries", { it.calendarColorBatteries }, { it.batteries }),
    MEAL_PLAN(
        "calendar_color_meal_plan",
        { it.calendarColorMealPlan },
        { it.recipes && it.recipesMealPlan },
    ),
}

/** What an event of the calendar is about, in the order Grocy lists them. */
enum class CalendarEventKind(val color: CalendarColorSetting) {
    PRODUCT(CalendarColorSetting.PRODUCTS),
    TASK(CalendarColorSetting.TASKS),
    CHORE(CalendarColorSetting.CHORES),
    BATTERY(CalendarColorSetting.BATTERIES),
    MEAL_PLAN_RECIPE(CalendarColorSetting.MEAL_PLAN),
    MEAL_PLAN_NOTE(CalendarColorSetting.MEAL_PLAN),
    MEAL_PLAN_PRODUCT(CalendarColorSetting.MEAL_PLAN),
}

/**
 * An event of Grocy's calendar. [time] is "HH:MM", null for what takes the whole day; [section]
 * the meal plan's, [assignedTo] the display name of the user a chore is assigned to.
 */
data class CalendarEvent(
    val kind: CalendarEventKind,
    val name: String,
    val epochDay: Long,
    val time: String? = null,
    val section: String? = null,
    val assignedTo: String? = null,
    val recipeId: Int? = null,
)

/** What the calendar is built from, all of it as last downloaded. */
class CalendarData(
    val config: ServerConfig,
    val products: List<Product> = emptyList(),
    val stock: List<StockAmount> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val chores: List<CurrentChore> = emptyList(),
    val users: List<User> = emptyList(),
    val currentBatteries: List<CurrentBattery> = emptyList(),
    val batteries: List<Battery> = emptyList(),
    val mealPlan: List<MealPlanEntry> = emptyList(),
    val sections: List<MealPlanSection> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
)

/**
 * The events of Grocy's `CalendarService`, computed from the stored data so the calendar works
 * offline (the server only offers them translated, in its iCal file). Whole days come first, then
 * by time and kind. Grocy's end of a day (23:59:59) counts as a whole day. What has no date, never
 * comes (the year 2999) or whose object is gone, is left out.
 */
fun calendarEventsOf(data: CalendarData): List<CalendarEvent> {
    val available = CalendarColorSetting.entries.filter { it.isAvailable(data.config) }
    fun has(setting: CalendarColorSetting) = setting in available
    return buildList {
        if (has(CalendarColorSetting.PRODUCTS)) addAll(productEvents(data))
        if (has(CalendarColorSetting.TASKS)) addAll(taskEvents(data))
        if (has(CalendarColorSetting.CHORES)) addAll(choreEvents(data))
        if (has(CalendarColorSetting.BATTERIES)) addAll(batteryEvents(data))
        if (has(CalendarColorSetting.MEAL_PLAN)) addAll(mealPlanEvents(data))
    }.sortedWith(
        compareBy<CalendarEvent> { it.epochDay }
            .thenBy { it.time != null }
            .thenBy { it.time }
            .thenBy { it.kind }
            .thenBy { it.name.lowercase() },
    )
}

private fun productEvents(data: CalendarData): List<CalendarEvent> {
    val products = data.products.associateBy { it.id }
    return data.stock.filter { it.amount > 0 }.mapNotNull { amount ->
        val name = products[amount.productId]?.name ?: return@mapNotNull null
        val day = amount.bestBeforeDate?.let(::dayOf) ?: return@mapNotNull null
        CalendarEvent(CalendarEventKind.PRODUCT, name, day)
    }
}

private fun taskEvents(data: CalendarData): List<CalendarEvent> =
    data.tasks.filter { !it.done }.mapNotNull { task ->
        val day = task.dueDate?.let(::dayOf) ?: return@mapNotNull null
        CalendarEvent(CalendarEventKind.TASK, task.name, day)
    }

private fun choreEvents(data: CalendarData): List<CalendarEvent> {
    val users = data.users.associateBy { it.id }
    return data.chores.mapNotNull { chore ->
        val next = chore.nextEstimatedExecutionTime ?: return@mapNotNull null
        val day = dayOf(next) ?: return@mapNotNull null
        CalendarEvent(
            kind = CalendarEventKind.CHORE,
            name = chore.name,
            epochDay = day,
            time = if (chore.trackDateOnly) null else timeOf(next),
            assignedTo = chore.assignedUserId?.let { users[it]?.displayName },
        )
    }
}

private fun batteryEvents(data: CalendarData): List<CalendarEvent> {
    val batteries = data.batteries.filter { it.active }.associateBy { it.id }
    return data.currentBatteries.mapNotNull { current ->
        val name = batteries[current.batteryId]?.name ?: return@mapNotNull null
        val next = current.nextEstimatedChargeTime ?: return@mapNotNull null
        val day = dayOf(next) ?: return@mapNotNull null
        CalendarEvent(CalendarEventKind.BATTERY, name, day, time = timeOf(next))
    }
}

private fun mealPlanEvents(data: CalendarData): List<CalendarEvent> {
    val sections = data.sections.associateBy { it.id }
    val recipes = data.recipes.associateBy { it.id }
    val products = data.products.associateBy { it.id }
    return data.mealPlan.mapNotNull { entry ->
        val day = dayOf(entry.day) ?: return@mapNotNull null
        val (kind, name) = when (entry.type) {
            MealPlanEntryType.RECIPE ->
                CalendarEventKind.MEAL_PLAN_RECIPE to recipes[entry.recipeId]?.name
            MealPlanEntryType.NOTE -> CalendarEventKind.MEAL_PLAN_NOTE to entry.note
            MealPlanEntryType.PRODUCT ->
                CalendarEventKind.MEAL_PLAN_PRODUCT to products[entry.productId]?.name
        }
        val section = sections[entry.sectionId]
        CalendarEvent(
            kind = kind,
            name = name ?: return@mapNotNull null,
            epochDay = day,
            time = section?.timeInfo?.trim()?.ifEmpty { null },
            section = section?.name?.trim()?.ifEmpty { null },
            recipeId = entry.recipeId.takeIf { kind == CalendarEventKind.MEAL_PLAN_RECIPE },
        )
    }
}

private fun dayOf(dateTime: String): Long? = epochDayOf(dateTime.trim().take(ISO_DATE_LENGTH))
    ?.takeIf { yearOfDay(it) < NEVER_YEAR }

private fun timeOf(dateTime: String): String? =
    timeOfDayOf(dateTime)?.takeUnless { dateTime.trim().endsWith(WHOLE_DAY_TIME) }
        ?.let { (hour, minute) ->
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        }

/**
 * Grocy's views of the calendar, its list as a schedule that goes on, in Google Calendar's order.
 */
@Serializable
enum class CalendarView { DAY, WEEK, MONTH, SCHEDULE }

/** The first day of the week of [epochDay], [firstDayOfWeek] from 0 (Sunday) to 6. */
fun calendarWeekStart(epochDay: Long, firstDayOfWeek: Int): Long =
    epochDay - (weekdayOf(epochDay) - firstDayOfWeek).mod(DAYS_PER_WEEK)

/** The whole weeks that hold the month of [epochDay], each by its first day. */
fun calendarMonthWeeks(epochDay: Long, firstDayOfWeek: Int): List<Long> {
    val last = lastDayOfMonth(epochDay)
    return generateSequence(calendarWeekStart(firstDayOfMonth(epochDay), firstDayOfWeek)) {
        it + DAYS_PER_WEEK
    }.takeWhile { it <= last }.toList()
}

/** The day to show when going [steps] periods of [view] on from [epochDay]. */
fun calendarDayAfter(view: CalendarView, epochDay: Long, steps: Int): Long = when (view) {
    CalendarView.DAY -> epochDay + steps
    CalendarView.WEEK, CalendarView.SCHEDULE -> epochDay + steps * DAYS_PER_WEEK
    CalendarView.MONTH -> addMonths(epochDay, steps)
}

/** [text] as "#rrggbb" if it is a color like Grocy's color inputs give, with or without "#". */
fun normalizedHexColor(text: String): String? {
    val digits = text.trim().removePrefix("#").lowercase()
    val isValid = digits.length == HEX_COLOR_DIGITS && digits.all { it in "0123456789abcdef" }
    return if (isValid) "#$digits" else null
}

/** The opaque ARGB value of a color [text], null for anything else. */
fun argbOfHexColor(text: String): Long? =
    normalizedHexColor(text)?.drop(1)?.toLong(radix = 16)?.plus(OPAQUE)

private const val ISO_DATE_LENGTH = 10
// Grocy's "whole day" time, and its date of what never comes
private const val WHOLE_DAY_TIME = "23:59:59"
private const val NEVER_YEAR = 2999
private const val HEX_COLOR_DIGITS = 6
private const val OPAQUE = 0xFF000000L
