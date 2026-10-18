package com.patrickzedler.grocy.core.data.util

import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why a download did not work; offline is only a note when local data exists. */
enum class RefreshError { OFFLINE, FAILED }

data class RefreshState(
    val isRefreshing: Boolean = false,
    val error: RefreshError? = null,
    /** True once a download worked, so the data is the server's and not just still empty. */
    val loadedOnce: Boolean = false,
)

/**
 * Runs the download of a screen (e.g. on open and on pull to refresh) and reports its state;
 * one download at a time, shown at least [MINIMUM_LOADING_DURATION] so indicators do not flicker.
 */
class RefreshController(
    private val scope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val download: suspend () -> Unit,
) {

    private val mutableState = MutableStateFlow(RefreshState())
    val state: StateFlow<RefreshState> = mutableState.asStateFlow()

    fun refresh() {
        if (mutableState.value.isRefreshing) return
        mutableState.update { it.copy(isRefreshing = true, error = null) }
        scope.launch {
            val error = try {
                withMinimumDuration(MINIMUM_LOADING_DURATION, timeSource) { download() }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (_: GrocyApiException.Unreachable) {
                RefreshError.OFFLINE
            } catch (_: GrocyApiException) {
                RefreshError.FAILED
            }
            mutableState.update {
                RefreshState(error = error, loadedOnce = it.loadedOnce || error == null)
            }
        }
    }

    /** The error was shown, e.g. as snackbar. */
    fun onErrorShown() {
        mutableState.update { it.copy(error = null) }
    }
}
