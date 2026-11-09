package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.network.CertificateSettings
import com.patrickzedler.grocy.core.model.network.ServerCertificate
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The client certificate and the servers' certificates the user trusts, read at app start. */
class CertificateRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {
    /** Null for the moment until they are read. */
    val settings: StateFlow<CertificateSettings?> =
        settingsStore.certificateSettings.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun awaitSettings(): CertificateSettings = settings.filterNotNull().first()

    suspend fun setClientAlias(alias: String?) {
        store(awaitSettings().copy(clientAlias = alias))
    }

    suspend fun trust(host: String, certificate: ServerCertificate) {
        val current = awaitSettings()
        if (current.isTrusted(host, certificate.sha256)) return
        val others = current.trusted.filterNot { it.host.equals(host, ignoreCase = true) }
        // One per server: a renewed certificate replaces the former
        store(current.copy(trusted = others + TrustedCertificate(host, certificate)))
    }

    /** Replaces every trusted certificate, e.g. with what the paired phone trusts. */
    suspend fun setTrusted(trusted: List<TrustedCertificate>) {
        val current = awaitSettings()
        if (current.trusted != trusted) store(current.copy(trusted = trusted))
    }

    suspend fun remove(trusted: TrustedCertificate) {
        val current = awaitSettings()
        store(current.copy(trusted = current.trusted - trusted))
    }

    private suspend fun store(new: CertificateSettings) {
        settingsStore.setCertificateSettings(new)
        // The handshake right after this reads the state
        settings.first { it == new }
    }
}

/** Asked about a server whose certificate nothing vouches for. */
data class CertificateQuestion(val host: String, val certificate: ServerCertificate)

/**
 * Lets a login wait while the user decides about a server's certificate. Only logins ask: it is
 * the moment the user chose that server, so nobody else ever sees this, e.g. in a network that
 * intercepts connections while the app downloads in the background.
 */
class CertificateTrustGate(private val repository: CertificateRepository) {

    private val mutex = Mutex()
    private var answer: CompletableDeferred<Boolean>? = null
    private val mutableQuestion = MutableStateFlow<CertificateQuestion?>(null)

    /** What the user has to decide right now. */
    val question: StateFlow<CertificateQuestion?> = mutableQuestion.asStateFlow()

    /** @return whether the user trusts [certificate] for [host]; it is then stored. */
    suspend fun ask(host: String, certificate: ServerCertificate): Boolean = mutex.withLock {
        val deferred = CompletableDeferred<Boolean>()
        answer = deferred
        mutableQuestion.value = CertificateQuestion(host, certificate)
        try {
            val trusted = deferred.await()
            if (trusted) repository.trust(host, certificate)
            trusted
        } finally {
            mutableQuestion.value = null
            answer = null
        }
    }

    fun onAnswered(trusted: Boolean) {
        answer?.complete(trusted)
    }
}
