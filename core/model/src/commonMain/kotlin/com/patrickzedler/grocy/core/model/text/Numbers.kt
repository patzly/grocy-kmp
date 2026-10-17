package com.patrickzedler.grocy.core.model.text

import kotlin.math.abs
import kotlin.math.round

/**
 * [value] with at most [maxDecimals] decimal places and without trailing zeros, e.g. "1.5" or
 * "2"; computed the same on every platform.
 */
fun formatDecimal(value: Double, maxDecimals: Int, decimalSeparator: Char = '.'): String {
    val decimals = maxDecimals.coerceIn(0, MAX_DECIMALS)
    val factor = TEN_POWERS[decimals]
    val scaled = round(abs(value) * factor).toLong()
    val integer = scaled / factor.toLong()
    val fraction = (scaled % factor.toLong()).toString().padStart(decimals, '0').trimEnd('0')
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    return if (fraction.isEmpty()) "$sign$integer" else "$sign$integer$decimalSeparator$fraction"
}

/** A number typed by the user, with "." or "," as decimal separator; null if it is none. */
fun parseDecimal(text: String): Double? {
    val trimmed = text.trim().replace(',', '.')
    if (!DECIMAL.matches(trimmed)) return null
    return trimmed.toDoubleOrNull()
}

/** Number of decimal places the user typed, e.g. 2 for "1,25". */
fun decimalPlaces(text: String): Int {
    val trimmed = text.trim().replace(',', '.')
    val separator = trimmed.indexOf('.')
    return if (separator < 0) 0 else trimmed.length - separator - 1
}

/** The decimal separator of the app language, "," in German. */
expect fun platformDecimalSeparator(): Char

private const val MAX_DECIMALS = 10

private val TEN_POWERS = DoubleArray(MAX_DECIMALS + 1) { power ->
    var result = 1.0
    repeat(power) { result *= 10 }
    result
}

// No exponents, no thousands separators, at most one decimal separator
private val DECIMAL = Regex("""-?(\d+([.]\d*)?|[.]\d+)""")
