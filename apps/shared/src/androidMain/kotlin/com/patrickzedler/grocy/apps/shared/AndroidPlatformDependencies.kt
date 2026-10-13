package com.patrickzedler.grocy.apps.shared

import android.content.Context
import com.patrickzedler.grocy.core.data.credentials.AndroidCredentialStore
import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.settings.AndroidSettingsStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker

class AndroidPlatformDependencies(
    context: Context,
    isPlayVariant: Boolean,
) : PlatformDependencies {

    private val appContext: Context = context.applicationContext

    override val credentialStore: CredentialStore by lazy { AndroidCredentialStore(appContext) }

    override val settingsStore: SettingsStore by lazy { AndroidSettingsStore(appContext) }

    override val unlockChecker: UnlockChecker? =
        if (isPlayVariant) PackageManagerUnlockChecker(appContext) else null

    override val versionName: String by lazy {
        appContext.packageManager.packageInfoOrNull(appContext.packageName)
            ?.versionName
            .orEmpty()
    }
}
