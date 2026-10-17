package com.patrickzedler.grocy.core.navigation

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHistory
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** A move through the browser history, e.g. by the back gesture of an installed web app. */
internal data class PopState(
    /** Index [BrowserHistoryInput] stored with the entry, null for entries of others. */
    val index: Int?,
    /** True if the browser animated the change already, e.g. with a back gesture preview. */
    val hasUserAgentTransition: Boolean,
)

/**
 * The browser's history API as far as [BrowserHistoryInput] needs it. Entries written by
 * [pushState] and [replaceState] also get the app's current back stack and its URL.
 */
internal interface BrowserHistory {

    /** Index stored with the current entry, null if [BrowserHistoryInput] did not create it. */
    val currentIndex: Int?

    val popStates: Flow<PopState>

    fun pushState(index: Int)

    fun replaceState(index: Int)

    /** Moves [delta] entries; browsers do that asynchronously and report it in [popStates]. */
    fun go(delta: Int)
}

/**
 * Mirrors the app's back stack into the browser history, so the back button and back gesture of
 * the browser navigate inside the app and only leave it from the first screen, like on Android.
 *
 * Every screen gets one entry storing its index. A dialog or popup on top gets one more entry,
 * so back closes it first. [isScreen] tells screens from such overlays.
 *
 * @param initialIndex index of the app's screen when it starts, above 0 when a reload restored
 *   the back stack from the browser history.
 * @param awaitFrame waits for the next frame; overlay changes are applied after it, because
 *   screens being replaced briefly look like overlays.
 * @param onUserAgentTransition called before a back the browser has animated already.
 */
internal class BrowserHistoryInput(
    private val history: BrowserHistory,
    private val initialIndex: Int,
    private val scope: CoroutineScope,
    private val awaitFrame: suspend () -> Unit,
    private val isScreen: (NavigationEventInfo) -> Boolean,
    private val onUserAgentTransition: () -> Unit,
) : NavigationEventInput() {

    /** Where the browser is, or will be once it has finished the moves this input started. */
    private var browserIndex = 0

    /** Where the browser should be for the current app state. */
    private var targetIndex = 0

    /** Indices the browser arrives at after [BrowserHistory.go] of this input, no user action. */
    private val pendingArrivals = ArrayDeque<Int>()

    private var popStateJob: Job? = null
    private var syncJob: Job? = null

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        // Undispatched, so no pop state is missed from here on
        popStateJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            history.popStates.collect(::onPopState)
        }
        val restoredIndex = history.currentIndex
        if (restoredIndex != null && restoredIndex > initialIndex) {
            // A reload on a dialog's entry, or one the app could not restore: back to the screen
            expectArrival(initialIndex)
            history.go(initialIndex - restoredIndex)
        } else {
            history.replaceState(initialIndex)
        }
        browserIndex = initialIndex
        targetIndex = initialIndex
    }

    override fun onRemoved() {
        popStateJob?.cancel()
        syncJob?.cancel()
        pendingArrivals.clear()
    }

    override fun onHistoryChanged(history: NavigationEventHistory) {
        val current = history.mergedHistory.getOrNull(history.currentIndex)
        // Without an enabled handler (current is null) the app is on its first screen
        val appIndex = history.currentIndex.coerceAtLeast(0)
        if (current == null || isScreen(current)) {
            targetIndex = appIndex
            // Right away, so the browser keeps a screenshot of the screen it leaves
            syncJob?.cancel()
            sync()
        } else {
            targetIndex = appIndex + 1
            scheduleSync()
        }
    }

    /**
     * Rewrites the current entry when the back stack changed without a move, e.g. the login
     * replacing its screen by the start screen. On the first screen no handler is enabled, so
     * [onHistoryChanged] does not tell. Only when [lastIndex] of the back stack is the entry's
     * index, so a half-done navigation never writes its back stack into the previous entry.
     */
    fun onBackStackChanged(lastIndex: Int) {
        if (lastIndex == targetIndex && targetIndex == browserIndex && pendingArrivals.isEmpty()) {
            history.replaceState(targetIndex)
        }
    }

    private fun onPopState(popState: PopState) {
        val index = popState.index ?: return
        if (pendingArrivals.isNotEmpty()) {
            if (pendingArrivals.first() == index) {
                pendingArrivals.removeFirst()
                if (pendingArrivals.isEmpty()) scheduleSync()
                return
            }
            // The user moved while the browser was still busy with a move of this input
            pendingArrivals.clear()
        }

        val steps = browserIndex - index
        browserIndex = index
        if (steps > 0) {
            if (popState.hasUserAgentTransition) onUserAgentTransition()
            repeat(steps) { dispatchOnBackCompleted() }
        }
        // Reverts forward moves, which the app does not know, and restores entries of backs
        // that nobody handled
        scheduleSync()
    }

    private fun scheduleSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            awaitFrame()
            sync()
        }
    }

    private fun sync() {
        // Continues once the browser has arrived where this input sent it
        if (pendingArrivals.isNotEmpty()) return
        val target = targetIndex
        when {
            target > browserIndex -> {
                for (index in browserIndex + 1..target) history.pushState(index)
                browserIndex = target
            }

            target < browserIndex -> {
                expectArrival(target)
                history.go(target - browserIndex)
                browserIndex = target
            }

            // Same index, maybe another screen (e.g. after replacing the back stack)
            else -> history.replaceState(target)
        }
    }

    private fun expectArrival(index: Int) {
        pendingArrivals.addLast(index)
        // Browsers ignore moves past the end of the history without telling
        scope.launch {
            delay(ARRIVAL_TIMEOUT)
            if (pendingArrivals.remove(index) && pendingArrivals.isEmpty()) scheduleSync()
        }
    }

    private companion object {
        val ARRIVAL_TIMEOUT = 1.seconds
    }
}
