package com.patrickzedler.grocy.core.model.text

/**
 * [value] in the [currency] of the server (ISO 4217, e.g. "EUR") with exactly [decimals] decimal
 * places, written like the app language does, e.g. "€1.50" or "1,50 €".
 */
expect fun formatCurrency(value: Double, currency: String, decimals: Int): String

/** [fraction] as whole percent of the app language, e.g. 0.25 as "25%" or "25 %". */
expect fun formatPercent(fraction: Double): String

/** Without platform support for [currency]: the plain number and the code. */
internal fun fallbackCurrency(value: Double, currency: String, decimals: Int): String {
    val number = formatDecimal(value, decimals, platformDecimalSeparator())
    val fraction = number.substringAfter(platformDecimalSeparator(), "")
    // formatDecimal drops trailing zeros, prices keep them
    val padded = when {
        decimals == 0 -> number
        fraction.isEmpty() -> number + platformDecimalSeparator() + "0".repeat(decimals)
        else -> number + "0".repeat(decimals - fraction.length)
    }
    return "$padded $currency"
}
