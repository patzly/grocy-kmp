package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

internal actual fun scrollableAlertDialogProperties(): DialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = false,
        usePlatformInsets = false,
        // Material scrim opacity
        scrimColor = Color.Black.copy(alpha = 0.32f),
    )
