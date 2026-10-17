package com.patrickzedler.grocy.core.model.text

// navigator.language is the app language (index.html sets it)
actual fun platformDecimalSeparator(): Char = decimalSeparatorOfBrowserLanguage().firstOrNull() ?: '.'

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        try {
            return (1.5).toLocaleString(navigator.language).replace(/[0-9]/g, '').charAt(0);
        } catch (e) {
            return '.';
        }
    }""",
)
private external fun decimalSeparatorOfBrowserLanguage(): String
