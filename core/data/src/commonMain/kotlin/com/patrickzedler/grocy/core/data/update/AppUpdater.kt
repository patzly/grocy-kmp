package com.patrickzedler.grocy.core.data.update

import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Finds and installs newer versions of the app, in the way of the platform and its store. */
interface AppUpdater {

    /**
     * True where the app may look for updates on its own (the web app, which updates like a
     * website); elsewhere only when the user asks for it.
     */
    val checksAutomatically: Boolean
        get() = false

    /** Emits when the app comes back to the foreground, a good moment for an automatic check. */
    val foregroundEvents: Flow<Unit>
        get() = emptyFlow()

    suspend fun check(): UpdateCheck

    /** Installs the update the last [check] found, e.g. by opening its download page. */
    suspend fun install()
}
