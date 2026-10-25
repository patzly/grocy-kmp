package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.scanner.ScannerRepository
import com.patrickzedler.grocy.core.data.server.GROCY_GITHUB_REPOSITORY
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository
import com.patrickzedler.grocy.core.data.update.UpdateRepository
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory
import com.patrickzedler.grocy.feature.about.impl.AboutDependencies
import com.patrickzedler.grocy.feature.login.impl.LoginDependencies
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataDependencies
import com.patrickzedler.grocy.feature.settings.impl.SettingsDependencies
import com.patrickzedler.grocy.feature.start.impl.StartDependencies
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manual dependency injection: creates every shared object once and hands it to the features.
 *
 * Created once per process, on Android in the Application class, on the web in main().
 * Only the constructor is public, everything else is wired inside this module.
 */
class AppGraph(
    private val platform: PlatformDependencies,
) {
    // Lives as long as the process, for state the whole app shares
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val httpClient by lazy { createGrocyHttpClient() }

    private val grocyApi by lazy { GrocyApi(httpClient) }

    internal val authRepository by lazy {
        AuthRepository(platform.credentialStore, grocyApi) { masterDataRepository.clear() }
    }

    private val masterDataRepository by lazy {
        MasterDataRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    private val stockRepository by lazy {
        StockRepository(
            api = grocyApi,
            database = platform.database,
            connection = { platform.credentialStore.connection.first() },
        )
    }

    /** The logged in server and its version, read right at app start. */
    private val serverRepository = ServerRepository(
        connection = platform.credentialStore.connection,
        masterDataRepository = masterDataRepository,
        grocyReleases = GitHubReleases(httpClient, GROCY_GITHUB_REPOSITORY),
        scope = appScope,
    )

    internal val unlockRepository by lazy {
        UnlockRepository(platform.unlockChecker, platform.settingsStore, appScope)
    }

    private val scannerRepository by lazy {
        ScannerRepository(
            settingsStore = platform.settingsStore,
            unlockRepository = unlockRepository,
            availableEngines = platform.barcodeDecoders.keys,
        )
    }

    private val gitHubReleases by lazy { GitHubReleases(httpClient, GITHUB_REPOSITORY) }

    internal val updateRepository by lazy {
        UpdateRepository(platform.createAppUpdater(gitHubReleases))
    }

    /** The language of the app, which the Android activity applies before Android 13. */
    internal val appLanguageStore: AppLanguageStore
        get() = platform.appLanguageStore

    /** How the app looks, from the general settings; read right at app start. */
    internal val appearanceRepository = AppearanceRepository(platform.settingsStore, appScope)

    /** Decoders of the engine that scans right now, provided to every scanner by GrocyApp. */
    internal val barcodeDecoderFactory: Flow<BarcodeDecoderFactory> by lazy {
        scannerRepository.activeEngine.map {
            platform.barcodeDecoders[it] ?: BarcodeDecoderFactory.Zxing
        }
    }

    internal val loginDependencies: LoginDependencies = object : LoginDependencies {
        override val authRepository: AuthRepository
            get() = this@AppGraph.authRepository
    }

    internal val settingsDependencies: SettingsDependencies = object : SettingsDependencies {
        override val settingsStore: SettingsStore
            get() = platform.settingsStore
        override val appearanceRepository: AppearanceRepository
            get() = this@AppGraph.appearanceRepository
        override val appLanguageStore: AppLanguageStore
            get() = platform.appLanguageStore
        override val unlockRepository: UnlockRepository
            get() = this@AppGraph.unlockRepository
        override val logReader: LogReader
            get() = platform.logReader
        override val serverRepository: ServerRepository
            get() = this@AppGraph.serverRepository
        override val authRepository: AuthRepository
            get() = this@AppGraph.authRepository
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository

        override fun restartApp() = platform.restartApp()
    }

    internal val startDependencies: StartDependencies = object : StartDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
    }

    internal val masterDataDependencies: MasterDataDependencies = object : MasterDataDependencies {
        override val masterDataRepository: MasterDataRepository
            get() = this@AppGraph.masterDataRepository
        override val stockRepository: StockRepository
            get() = this@AppGraph.stockRepository
        override val serverConfig: StateFlow<ServerConfig?>
            get() = serverRepository.serverConfig
    }

    internal val aboutDependencies: AboutDependencies = object : AboutDependencies {
        override val unlockRepository: UnlockRepository
            get() = this@AppGraph.unlockRepository
        override val updateRepository: UpdateRepository
            get() = this@AppGraph.updateRepository
        override val versionName: String
            get() = platform.versionName
    }
}

/** Where the app's releases are published, also for update checks outside Google Play. */
private const val GITHUB_REPOSITORY = "patzly/grocy-kmp"
