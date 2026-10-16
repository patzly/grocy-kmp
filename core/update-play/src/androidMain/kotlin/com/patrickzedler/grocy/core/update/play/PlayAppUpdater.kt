package com.patrickzedler.grocy.core.update.play

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.patrickzedler.grocy.core.data.update.AppUpdater
import com.patrickzedler.grocy.core.model.UpdateCheck
import kotlin.coroutines.cancellation.CancellationException

private const val PLAY_STORE_PACKAGE = "com.android.vending"

/**
 * Updates through Google Play's in-app update API, which installs the new version in a Play
 * screen on top of the app. Installs from elsewhere (nightly, debug or downloaded builds) use
 * [fallback], usually the GitHub releases.
 *
 * @param currentActivity the activity in front, which the Play update screen starts from.
 */
class PlayAppUpdater(
    context: Context,
    private val currentActivity: () -> Activity?,
    private val fallback: AppUpdater,
) : AppUpdater {

    private val appContext = context.applicationContext
    private val manager by lazy { AppUpdateManagerFactory.create(appContext) }
    private var foundUpdate: AppUpdateInfo? = null

    override suspend fun check(): UpdateCheck {
        if (!isInstalledFromPlay()) return fallback.check()
        val info = try {
            manager.requestAppUpdateInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return UpdateCheck.Failed
        }
        val result = updateCheckOf(info.updateAvailability())
        foundUpdate = info.takeIf { result is UpdateCheck.Available }
        return result
    }

    override suspend fun install() {
        if (!isInstalledFromPlay()) return fallback.install()
        val info = foundUpdate ?: return
        val activity = currentActivity() ?: return
        if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            manager.startUpdateFlow(
                info,
                activity,
                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
            )
        } else {
            // Play does not offer the update in the app right now, its store page does
            openStorePage(activity)
        }
    }

    private fun openStorePage(activity: Activity) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=${appContext.packageName}"),
        ).setPackage(PLAY_STORE_PACKAGE)
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun isInstalledFromPlay(): Boolean =
        installerPackage(appContext) == PLAY_STORE_PACKAGE
}

/** Maps Play's update availability to the app's result; a started update counts as available. */
internal fun updateCheckOf(availability: Int): UpdateCheck = when (availability) {
    UpdateAvailability.UPDATE_AVAILABLE,
    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
        -> UpdateCheck.Available(versionName = null)

    UpdateAvailability.UPDATE_NOT_AVAILABLE -> UpdateCheck.UpToDate
    else -> UpdateCheck.Failed
}

private fun installerPackage(context: Context): String? =
    try {
        val packageManager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(context.packageName)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
