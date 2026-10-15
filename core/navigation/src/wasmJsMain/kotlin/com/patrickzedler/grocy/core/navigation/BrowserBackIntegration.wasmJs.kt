package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.navigation3.scene.SceneInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Composable
internal actual fun BrowserBackIntegration(onUserAgentTransition: () -> Unit) {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
        ?: return
    // Has a frame clock, which awaitFrame needs
    val scope = rememberCoroutineScope()
    val currentOnUserAgentTransition by rememberUpdatedState(onUserAgentTransition)

    DisposableEffect(dispatcher, scope) {
        val input = BrowserHistoryInput(
            history = WindowBrowserHistory,
            scope = scope,
            awaitFrame = { withFrameNanos {} },
            isScreen = { it is SceneInfo<*> },
            onUserAgentTransition = { currentOnUserAgentTransition() },
        )
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private object WindowBrowserHistory : BrowserHistory {

    override val currentIndex: Int?
        get() = historyStateIndex().takeIf { it >= 0 }

    override val popStates: Flow<PopState> = callbackFlow {
        val listener = addPopStateListener { index, hasUserAgentTransition ->
            trySend(PopState(index.takeIf { it >= 0 }, hasUserAgentTransition))
        }
        awaitClose { removePopStateListener(listener) }
    }

    override fun pushState(index: Int) = historyPushState(index)

    override fun replaceState(index: Int) = historyReplaceState(index)

    override fun go(delta: Int) {
        // go(0) would reload the page
        if (delta != 0) historyGo(delta)
    }
}

/** Index stored in the current history entry, -1 if it holds something else. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Number.isInteger(history.state) ? history.state : -1")
private external fun historyStateIndex(): Int

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(index) => history.pushState(index, '')")
private external fun historyPushState(index: Int)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(index) => history.replaceState(index, '')")
private external fun historyReplaceState(index: Int)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(delta) => history.go(delta)")
private external fun historyGo(delta: Int)

/** hasUAVisualTransition is true when the browser animated the move, e.g. Chrome on Android. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(callback) => {
        const listener = (event) => callback(
            Number.isInteger(event.state) ? event.state : -1,
            event.hasUAVisualTransition === true,
        );
        window.addEventListener('popstate', listener);
        return listener;
    }""",
)
private external fun addPopStateListener(
    callback: (index: Int, hasUserAgentTransition: Boolean) -> Unit,
): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(listener) => window.removeEventListener('popstate', listener)")
private external fun removePopStateListener(listener: JsAny)
