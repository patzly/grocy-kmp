package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore

/**
 * Everything the shared app needs from the platform it runs on.
 *
 * Each platform provides one implementation; the app modules pass it to [AppGraph].
 */
interface PlatformDependencies {
    val credentialStore: CredentialStore
}
