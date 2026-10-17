package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.update.AppUpdater
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory

/**
 * Everything the shared app needs from the platform it runs on.
 *
 * Each platform provides one implementation; the app modules pass it to [AppGraph].
 */
interface PlatformDependencies {
    val credentialStore: CredentialStore
    val settingsStore: SettingsStore
    val appLanguageStore: AppLanguageStore

    /** The local copy of the server's data, created once on first use. */
    val database: GrocyDatabase

    val logReader: LogReader

    /** Null where no unlock app exists, which hides everything related to it. */
    val unlockChecker: UnlockChecker?

    val versionName: String

    /** Barcode engines of this platform; ZXing exists everywhere, ML Kit only in the Play app. */
    val barcodeDecoders: Map<ScannerEngine, BarcodeDecoderFactory>

    /**
     * How this build updates: Google Play in the Play app, GitHub releases ([gitHubReleases])
     * for every other Android build, its service worker on the web.
     */
    fun createAppUpdater(gitHubReleases: GitHubReleases): AppUpdater
}
