package com.patrickzedler.grocy.core.data.server

import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.update.isNewerVersion
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.network.GitHubReleasesException
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/** The logged in server as the server settings show it. */
data class ServerState(
    val connection: ServerConnection,
    /** Null until the server's version was downloaded once. */
    val grocyVersion: String?,
) {
    /** Grocy's public demo, where everything entered is visible to everyone. */
    val isDemo: Boolean
        get() = connection is ServerConnection.Demo
}

/** Whether a newer Grocy than the server's exists. */
sealed interface GrocyVersionCheck {

    data class UpToDate(val serverVersion: String) : GrocyVersionCheck

    data class Available(
        val latestVersion: String,
        val serverVersion: String,
        /** The release on github.com with its changes. */
        val releaseUrl: String,
    ) : GrocyVersionCheck

    /** Neither the server's nor the newest version could be found out, e.g. offline. */
    data object Failed : GrocyVersionCheck
}

/** Grocy's repository on GitHub, whose releases are the published versions. */
const val GROCY_GITHUB_REPOSITORY = "grocy/grocy"

/**
 * The logged in server, read once at app start ([scope] is the app's), so the server settings
 * show it in their first frame.
 *
 * @param connection the stored connection, null when logged out.
 * @param grocyReleases the releases of [GROCY_GITHUB_REPOSITORY].
 */
class ServerRepository(
    connection: Flow<ServerConnection?>,
    private val masterDataRepository: MasterDataRepository,
    private val grocyReleases: GitHubReleases,
    scope: CoroutineScope,
) {

    /** Null while logged out and for the moment until the stored login is read. */
    val server: StateFlow<ServerState?> =
        combine(connection, masterDataRepository.systemInfo) { connection, systemInfo ->
            connection?.let { ServerState(it, systemInfo?.grocyVersion) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The stored configuration of the server, null for the moment until it is read. Screens get
     * it without waiting for the database, which a running download can keep busy for a while.
     */
    val serverConfig: StateFlow<ServerConfig?> =
        masterDataRepository.serverConfig.stateIn(scope, SharingStarted.Eagerly, null)

    /** @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline. */
    suspend fun reloadConfiguration() {
        masterDataRepository.reloadConfiguration()
    }

    /** @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline. */
    suspend fun refreshVersion() {
        masterDataRepository.refreshSystemInfo()
    }

    /**
     * Compares the server's version, downloaded anew (an update of the server changes it), with
     * Grocy's newest release. An unreachable server is compared by its stored version.
     */
    suspend fun checkGrocyVersion(): GrocyVersionCheck {
        val serverVersion = try {
            masterDataRepository.refreshSystemInfo().grocyVersion
        } catch (_: GrocyApiException) {
            masterDataRepository.systemInfo.first()?.grocyVersion
        } ?: return GrocyVersionCheck.Failed
        val release = try {
            grocyReleases.latestRelease()
        } catch (_: GitHubReleasesException) {
            null
        } ?: return GrocyVersionCheck.Failed
        val latestVersion = release.tagName.removePrefix("v")
        return if (isNewerVersion(latestVersion, than = serverVersion)) {
            GrocyVersionCheck.Available(latestVersion, serverVersion, release.pageUrl)
        } else {
            GrocyVersionCheck.UpToDate(serverVersion)
        }
    }
}
