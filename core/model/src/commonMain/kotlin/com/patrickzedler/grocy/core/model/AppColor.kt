package com.patrickzedler.grocy.core.model

enum class AppColor {
    /** Color scheme generated from a hue. */
    STATIC,

    /** Wallpaper colors on Android 12 and newer, falls back to [STATIC] elsewhere. */
    DYNAMIC,
}
