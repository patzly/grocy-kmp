package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockChecker

/**
 * Everything the shared app needs from the platform it runs on.
 *
 * Each platform provides one implementation; the app modules pass it to [AppGraph].
 */
interface PlatformDependencies {
    val credentialStore: CredentialStore
    val settingsStore: SettingsStore

    /** Null where no unlock app exists, which hides everything related to it. */
    val unlockChecker: UnlockChecker?

    val versionName: String
}
