package com.patrickzedler.grocy.core.model.text

import java.text.DecimalFormatSymbols

// Locale.getDefault() is the app language (AndroidAppLanguageStore sets it)
actual fun platformDecimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
