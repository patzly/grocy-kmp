package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable

/** The state of the permission to reach devices in the local network. */
enum class LocalNetworkPermission {
    Granted,

    /** Android's question can be asked. */
    Requestable,

    /** Denied for good: only the system settings can allow it now. */
    Denied,
}

/**
 * The permission to reach the local network, where self-hosted servers often run. Android 17
 * blocks the local network for apps without it.
 */
interface LocalNetworkAccess {
    val permission: LocalNetworkPermission

    /** Asks Android, the answer arrives in the callback of [rememberLocalNetworkAccess]. */
    fun request()

    fun openSettings()
}

/**
 * The local network permission, null where no permission guards it: browsers ask themselves,
 * Android before 17 allows it to every app with internet access. [onResult] gets whether the
 * user allowed it after [LocalNetworkAccess.request].
 */
@Composable
expect fun rememberLocalNetworkAccess(onResult: (granted: Boolean) -> Unit = {}): LocalNetworkAccess?
