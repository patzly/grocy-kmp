package com.patrickzedler.grocy.apps.shared

/**
 * Everything the shared app needs from the platform it runs on.
 *
 * Each platform provides one implementation; the app modules pass it to [AppGraph].
 */
interface PlatformDependencies
