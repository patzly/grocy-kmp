package com.patrickzedler.grocy.core.design.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

// Android leaves touch mode with the first key press and returns with the next touch
@Composable
internal actual fun rememberKeyboardInput(): () -> Boolean {
    val inputModeManager = LocalInputModeManager.current
    return remember(inputModeManager) { { inputModeManager.inputMode == InputMode.Keyboard } }
}
