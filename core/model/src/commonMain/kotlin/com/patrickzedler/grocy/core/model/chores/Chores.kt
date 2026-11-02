package com.patrickzedler.grocy.core.model.chores

import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf

/**
 * An active chore as Grocy computes it for its overview (`/chores`). Times are Grocy's
 * "YYYY-MM-DD HH:MM:SS".
 */
data class CurrentChore(
    val choreId: Int,
    val name: String,
    val lastTrackedTime: String? = null,
    /** Null for chores without a schedule. */
    val nextEstimatedExecutionTime: String? = null,
    val trackDateOnly: Boolean = false,
    val assignedUserId: Int? = null,
    val isRescheduled: Boolean = false,
    val isReassigned: Boolean = false,
)

/** One row of Grocy's chores journal (`chores_log`). */
data class ChoreExecution(
    val id: Int,
    val choreId: Int,
    val trackedTime: String,
    val scheduledExecutionTime: String? = null,
    val doneByUserId: Int? = null,
    val skipped: Boolean = false,
    val undone: Boolean = false,
    val undoneTimestamp: String? = null,
    val createdTimestamp: String? = null,
    val userfields: Map<String, String?> = emptyMap(),
) {
    /** Grocy highlights an execution tracked after its schedule, unless it was skipped. */
    val isLate: Boolean
        get() = !skipped && scheduledExecutionTime != null && trackedTime > scheduledExecutionTime
}

/** What Grocy's "Chore overview" card shows, as the server computed it (`/chores/{id}`). */
data class ChoreDetails(
    val choreId: Int,
    val lastTracked: String? = null,
    val trackedCount: Int = 0,
    val lastDoneByName: String? = null,
    val averageFrequencyHours: Double? = null,
)

/** How close a chore's next execution is, Grocy's row highlighting and status filter. */
enum class ChoreDueStatus { OVERDUE, DUE_TODAY, DUE_SOON }

/**
 * The status of a chore due at [next] like Grocy's overview at [now] (both Grocy's "YYYY-MM-DD
 * HH:MM:SS", which compare as text); null when it is not due yet or has no schedule.
 */
fun choreDueStatusOf(next: String?, now: String, dueSoonDays: Int): ChoreDueStatus? = when {
    next == null -> null
    next < now -> ChoreDueStatus.OVERDUE
    next <= endOfDay(now) -> ChoreDueStatus.DUE_TODAY
    dueSoonDays > 0 && next <= daysAfter(now, dueSoonDays) -> ChoreDueStatus.DUE_SOON
    else -> null
}

/** Grocy's messages above its chores overview; [dueSoon] counts the ones due today as well. */
data class ChoresSummary(
    val count: Int = 0,
    val overdue: Int = 0,
    val dueToday: Int = 0,
    val dueSoon: Int = 0,
    val assignedToMe: Int = 0,
)

/** Counts [chores] like Grocy's messages; [userId] is the logged in user. */
fun choresSummaryOf(
    chores: List<CurrentChore>,
    now: String,
    dueSoonDays: Int,
    userId: Int?,
): ChoresSummary {
    val statuses = chores.map { choreDueStatusOf(it.nextEstimatedExecutionTime, now, dueSoonDays) }
    val dueToday = statuses.count { it == ChoreDueStatus.DUE_TODAY }
    return ChoresSummary(
        count = chores.size,
        overdue = statuses.count { it == ChoreDueStatus.OVERDUE },
        dueToday = dueToday,
        dueSoon = dueToday + statuses.count { it == ChoreDueStatus.DUE_SOON },
        assignedToMe = chores.count { userId != null && it.assignedUserId == userId },
    )
}

/**
 * The time Grocy's buttons track: the next schedule ([atNextSchedule], [now] for chores without
 * one) or [now], only its date for chores that track no time.
 */
fun choreTrackedTime(
    next: String?,
    trackDateOnly: Boolean,
    now: String,
    atNextSchedule: Boolean,
): String {
    val time = next.takeIf { atNextSchedule } ?: now
    return if (trackDateOnly) time.take(DATE_LENGTH) else time
}

private fun endOfDay(dateTime: String) = dateTime.take(DATE_LENGTH) + " 23:59:59"

private fun daysAfter(dateTime: String, days: Int): String {
    val day = epochDayOf(dateTime) ?: return dateTime
    return isoDateOf(day + days) + dateTime.drop(DATE_LENGTH)
}

private const val DATE_LENGTH = 10
