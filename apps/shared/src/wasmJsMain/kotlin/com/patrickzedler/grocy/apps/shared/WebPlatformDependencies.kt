package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.credentials.WebCredentialStore
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.language.WebAppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.settings.WebSettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.data.update.AppUpdater
import com.patrickzedler.grocy.core.data.update.ServiceWorkerAppUpdater
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory

/** [buildId] tells deployments apart for updates, `DEV_BUILD_ID` outside CI. */
class WebPlatformDependencies(
    override val versionName: String,
    private val buildId: String,
) : PlatformDependencies {

    override val credentialStore: CredentialStore by lazy { WebCredentialStore() }

    override val settingsStore: SettingsStore by lazy { WebSettingsStore() }

    override val appLanguageStore: AppLanguageStore by lazy { WebAppLanguageStore() }

    // Browsers keep their console to themselves
    override val logReader: LogReader? = null

    override val unlockChecker: UnlockChecker? = null

    override val barcodeDecoders: Map<ScannerEngine, BarcodeDecoderFactory> =
        mapOf(ScannerEngine.ZXING to BarcodeDecoderFactory.Zxing)

    // The web app is deployed, not released; its service worker knows the newest version
    override fun createAppUpdater(gitHubReleases: GitHubReleases): AppUpdater =
        ServiceWorkerAppUpdater(buildId)
}
