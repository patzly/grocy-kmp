package com.patrickzedler.grocy.feature.settings.impl.certificates

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.security.KeyChain
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberClientCertificatePicker(
    currentAlias: String?,
    onPicked: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember(context, currentAlias) {
        {
            val activity = context.findActivity()
            if (activity != null) {
                // The user choosing here grants this app the use of the key, no permission needed
                KeyChain.choosePrivateKeyAlias(
                    activity,
                    { alias ->
                        // Called on a thread of the key chain, null for a cancelled choice
                        if (alias != null) {
                            Handler(Looper.getMainLooper()).post { currentOnPicked(alias) }
                        }
                    },
                    null,
                    null,
                    null,
                    currentAlias,
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
