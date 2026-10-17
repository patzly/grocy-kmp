package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Composable
internal actual fun BrowserBackIntegration(
    navigator: Navigator,
    onUserAgentTransition: () -> Unit,
) {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
        ?: return
    // Has a frame clock, which awaitFrame needs
    val scope = rememberCoroutineScope()
    val currentOnUserAgentTransition by rememberUpdatedState(onUserAgentTransition)

    val input = remember(scope, navigator) {
        BrowserHistoryInput(
            history = WindowBrowserHistory(navigator),
            // One scene per back stack entry, as long as no scene shows several
            initialIndex = navigator.backStack.lastIndex,
            scope = scope,
            awaitFrame = { withFrameNanos {} },
            isScreen = { it is SceneInfo<*> },
            onUserAgentTransition = { currentOnUserAgentTransition() },
        )
    }
    DisposableEffect(dispatcher, input) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
    LaunchedEffect(input) {
        snapshotFlow { navigator.backStack.toList() }
            .collect { input.onBackStackChanged(it.lastIndex) }
    }
}

internal actual fun restoredBackStack(codec: BackStackCodec): List<NavKey>? =
    historyStateBackStack()?.let(codec::decode)

/** Entries store `{ index, stack }`, the URL shows the screen as hash. */
@OptIn(ExperimentalWasmJsInterop::class)
private class WindowBrowserHistory(private val navigator: Navigator) : BrowserHistory {

    init {
        trackPageLeave()
    }

    override val currentIndex: Int?
        get() = historyStateIndex().takeIf { it >= 0 }

    override val popStates: Flow<PopState> = callbackFlow {
        val listener = addPopStateListener { index, hasUserAgentTransition ->
            trySend(PopState(index.takeIf { it >= 0 }, hasUserAgentTransition))
        }
        awaitClose { removePopStateListener(listener) }
    }

    // A move while the page is being left, e.g. closing the dialog of a language change that
    // reloads, would cancel that navigation

    override fun pushState(index: Int) {
        if (!isLeavingPage()) write(index, ::historyPushState)
    }

    override fun replaceState(index: Int) {
        if (!isLeavingPage()) write(index, ::historyReplaceState)
    }

    override fun go(delta: Int) {
        // go(0) would reload the page
        if (delta != 0 && !isLeavingPage()) historyGo(delta)
    }

    private fun write(index: Int, state: (index: Int, stack: String?, hash: String) -> Unit) {
        val codec = navigator.backStackCodec
        val backStack = navigator.backStack.toList()
        state(index, codec?.encode(backStack), codec?.hash(backStack).orEmpty())
    }
}

/** Marks the page as being left from beforeunload until it shows again (back-forward cache). */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        if (window.grocyLeavingPage !== undefined) return;
        window.grocyLeavingPage = false;
        window.addEventListener('beforeunload', () => { window.grocyLeavingPage = true; });
        window.addEventListener('pageshow', () => { window.grocyLeavingPage = false; });
    }""",
)
private external fun trackPageLeave()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.grocyLeavingPage === true")
private external fun isLeavingPage(): Boolean

/** Index stored in the current history entry, -1 if it holds something else. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        const state = history.state;
        return state !== null && typeof state === 'object' && Number.isInteger(state.index)
            ? state.index
            : -1;
    }""",
)
private external fun historyStateIndex(): Int

/** Back stack stored in the current history entry, null if it holds none. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        const state = history.state;
        return state !== null && typeof state === 'object' && typeof state.stack === 'string'
            ? state.stack
            : null;
    }""",
)
private external fun historyStateBackStack(): String?

/** An empty [hash] removes the hash, path and query stay. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(index, stack, hash) => history.pushState(
        { index, stack },
        '',
        location.pathname + location.search + hash,
    )""",
)
private external fun historyPushState(index: Int, stack: String?, hash: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(index, stack, hash) => history.replaceState(
        { index, stack },
        '',
        location.pathname + location.search + hash,
    )""",
)
private external fun historyReplaceState(index: Int, stack: String?, hash: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(delta) => history.go(delta)")
private external fun historyGo(delta: Int)

/** hasUAVisualTransition is true when the browser animated the move, e.g. Chrome on Android. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(callback) => {
        const listener = (event) => {
            const state = event.state;
            const index = state !== null && typeof state === 'object'
                && Number.isInteger(state.index) ? state.index : -1;
            callback(index, event.hasUAVisualTransition === true);
        };
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
