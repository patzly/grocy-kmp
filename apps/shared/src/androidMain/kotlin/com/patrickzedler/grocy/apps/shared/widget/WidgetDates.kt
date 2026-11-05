package com.patrickzedler.grocy.apps.shared.widget

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import android.icu.util.ULocale
import com.patrickzedler.grocy.core.model.text.yearOfDay
import java.util.Date

// Dates as Google's widgets write them, in the app's language (Locale.getDefault)

/** "Sat, Nov 7". */
internal fun formatWeekdayDate(epochDay: Long): String = format("MMMEd", epochDay)

/** "5 Thu", the date of a narrow schedule. */
internal fun formatDayWeekday(epochDay: Long): String = format("dEEE", epochDay)

/** "M", the narrow name of the weekday. */
internal fun formatWeekdayNarrow(epochDay: Long): String = format("EEEEE", epochDay)

/** "November", with the year only when it is not [today]'s. */
internal fun formatMonthTitle(epochDay: Long, today: Long): String =
    format(if (yearOfDay(epochDay) == yearOfDay(today)) "LLLL" else "yMMMM", epochDay)

// The day itself, not its midnight in another time zone
private fun format(skeleton: String, epochDay: Long): String =
    DateFormat.getInstanceForSkeleton(skeleton, ULocale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(epochDay * MILLIS_PER_DAY))

private const val MILLIS_PER_DAY = 86_400_000L
