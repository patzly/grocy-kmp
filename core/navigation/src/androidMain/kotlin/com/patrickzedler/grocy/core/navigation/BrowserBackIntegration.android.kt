package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

@Composable
internal actual fun BrowserBackIntegration(
    navigator: Navigator,
    onUserAgentTransition: () -> Unit,
) = Unit

// Android restores the back stack itself through saved state
internal actual fun restoredBackStack(codec: BackStackCodec): List<NavKey>? = null
