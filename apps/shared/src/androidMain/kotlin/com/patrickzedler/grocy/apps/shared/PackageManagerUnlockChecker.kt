package com.patrickzedler.grocy.apps.shared

import android.content.Context
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker

/** Needs a <queries> entry for both packages in the app manifest, see android-play. */
internal class PackageManagerUnlockChecker(
    context: Context,
) : UnlockChecker {

    private val packageManager = context.packageManager

    override fun isKeyInstalled(): Boolean =
        packageManager.packageInfoOrNull(UNLOCK_PACKAGE) != null

    override fun isPlayStoreInstalled(): Boolean =
        packageManager.packageInfoOrNull(PLAY_STORE_PACKAGE) != null

    private companion object {
        const val UNLOCK_PACKAGE = "com.patrickzedler.grocy.unlock"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}
