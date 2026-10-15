package com.patrickzedler.grocy.core.design.extensions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** The color as opaque CSS hex value, e.g. "#11140F"; alpha is dropped. */
internal fun Color.toCssHex(): String {
    val rgb = toArgb() and 0xFFFFFF
    return "#" + rgb.toString(16).uppercase().padStart(6, '0')
}
