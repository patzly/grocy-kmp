package com.patrickzedler.grocy.core.model

/** How the app looks, chosen in the general settings. */
data class AppearanceSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val color: AppColor = AppColor.STATIC,
    /** Hue of the seed color for [AppColor.STATIC], 0 to 360 degrees. */
    val hue: Float = DEFAULT_HUE,
    /** Variant of the color scheme for [AppColor.STATIC]. */
    val style: AppPaletteStyle = AppPaletteStyle.TONAL_SPOT,
    val contrast: AppContrast = AppContrast.STANDARD,
) {
    companion object {
        /** Grocy green. */
        const val DEFAULT_HUE = 154f
    }
}
