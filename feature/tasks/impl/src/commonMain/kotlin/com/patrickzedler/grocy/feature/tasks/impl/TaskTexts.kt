package com.patrickzedler.grocy.feature.tasks.impl

import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatRelativeTime
import com.patrickzedler.grocy.core.model.text.formatRelativeTimeLabel
import com.patrickzedler.grocy.core.model.text.timeOfDayOf
import com.patrickzedler.grocy.core.model.text.timeSpanBetween

/** [date] as the user's date, as it is when it is no date. */
internal fun taskDateText(date: String): String =
    epochDayOf(date)?.let { formatDate(it) } ?: date

/** Grocy's "2026-10-22 18:05:00" as date and time, as it is when it is no date. */
internal fun taskDateTimeText(timestamp: String): String {
    val time = timeOfDayOf(timestamp)?.let { (hour, minute) ->
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
    return listOfNotNull(taskDateText(timestamp), time).joinToString(" ")
}

/** How far [date] lies from [today] as a label, e.g. "Tomorrow" in a chip. */
internal fun taskRelativeLabel(date: String, today: Long): String {
    val day = epochDayOf(date) ?: return date
    return formatRelativeTimeLabel(timeSpanBetween(today, day))
}

/** [date] with how far it lies from [today], e.g. "Oct 22, 2026 (3 days ago)". */
internal fun taskDateWithRelative(date: String, today: Long): String {
    val day = epochDayOf(date) ?: return date
    return "${formatDate(day)} (${formatRelativeTime(timeSpanBetween(today, day))})"
}
