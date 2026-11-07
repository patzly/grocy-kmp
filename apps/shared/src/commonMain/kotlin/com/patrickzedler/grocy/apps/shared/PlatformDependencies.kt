package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.notifications.NotificationAlarm
import com.patrickzedler.grocy.core.data.notifications.Notifier
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.update.AppUpdater
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.model.ScannerEngine
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

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

    /**
     * Emits whenever the device gets a network connection, e.g. to send what waited offline;
     * collected once in the app's scope.
     */
    val networkAvailable: Flow<Unit>

    /** Barcode engines of this platform; ZXing exists everywhere, ML Kit only in the Play app. */
    val barcodeDecoders: Map<ScannerEngine, BarcodeDecoderFactory>

    /**
     * How this build updates: Google Play in the Play app, GitHub releases ([gitHubReleases])
     * for every other Android build, its service worker on the web.
     */
    fun createAppUpdater(gitHubReleases: GitHubReleases): AppUpdater

    /** Shows the daily notifications. */
    val notifier: Notifier

    /**
     * Wakes the app for notifications: an alarm of the system on Android, which starts the app
     * and calls [AppGraph.checkNotifications], a timer of the open page on the web that calls
     * [onDue] in [scope].
     */
    fun createNotificationAlarm(scope: CoroutineScope, onDue: suspend () -> Unit): NotificationAlarm

    /**
     * Keeps the home screen widgets up to date with [dependencies] while the app runs in [scope];
     * only Android has widgets.
     */
    fun startWidgetUpdates(dependencies: WidgetDependencies, scope: CoroutineScope) = Unit

    /** Starts the app anew: a new process on Android, a reload of the page on the web. */
    fun restartApp()

    /**
     * Whether the app may reach the local network now; null where no permission guards it
     * (browsers ask themselves, Android before 17 allows it).
     */
    val localNetworkPermission: (() -> Boolean)?
        get() = null

    /** Asks Orbot to start Tor for this process's requests; only Android can use Tor. */
    fun requestTorStart() = Unit
}
