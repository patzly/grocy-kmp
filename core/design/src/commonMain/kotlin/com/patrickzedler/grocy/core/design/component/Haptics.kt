package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

fun HapticFeedback.confirm() {
    performHapticFeedback(HapticFeedbackType.Confirm)
}
