package com.patrickzedler.grocy.feature.settings.impl.certificates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.settings.CertificateRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.network.CertificateSettings
import com.patrickzedler.grocy.core.model.network.TrustedCertificate
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What a check of the logged in server found, shown as snackbar. */
internal enum class CertificateCheck { TRUSTED, FAILED }

internal class CertificatesViewModel(
    private val repository: CertificateRepository,
    server: StateFlow<ServerState?>,
    private val authRepository: AuthRepository,
) : ViewModel() {

    /** Read at app start, so the page shows them in its first frame. */
    val settings: StateFlow<CertificateSettings?> = repository.settings

    /** The server whose certificate can be checked, only one reached through TLS. */
    val checkableServer: StateFlow<ServerConnection?> = server
        .map(::checkableConnection)
        .stateIn(viewModelScope, SharingStarted.Eagerly, checkableConnection(server.value))

    var isChecking by mutableStateOf(false)
        private set

    var checkResult by mutableStateOf<CertificateCheck?>(null)
        private set

    fun setClientAlias(alias: String?) {
        viewModelScope.launch { repository.setClientAlias(alias) }
    }

    fun remove(trusted: TrustedCertificate) {
        viewModelScope.launch { repository.remove(trusted) }
    }

    /** Connects like a login does, which asks about a certificate nothing vouches for. */
    fun checkServer() {
        val connection = checkableServer.value ?: return
        if (isChecking) return
        isChecking = true
        viewModelScope.launch {
            checkResult = try {
                authRepository.verify(connection)
                CertificateCheck.TRUSTED
            } catch (_: GrocyApiException) {
                CertificateCheck.FAILED
            } finally {
                isChecking = false
            }
        }
    }

    fun onCheckResultShown() {
        checkResult = null
    }

    private fun checkableConnection(server: ServerState?): ServerConnection? =
        server?.connection?.takeIf { it.serverUrl.startsWith("https://") }
}
