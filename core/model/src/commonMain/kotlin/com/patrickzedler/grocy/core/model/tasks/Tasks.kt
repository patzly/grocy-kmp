package com.patrickzedler.grocy.core.model.tasks

import com.patrickzedler.grocy.core.model.text.epochDayOf

/** A task of Grocy (`objects/tasks`), [dueDate] as Grocy's "YYYY-MM-DD". */
data class Task(
    val id: Int,
    val name: String,
    val description: String? = null,
    val dueDate: String? = null,
    val done: Boolean = false,
    val doneTimestamp: String? = null,
    val categoryId: Int? = null,
    val assignedToUserId: Int? = null,
    val userfields: Map<String, String?> = emptyMap(),
)

/** How close a task's due date is, Grocy's row highlighting and status filter. */
enum class TaskDueStatus { OVERDUE, DUE_TODAY, DUE_SOON }

/**
 * The status of a task due on [dueDate] like Grocy's tasks page on the day [today]; null when it
 * has no date or is not due yet. Grocy compares whole days, a task is due until its day ends.
 */
fun taskDueStatusOf(dueDate: String?, today: Long, dueSoonDays: Int): TaskDueStatus? {
    val day = dueDate?.let(::epochDayOf) ?: return null
    return when {
        day < today -> TaskDueStatus.OVERDUE
        day == today -> TaskDueStatus.DUE_TODAY
        dueSoonDays > 0 && day <= today + dueSoonDays -> TaskDueStatus.DUE_SOON
        else -> null
    }
}

/** Grocy's messages above its tasks; [dueSoon] counts the ones due today as well. */
data class TasksSummary(
    val count: Int = 0,
    val overdue: Int = 0,
    val dueToday: Int = 0,
    val dueSoon: Int = 0,
)

/** Counts the tasks still to do of [tasks] like Grocy's messages. */
fun tasksSummaryOf(tasks: List<Task>, today: Long, dueSoonDays: Int): TasksSummary {
    val open = tasks.filter { !it.done }
    val statuses = open.map { taskDueStatusOf(it.dueDate, today, dueSoonDays) }
    val dueToday = statuses.count { it == TaskDueStatus.DUE_TODAY }
    return TasksSummary(
        count = open.size,
        overdue = statuses.count { it == TaskDueStatus.OVERDUE },
        dueToday = dueToday,
        dueSoon = dueToday + statuses.count { it == TaskDueStatus.DUE_SOON },
    )
}
