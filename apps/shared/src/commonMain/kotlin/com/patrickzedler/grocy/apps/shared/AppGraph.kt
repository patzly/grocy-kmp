package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import com.patrickzedler.grocy.feature.login.impl.LoginDependencies

/**
 * Manual dependency injection: creates every shared object once and hands it to the features.
 *
 * Created once per process, on Android in the Application class, on the web in main().
 */
class AppGraph(
    private val platform: PlatformDependencies,
) : LoginDependencies {

    private val httpClient by lazy { createGrocyHttpClient() }

    val grocyApi by lazy { GrocyApi(httpClient) }

    override val authRepository by lazy { AuthRepository(platform.credentialStore, grocyApi) }
}
