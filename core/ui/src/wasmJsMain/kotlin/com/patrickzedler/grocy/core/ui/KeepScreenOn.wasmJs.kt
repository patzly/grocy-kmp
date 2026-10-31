package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val lock = acquireWakeLock()
        onDispose { releaseWakeLock(lock) }
    }
}

/**
 * Requests a screen wake lock and requests it again whenever the page becomes visible, browsers
 * release it when the tab is hidden; without the API (or permission) the screen just turns off.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        const state = { active: true, lock: null };
        const request = () => {
            if (!state.active || !navigator.wakeLock || document.visibilityState !== 'visible') {
                return;
            }
            navigator.wakeLock.request('screen').then((lock) => {
                if (state.active) state.lock = lock; else lock.release().catch(() => {});
            }).catch(() => {});
        };
        state.onVisibilityChange = request;
        document.addEventListener('visibilitychange', request);
        request();
        return state;
    }""",
)
private external fun acquireWakeLock(): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(state) => {
        state.active = false;
        document.removeEventListener('visibilitychange', state.onVisibilityChange);
        if (state.lock) state.lock.release().catch(() => {});
    }""",
)
private external fun releaseWakeLock(state: JsAny)
