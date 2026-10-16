package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.data.update.AppUpdater
import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * [AppUpdater] answering [result] to every check; emit to [foregroundEvents] to simulate the
 * app returning to the foreground, count [checks] and [installs].
 */
class FakeAppUpdater(
    var result: UpdateCheck = UpdateCheck.UpToDate,
    override var checksAutomatically: Boolean = false,
) : AppUpdater {

    override val foregroundEvents = MutableSharedFlow<Unit>()

    var checks = 0
        private set

    var installs = 0
        private set

    override suspend fun check(): UpdateCheck {
        checks++
        return result
    }

    override suspend fun install() {
        installs++
    }
}
