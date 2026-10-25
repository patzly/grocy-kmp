package com.patrickzedler.grocy.core.design.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Compose on the web keeps no input mode, so the page watches keys and pointers itself
@Composable
internal actual fun rememberKeyboardInput(): () -> Boolean = remember {
    installKeyboardInputListeners()
    ::isKeyboardInput
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        if (window.grocyKeyboardInput !== undefined) return;
        window.grocyKeyboardInput = false;
        window.addEventListener('keydown', () => { window.grocyKeyboardInput = true; }, true);
        window.addEventListener('pointerdown', () => { window.grocyKeyboardInput = false; }, true);
    }""",
)
private external fun installKeyboardInputListeners()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.grocyKeyboardInput === true")
private external fun isKeyboardInput(): Boolean
