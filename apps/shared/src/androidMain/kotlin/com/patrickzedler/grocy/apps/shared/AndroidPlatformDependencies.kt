package com.patrickzedler.grocy.apps.shared

import android.content.Context
import com.patrickzedler.grocy.core.data.credentials.AndroidCredentialStore
import com.patrickzedler.grocy.core.data.credentials.CredentialStore

class AndroidPlatformDependencies(
    context: Context,
) : PlatformDependencies {

    // Application context only, an Activity context would leak on rotation
    private val context: Context = context.applicationContext

    override val credentialStore: CredentialStore by lazy { AndroidCredentialStore(context) }
}
