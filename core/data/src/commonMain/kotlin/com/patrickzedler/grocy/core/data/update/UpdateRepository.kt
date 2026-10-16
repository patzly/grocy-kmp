package com.patrickzedler.grocy.core.data.update

import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Update checks of the app: on request everywhere, and automatically where the [updater] allows
 * it, i.e. in the browser.
 */
class UpdateRepository(
    private val updater: AppUpdater,
    private val checkInterval: Duration = 4.hours,
) {

    private val mutex = Mutex()
    private val postponed = mutableSetOf<UpdateCheck.Available>()
    private val _pendingUpdate = MutableStateFlow<UpdateCheck.Available?>(null)

    /** An update an automatic check found and the user has not answered yet. */
    val pendingUpdate: StateFlow<UpdateCheck.Available?> = _pendingUpdate.asStateFlow()

    /**
     * Checks now, whenever the app returns to the foreground and every [checkInterval], until
     * cancelled. Does nothing where updates are only checked on request.
     */
    suspend fun runAutomaticChecks() {
        if (!updater.checksAutomatically) return
        val ticks = flow {
            while (true) {
                emit(Unit)
                delay(checkInterval)
            }
        }
        merge(ticks, updater.foregroundEvents).collect {
            val result = check()
            if (result is UpdateCheck.Available && result !in postponed) {
                _pendingUpdate.value = result
            }
        }
    }

    /** Checks right away, e.g. when the user asks for it; parallel requests share one check. */
    suspend fun check(): UpdateCheck = mutex.withLock { updater.check() }

    /** The user chose to update later: the same version is not offered again automatically. */
    fun postpone(update: UpdateCheck.Available) {
        postponed += update
        _pendingUpdate.compareAndSet(update, null)
    }

    suspend fun install() {
        _pendingUpdate.value = null
        updater.install()
    }
}
