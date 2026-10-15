package com.patrickzedler.grocy.core.testing

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.model.ServerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [CredentialStore]; [connection] is a [StateFlow] for direct assertions. */
class FakeCredentialStore(
    initial: ServerConnection? = null,
) : CredentialStore {

    override val connection = MutableStateFlow(initial)

    override suspend fun save(connection: ServerConnection) {
        this.connection.value = connection
    }

    override suspend fun clear() {
        connection.value = null
    }
}

/** In-memory [SettingsStore] with the same defaults as the real stores. */
class FakeSettingsStore(
    checkUnlockKey: Boolean = true,
    scannerEngine: ScannerEngine = ScannerEngine.ZXING,
) : SettingsStore {

    override val checkUnlockKey = MutableStateFlow(checkUnlockKey)

    override suspend fun setCheckUnlockKey(checkUnlockKey: Boolean) {
        this.checkUnlockKey.value = checkUnlockKey
    }

    override val scannerEngine = MutableStateFlow(scannerEngine)

    override suspend fun setScannerEngine(scannerEngine: ScannerEngine) {
        this.scannerEngine.value = scannerEngine
    }
}

/** [UnlockChecker] whose answers tests can change, e.g. to simulate an installation. */
class FakeUnlockChecker(
    var keyInstalled: Boolean = false,
    var playStoreInstalled: Boolean = true,
) : UnlockChecker {

    override fun isKeyInstalled(): Boolean = keyInstalled

    override fun isPlayStoreInstalled(): Boolean = playStoreInstalled
}
