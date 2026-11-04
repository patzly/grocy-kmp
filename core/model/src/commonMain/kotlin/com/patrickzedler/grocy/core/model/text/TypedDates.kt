package com.patrickzedler.grocy.core.model.text

/** The order in which a date is written. */
enum class DateFieldOrder(internal val separator: String, internal val pattern: String) {
    DAY_MONTH_YEAR(".", "DD.MM.YY"),
    MONTH_DAY_YEAR("/", "MM/DD/YY"),
    YEAR_MONTH_DAY("-", "YY-MM-DD"),
}

/** The digits of the longest typed date, with a four-digit year. */
const val MAX_TYPED_DATE_DIGITS = 8

/** The digits of the longest typed date with its time. */
const val MAX_TYPED_DATE_TIME_DIGITS = 12

/** The order [formatted], the text of a date with day 22, month 11 and year 2003, is written in. */
fun dateOrderOf(formatted: String): DateFieldOrder {
    val year = formatted.indexOf(PROBE_YEAR)
    val day = formatted.indexOf(PROBE_DAY)
    val month = formatted.indexOf(PROBE_MONTH).takeIf { it >= 0 }
        ?: formatted.indexOfFirst { it.isLetter() }
    return when {
        year in 0 until day -> DateFieldOrder.YEAR_MONTH_DAY
        month in 0 until day -> DateFieldOrder.MONTH_DAY_YEAR
        else -> DateFieldOrder.DAY_MONTH_YEAR
    }
}

/** The order the language of the app writes dates in. */
fun languageDateOrder(): DateFieldOrder = dateOrderOf(formatDate(PROBE_EPOCH_DAY))

/** How a date of [order] is typed, for messages: "DD.MM.YY", with time "DD.MM.YY HH:MM". */
fun typedDatePattern(order: DateFieldOrder, withTime: Boolean): String =
    if (withTime) "${order.pattern} HH:MM" else order.pattern

/**
 * Grocy's date (with its time for [withTime], midnight when none is typed) of typed [digits] in
 * [order], with a two or four-digit year; "" for nothing typed, null while it names no date.
 */
fun grocyDateOfTyped(digits: String, order: DateFieldOrder, withTime: Boolean): String? {
    if (digits.isEmpty()) return ""
    if (!digits.all { it.isDigit() }) return null
    val dateLength = dateLengthOf(digits.length, withTime) ?: return null
    val date = isoDateOfTyped(digits.take(dateLength), order) ?: return null
    if (!withTime) return date
    val time = digits.drop(dateLength)
    if (time.isEmpty()) return "$date 00:00:00"
    return normalizedDateTime("$date ${time.take(2)}:${time.drop(2)}")
}

/** The digits to type for Grocy's date [text]; what is no date keeps its digits. */
fun typedDateOf(text: String, order: DateFieldOrder, withTime: Boolean): String {
    val trimmed = text.trim()
    val date = trimmed.take(ISO_DATE_LENGTH)
    if (epochDayOf(date) == null) return trimmed.filter { it.isDigit() }
    val (year, month, day) = date.split('-')
    // A year that two digits do not reach back from keeps all four, e.g. never overdue
    val shownYear = if (year.startsWith(CENTURY)) year.drop(2) else year
    val digits = when (order) {
        DateFieldOrder.DAY_MONTH_YEAR -> day + month + shownYear
        DateFieldOrder.MONTH_DAY_YEAR -> month + day + shownYear
        DateFieldOrder.YEAR_MONTH_DAY -> shownYear + month + day
    }
    if (!withTime) return digits
    val (hour, minute) = timeOfDayOf(trimmed) ?: return digits
    return digits + hour.toString().padStart(2, '0') + minute.toString().padStart(2, '0')
}

/** Where typed digits of [length] show a separator, by position in the digits. */
fun typedDateSeparators(
    length: Int,
    order: DateFieldOrder,
    withTime: Boolean,
): List<Pair<Int, String>> {
    val dateLength = dateLengthOf(length, withTime) ?: length.coerceAtMost(MAX_TYPED_DATE_DIGITS)
    val yearFirstLong = order == DateFieldOrder.YEAR_MONTH_DAY && dateLength > SHORT_DATE_DIGITS
    val first = if (yearFirstLong) 4 else 2
    return buildList {
        if (length > first) add(first to order.separator)
        if (length > first + 2) add(first + 2 to order.separator)
        if (withTime && length > dateLength) add(dateLength to " ")
        if (withTime && length > dateLength + 2) add(dateLength + 2 to ":")
    }
}

/** The digits of the date in [length] typed digits, null where it cannot be complete yet. */
private fun dateLengthOf(length: Int, withTime: Boolean): Int? = when {
    length == SHORT_DATE_DIGITS || length == MAX_TYPED_DATE_DIGITS -> length
    !withTime -> null
    length == SHORT_DATE_DIGITS + TIME_DIGITS -> SHORT_DATE_DIGITS
    length == MAX_TYPED_DATE_TIME_DIGITS -> MAX_TYPED_DATE_DIGITS
    else -> null
}

private fun isoDateOfTyped(digits: String, order: DateFieldOrder): String? {
    val yearLength = digits.length - 4
    val (year, month, day) = when (order) {
        DateFieldOrder.DAY_MONTH_YEAR ->
            Triple(digits.drop(4), digits.substring(2, 4), digits.take(2))
        DateFieldOrder.MONTH_DAY_YEAR ->
            Triple(digits.drop(4), digits.take(2), digits.substring(2, 4))
        DateFieldOrder.YEAR_MONTH_DAY -> Triple(
            digits.take(yearLength),
            digits.substring(yearLength, yearLength + 2),
            digits.takeLast(2),
        )
    }
    val fullYear = if (year.length == 2) CENTURY + year else year
    return "$fullYear-$month-$day".takeIf { normalizedDate(it) != null }
}

private const val PROBE_EPOCH_DAY = 12_378L
private const val PROBE_YEAR = "2003"
private const val PROBE_MONTH = "11"
private const val PROBE_DAY = "22"
private const val CENTURY = "20"
private const val ISO_DATE_LENGTH = 10
private const val SHORT_DATE_DIGITS = 6
private const val TIME_DIGITS = 4
