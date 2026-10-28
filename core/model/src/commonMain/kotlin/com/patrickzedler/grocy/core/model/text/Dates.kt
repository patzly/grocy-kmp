package com.patrickzedler.grocy.core.model.text

import kotlin.math.abs
import kotlin.math.roundToInt

/** Units of [TimeSpan], the ones Grocy's web frontend (moment.js) rounds to. */
enum class TimeSpanUnit { DAY, MONTH, YEAR }

/** A rounded amount of time; negative values lie in the past. */
data class TimeSpan(val value: Int, val unit: TimeSpanUnit)

/** Days since 1970-01-01 of the date "YYYY-MM-DD" (time after it is ignored); null if invalid. */
fun epochDayOf(isoDate: String): Long? {
    val match = ISO_DATE.matchAt(isoDate, 0) ?: return null
    val (year, month, day) = match.destructured.toList().map { it.toInt() }
    if (month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
    return daysFromCivil(year, month, day)
}

/** The date [epochDay] (days since 1970-01-01) as "YYYY-MM-DD". */
fun isoDateOf(epochDay: Long): String {
    val (year, month, day) = civilFromDays(epochDay)
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-" +
        day.toString().padStart(2, '0')
}

/** Hour and minute of Grocy's "YYYY-MM-DD HH:MM[:SS]"; null without a valid time. */
fun timeOfDayOf(dateTime: String): Pair<Int, Int>? {
    val match = TIME_OF_DAY.find(dateTime.trim()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return (hour to minute).takeIf { hour in 0..23 && minute in 0..59 }
}

/**
 * How far [epochDay] lies from [today], rounded like Grocy's "3 days ago": days below 26 days,
 * months below 320 days, years from there on.
 */
fun timeSpanBetween(today: Long, epochDay: Long): TimeSpan {
    val days = epochDay - today
    val span = durationOf(abs(days).toDouble())
    return if (days < 0) span.copy(value = -span.value) else span
}

/** [days] rounded to the unit Grocy shows for durations, e.g. 60 days as 2 months. */
fun durationOf(days: Double): TimeSpan {
    val absolute = abs(days)
    return when {
        absolute < DAYS_UP_TO_MONTHS -> TimeSpan(absolute.roundToInt(), TimeSpanUnit.DAY)
        absolute < DAYS_UP_TO_YEARS -> roundedSpan(absolute / DAYS_PER_MONTH, TimeSpanUnit.MONTH)
        else -> roundedSpan(absolute / DAYS_PER_YEAR, TimeSpanUnit.YEAR)
    }
}

// Never "0 months": the unit starts at one
private fun roundedSpan(units: Double, unit: TimeSpanUnit) =
    TimeSpan(units.roundToInt().coerceAtLeast(1), unit)

/** Today's date in the device's time zone as days since 1970-01-01. */
expect fun todayEpochDay(): Long

/** Now in the device's time zone, as milliseconds since 1970-01-01 00:00 of that zone. */
expect fun localNowMillis(): Long

/** The local time [localMillis] (see [localNowMillis]) as Grocy's "YYYY-MM-DD HH:MM:SS". */
fun grocyDateTimeOf(localMillis: Long): String {
    val day = localMillis.floorDiv(MILLIS_PER_DAY)
    val secondOfDay = localMillis.mod(MILLIS_PER_DAY) / MILLIS_PER_SECOND
    val (year, month, dayOfMonth) = civilFromDays(day)
    fun two(value: Long) = value.toString().padStart(2, '0')
    return "${year.toString().padStart(4, '0')}-${two(month.toLong())}-" +
        "${two(dayOfMonth.toLong())} ${two(secondOfDay / 3600)}:" +
        "${two(secondOfDay / 60 % 60)}:${two(secondOfDay % 60)}"
}

/** [epochDay] as medium date of the app language, e.g. "Oct 24, 2026". */
expect fun formatDate(epochDay: Long): String

/** [span] relative to now in the app language, e.g. "yesterday" or "in 3 months". */
expect fun formatRelativeTime(span: TimeSpan): String

/**
 * [span] like [formatRelativeTime] as a label of its own, e.g. "Tomorrow" in a chip: the
 * platforms format it for the middle of a sentence ("morgen" in German).
 */
fun formatRelativeTimeLabel(span: TimeSpan): String = capitalizeStart(formatRelativeTime(span))

/** [text] with its first letter capitalized, e.g. "in 3 Tagen" as "In 3 Tagen". */
internal fun capitalizeStart(text: String): String = text.replaceFirstChar { it.titlecase() }

/** [span] as duration in the app language, e.g. "2 months". */
expect fun formatDuration(span: TimeSpan): String

// Howard Hinnant's days_from_civil, exact for the proleptic Gregorian calendar
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val monthIndex = (month + 9) % 12
    val dayOfYear = (153 * monthIndex + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097 + dayOfEra - 719_468
}

// Howard Hinnant's civil_from_days, the inverse of daysFromCivil
private fun civilFromDays(epochDay: Long): Triple<Long, Int, Int> {
    val z = epochDay + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val dayOfEra = z - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthIndex = (5 * dayOfYear + 2) / 153
    val day = (dayOfYear - (153 * monthIndex + 2) / 5 + 1).toInt()
    val month = (if (monthIndex < 10) monthIndex + 3 else monthIndex - 9).toInt()
    val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
    return Triple(year, month, day)
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private const val MILLIS_PER_DAY = 86_400_000L
private const val MILLIS_PER_SECOND = 1_000L

private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")

// The time after a date: "2026-10-24 08:30" or "2026-10-24 08:30:00"
private val TIME_OF_DAY = Regex("""^\d{4}-\d{2}-\d{2}[ T](\d{2}):(\d{2})(:\d{2})?$""")

// moment.js: "a month" from 26 days on, "a year" from 320 days on
private const val DAYS_UP_TO_MONTHS = 25.5
private const val DAYS_UP_TO_YEARS = 319.5
private const val DAYS_PER_MONTH = 30.436875
private const val DAYS_PER_YEAR = 365.2425
