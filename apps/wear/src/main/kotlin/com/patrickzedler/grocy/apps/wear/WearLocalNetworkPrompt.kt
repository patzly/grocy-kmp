package com.patrickzedler.grocy.apps.wear

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.network.LocalNetworkGate

/**
 * Asks Android for the local network when a request goes there (Android 17); the request waits
 * at [gate] meanwhile. The watch shows Android's question directly, its screen has no room for
 * an explanation before, and the list already shows why it waits.
 */
@Composable
internal fun WearLocalNetworkPrompt(gate: LocalNetworkGate) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { gate.onAnswered() }
    // Android's question is an activity of its own, which only pauses this one
    LifecycleStartEffect(gate) {
        gate.setCanAsk(true)
        onStopOrDispose { gate.setCanAsk(false) }
    }
    val isAsking by gate.isAsking.collectAsStateWithLifecycle()
    LaunchedEffect(isAsking) {
        if (isAsking) launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
}
