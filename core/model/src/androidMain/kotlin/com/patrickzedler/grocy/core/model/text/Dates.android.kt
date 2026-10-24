package com.patrickzedler.grocy.core.model.text

import android.icu.text.MeasureFormat
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs

// Locale.getDefault() is the app language (AndroidAppLanguageStore sets it), ICU follows it

actual fun todayEpochDay(): Long {
    val now = System.currentTimeMillis()
    return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), MILLIS_PER_DAY)
}

actual fun localNowMillis(): Long {
    val now = System.currentTimeMillis()
    return now + TimeZone.getDefault().getOffset(now)
}

actual fun formatDate(epochDay: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM)
        // The day itself, not its midnight in another time zone
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(epochDay * MILLIS_PER_DAY))

actual fun formatRelativeTime(span: TimeSpan): String {
    val formatter = RelativeDateTimeFormatter.getInstance(ULocale.getDefault())
    val direction = if (span.value < 0) {
        RelativeDateTimeFormatter.Direction.LAST
    } else {
        RelativeDateTimeFormatter.Direction.NEXT
    }
    return when {
        // "today", "yesterday" and "tomorrow" instead of numbers, like Intl's numeric "auto"
        span.unit == TimeSpanUnit.DAY && span.value == 0 -> formatter.format(
            RelativeDateTimeFormatter.Direction.THIS,
            RelativeDateTimeFormatter.AbsoluteUnit.DAY,
        )

        span.unit == TimeSpanUnit.DAY && abs(span.value) == 1 ->
            formatter.format(direction, RelativeDateTimeFormatter.AbsoluteUnit.DAY)

        else -> formatter.format(
            abs(span.value).toDouble(),
            direction,
            when (span.unit) {
                TimeSpanUnit.DAY -> RelativeDateTimeFormatter.RelativeUnit.DAYS
                TimeSpanUnit.MONTH -> RelativeDateTimeFormatter.RelativeUnit.MONTHS
                TimeSpanUnit.YEAR -> RelativeDateTimeFormatter.RelativeUnit.YEARS
            },
        )
    }
}

actual fun formatDuration(span: TimeSpan): String {
    val unit = when (span.unit) {
        TimeSpanUnit.DAY -> MeasureUnit.DAY
        TimeSpanUnit.MONTH -> MeasureUnit.MONTH
        TimeSpanUnit.YEAR -> MeasureUnit.YEAR
    }
    return MeasureFormat.getInstance(ULocale.getDefault(), MeasureFormat.FormatWidth.WIDE)
        .format(Measure(abs(span.value), unit))
}

private const val MILLIS_PER_DAY = 86_400_000L
