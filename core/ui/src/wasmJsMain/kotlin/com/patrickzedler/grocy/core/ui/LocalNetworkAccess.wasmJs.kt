package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

// Browsers ask for the local network themselves when a page reaches into it
@Composable
actual fun rememberLocalNetworkAccess(onResult: (granted: Boolean) -> Unit): LocalNetworkAccess? =
    null
