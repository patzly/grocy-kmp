package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.credentials.CredentialStore
import com.patrickzedler.grocy.core.data.credentials.WebCredentialStore

class WebPlatformDependencies : PlatformDependencies {

    override val credentialStore: CredentialStore by lazy { WebCredentialStore() }
}
