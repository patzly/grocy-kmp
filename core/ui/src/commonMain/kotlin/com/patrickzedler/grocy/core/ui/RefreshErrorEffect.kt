package com.patrickzedler.grocy.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_load_failed
import com.patrickzedler.grocy.core.resources.data_offline
import org.jetbrains.compose.resources.getString

/**
 * Shows a failed download as snackbar while the screen has saved data to show; without data
 * the screen explains it in its list instead and [hasData] is false.
 */
@Composable
fun RefreshErrorEffect(
    error: RefreshError?,
    hasData: Boolean,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit,
) {
    val currentOnErrorShown by rememberUpdatedState(onErrorShown)
    LaunchedEffect(error, hasData) {
        if (error == null || !hasData) return@LaunchedEffect
        val message = when (error) {
            RefreshError.OFFLINE -> Res.string.data_offline
            RefreshError.FAILED -> Res.string.data_load_failed
        }
        // Cleared afterwards, a new key would cancel the snackbar while it shows
        snackbarHostState.showSnackbar(getString(message))
        currentOnErrorShown()
    }
}
