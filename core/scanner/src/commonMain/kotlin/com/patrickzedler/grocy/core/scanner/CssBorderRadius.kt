package com.patrickzedler.grocy.core.scanner

import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Outline

/**
 * CSS border-radius of an outline, for platform views that cannot be clipped by Compose.
 * [density] converts pixels to CSS pixels (dp); outlines without rounded corners give "0".
 */
internal fun Outline.toCssBorderRadius(density: Float): String =
    (this as? Outline.Rounded)?.roundRect?.toCssBorderRadius(density) ?: "0"

/** Corners clockwise from top left, as CSS border-radius expects them. */
internal fun RoundRect.toCssBorderRadius(density: Float): String {
    val rect = this
    return listOf(
        rect.topLeftCornerRadius.x,
        rect.topRightCornerRadius.x,
        rect.bottomRightCornerRadius.x,
        rect.bottomLeftCornerRadius.x,
    ).joinToString(" ") { "${(it / density).roundToCss()}px" }
}

private fun Float.roundToCss(): String {
    val rounded = kotlin.math.round(this * 100) / 100
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else "$rounded"
}
