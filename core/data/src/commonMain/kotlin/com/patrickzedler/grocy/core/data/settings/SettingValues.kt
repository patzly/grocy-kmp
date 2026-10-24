package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.AppColor
import com.patrickzedler.grocy.core.model.AppContrast
import com.patrickzedler.grocy.core.model.AppPaletteStyle
import com.patrickzedler.grocy.core.model.AppTheme
import com.patrickzedler.grocy.core.model.AppearanceSettings
import com.patrickzedler.grocy.core.model.ScannerEngine

/** Stored engine name, falling back to the default for missing or unknown values. */
internal fun scannerEngineOf(storedName: String?): ScannerEngine =
    ScannerEngine.entries.firstOrNull { it.name == storedName } ?: ScannerEngine.ZXING

/**
 * Appearance from its stored parts. Missing or unknown values (e.g. from a newer version) fall
 * back to the default, a hue outside 0 to 360 degrees as well.
 */
internal fun appearanceOf(
    theme: String?,
    color: String?,
    hue: Float?,
    contrast: String?,
    style: String? = null,
): AppearanceSettings {
    val default = AppearanceSettings()
    return AppearanceSettings(
        theme = AppTheme.entries.firstOrNull { it.name == theme } ?: default.theme,
        color = AppColor.entries.firstOrNull { it.name == color } ?: default.color,
        hue = hue?.takeIf { it in 0f..360f } ?: default.hue,
        style = AppPaletteStyle.entries.firstOrNull { it.name == style } ?: default.style,
        contrast = AppContrast.entries.firstOrNull { it.name == contrast } ?: default.contrast,
    )
}
