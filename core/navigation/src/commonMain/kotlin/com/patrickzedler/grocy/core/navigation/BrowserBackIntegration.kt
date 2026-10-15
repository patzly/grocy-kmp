package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable

/**
 * Connects the browser's back button and back gesture to the app's back handling on the web,
 * nothing on Android, where the system back already reaches the app.
 *
 * @param onUserAgentTransition called before a back the browser has animated already.
 */
@Composable
internal expect fun BrowserBackIntegration(onUserAgentTransition: () -> Unit)
