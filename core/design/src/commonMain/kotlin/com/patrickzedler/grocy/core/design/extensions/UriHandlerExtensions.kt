package com.patrickzedler.grocy.core.design.component

import androidx.compose.ui.platform.UriHandler

/** Opens [uri] if any app can handle it, Android throws IllegalArgumentException otherwise. */
fun UriHandler.tryOpenUri(uri: String) {
    try {
        openUri(uri)
    } catch (_: IllegalArgumentException) {
    }
}
