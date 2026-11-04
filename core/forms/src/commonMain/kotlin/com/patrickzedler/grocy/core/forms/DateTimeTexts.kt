package com.patrickzedler.grocy.core.forms

import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.model.text.formatRelativeTime
import com.patrickzedler.grocy.core.model.text.formatRelativeTimeLabel
import com.patrickzedler.grocy.core.model.text.timeOfDayOf
import com.patrickzedler.grocy.core.model.text.timeSpanBetween

/** The time of Grocy's "2026-10-22 18:05:00" as "18:05", null without one or for [dateOnly]. */
fun timeOfDayText(timestamp: String, dateOnly: Boolean = false): String? {
    if (dateOnly) return null
    return timeOfDayOf(timestamp)?.let { (hour, minute) ->
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
}

/** [timestamp] as date and, unless [dateOnly], time; as it is when it is no date. */
fun dateTimeText(timestamp: String, dateOnly: Boolean = false): String {
    val day = epochDayOf(timestamp) ?: return timestamp
    return listOfNotNull(formatDate(day), timeOfDayText(timestamp, dateOnly)).joinToString(" ")
}

/** [timestamp] with how far it lies from [today], e.g. "Oct 22, 2026 18:05 (3 days ago)". */
fun dateTimeWithRelative(timestamp: String, today: Long, dateOnly: Boolean = false): String {
    val day = epochDayOf(timestamp) ?: return timestamp
    val relative = formatRelativeTime(timeSpanBetween(today, day))
    return "${dateTimeText(timestamp, dateOnly)} ($relative)"
}

/** How far [timestamp] lies from [today] as a label, e.g. "Tomorrow 18:05" in a chip. */
fun relativeDateTimeLabel(timestamp: String, today: Long, dateOnly: Boolean = false): String {
    val day = epochDayOf(timestamp) ?: return timestamp
    return listOfNotNull(
        formatRelativeTimeLabel(timeSpanBetween(today, day)),
        timeOfDayText(timestamp, dateOnly),
    ).joinToString(" ")
}
