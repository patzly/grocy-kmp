package com.patrickzedler.grocy.core.model.text

import java.text.NumberFormat
import java.util.Currency

// Locale.getDefault() is the app language (AndroidAppLanguageStore sets it)

actual fun formatCurrency(value: Double, currency: String, decimals: Int): String = try {
    NumberFormat.getCurrencyInstance().apply {
        this.currency = Currency.getInstance(currency)
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)
} catch (_: IllegalArgumentException) {
    // A code Java does not know
    fallbackCurrency(value, currency, decimals)
}

actual fun formatPercent(fraction: Double): String =
    NumberFormat.getPercentInstance().format(fraction)
