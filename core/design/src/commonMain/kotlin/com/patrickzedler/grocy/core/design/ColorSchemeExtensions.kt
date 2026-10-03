package com.patrickzedler.grocy.core.design

import androidx.compose.material3.ColorScheme
import com.materialkolor.ktx.harmonize

/** Shifts the error colors slightly towards primary, so red fits every seed color. */
internal fun ColorScheme.harmonizeError(): ColorScheme = copy(
    error = error.harmonize(primary),
    onError = onError.harmonize(primary),
    errorContainer = errorContainer.harmonize(primary),
    onErrorContainer = onErrorContainer.harmonize(primary),
)
