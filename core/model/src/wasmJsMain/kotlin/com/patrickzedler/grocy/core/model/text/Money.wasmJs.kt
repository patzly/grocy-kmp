@file:OptIn(ExperimentalWasmJsInterop::class)

package com.patrickzedler.grocy.core.model.text

// navigator.language is the app language (index.html sets it)

actual fun formatCurrency(value: Double, currency: String, decimals: Int): String =
    formatCurrencyInBrowser(value, currency, decimals)
        // A code the browser does not know
        ?: fallbackCurrency(value, currency, decimals)

actual fun formatPercent(fraction: Double): String = formatPercentInBrowser(fraction)

@JsFun(
    """(value, currency, decimals) => {
        try {
            return new Intl.NumberFormat(globalThis.navigator?.language, {
                style: 'currency',
                currency: currency,
                minimumFractionDigits: decimals,
                maximumFractionDigits: decimals,
            }).format(value);
        } catch (e) {
            return null;
        }
    }""",
)
private external fun formatCurrencyInBrowser(
    value: Double,
    currency: String,
    decimals: Int,
): String?

@JsFun(
    """(fraction) => new Intl.NumberFormat(
        globalThis.navigator?.language,
        { style: 'percent' }
    ).format(fraction)""",
)
private external fun formatPercentInBrowser(fraction: Double): String
