package com.patrickzedler.grocy.apps.shared

/**
 * Manual dependency injection: creates every shared object once and hands it to the features.
 *
 * Created once per process, on Android in the Application class, on the web in main().
 */
class AppGraph(
    private val platform: PlatformDependencies,
)
