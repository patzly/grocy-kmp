package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.window.DialogProperties

internal actual fun scrollableAlertDialogProperties(): DialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
    )
