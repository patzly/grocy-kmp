package com.patrickzedler.grocy.apps.shared

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.patrickzedler.grocy.core.data.credentials.AndroidCredentialStore
import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.createGrocyDatabase
import com.patrickzedler.grocy.core.data.settings.AndroidSettingsStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.data.update.AppUpdater
import com.patrickzedler.grocy.core.data.update.GitHubAppUpdater
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory

/**
 * [additionalBarcodeDecoders] are engines an app module adds, e.g. ML Kit in the Play app,
 * which apps:shared must not depend on. [appUpdater] likewise replaces the GitHub updater it
 * gets, e.g. with Google Play's in-app updates.
 */
class AndroidPlatformDependencies(
    context: Context,
    isPlayVariant: Boolean,
    additionalBarcodeDecoders: Map<ScannerEngine, BarcodeDecoderFactory> = emptyMap(),
    private val appUpdater: (gitHubUpdater: AppUpdater) -> AppUpdater = { it },
) : PlatformDependencies {

    private val appContext: Context = context.applicationContext

    override val credentialStore: CredentialStore by lazy { AndroidCredentialStore(appContext) }

    override val settingsStore: SettingsStore by lazy { AndroidSettingsStore(appContext) }

    override val appLanguageStore: AppLanguageStore by lazy { AndroidAppLanguageStore(appContext) }

    override val database: GrocyDatabase by lazy { createGrocyDatabase(appContext) }

    override val logReader: LogReader = LogcatReader()

    override val unlockChecker: UnlockChecker? =
        if (isPlayVariant) PackageManagerUnlockChecker(appContext) else null

    override val barcodeDecoders: Map<ScannerEngine, BarcodeDecoderFactory> =
        mapOf(ScannerEngine.ZXING to BarcodeDecoderFactory.Zxing) + additionalBarcodeDecoders

    override val versionName: String by lazy {
        appContext.packageManager.packageInfoOrNull(appContext.packageName)
            ?.versionName
            .orEmpty()
    }

    override fun createAppUpdater(gitHubReleases: GitHubReleases): AppUpdater =
        appUpdater(
            GitHubAppUpdater(
                releases = gitHubReleases,
                versionName = versionName,
                openUrl = ::openInBrowser,
            ),
        )

    override fun restartApp() {
        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?: return
        appContext.startActivity(Intent.makeRestartActivityTask(launchIntent.component))
        // A new process, so nothing read before the restart survives in memory
        Runtime.getRuntime().exit(0)
    }

    private fun openInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
