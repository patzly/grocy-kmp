@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.model.text

import kotlin.math.abs

// navigator.language is the app language (index.html sets it); Intl knows every language

actual fun todayEpochDay(): Long = localTodayEpochDay().toLong()

actual fun localNowMillis(): Long = localNowInBrowser().toLong()

actual fun formatDate(epochDay: Long): String =
    formatDateInBrowser(epochDay.toDouble() * MILLIS_PER_DAY)

actual fun formatRelativeTime(span: TimeSpan): String =
    formatRelativeInBrowser(span.value, span.unit.intlName)

actual fun formatDuration(span: TimeSpan): String =
    formatDurationInBrowser(abs(span.value), span.unit.intlName)

private val TimeSpanUnit.intlName: String
    get() = when (this) {
        TimeSpanUnit.DAY -> "day"
        TimeSpanUnit.MONTH -> "month"
        TimeSpanUnit.YEAR -> "year"
    }

private const val MILLIS_PER_DAY = 86_400_000.0

@JsFun(
    """() => {
        const now = new Date();
        return Math.floor(
            Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / 86400000
        );
    }""",
)
private external fun localTodayEpochDay(): Double

@JsFun("() => { const now = new Date(); return now.getTime() - now.getTimezoneOffset() * 60000; }")
private external fun localNowInBrowser(): Double

// The day itself, not its midnight in another time zone
@JsFun(
    """(millis) => new Date(millis).toLocaleDateString(
        globalThis.navigator?.language,
        { timeZone: 'UTC', dateStyle: 'medium' }
    )""",
)
private external fun formatDateInBrowser(millis: Double): String

// "auto" says "yesterday" instead of "1 day ago"
@JsFun(
    """(value, unit) => new Intl.RelativeTimeFormat(
        globalThis.navigator?.language,
        { numeric: 'auto' }
    ).format(value, unit)""",
)
private external fun formatRelativeInBrowser(value: Int, unit: String): String

@JsFun(
    """(value, unit) => new Intl.NumberFormat(
        globalThis.navigator?.language,
        { style: 'unit', unit: unit, unitDisplay: 'long' }
    ).format(value)""",
)
private external fun formatDurationInBrowser(value: Int, unit: String): String
