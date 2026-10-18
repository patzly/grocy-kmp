package com.patrickzedler.grocy.apps.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.auth.AuthState
import com.patrickzedler.grocy.core.data.update.UpdateRepository
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.rememberThemeFontsReady
import com.patrickzedler.grocy.core.navigation.GrocyNavDisplay
import com.patrickzedler.grocy.core.navigation.rememberNavigator
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory
import com.patrickzedler.grocy.core.scanner.LocalBarcodeDecoderFactory
import com.patrickzedler.grocy.core.ui.dialog.UpdateAvailableDialog
import com.patrickzedler.grocy.feature.about.api.aboutRouteSerializers
import com.patrickzedler.grocy.feature.about.impl.aboutEntries
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.api.loginRouteSerializers
import com.patrickzedler.grocy.feature.login.impl.loginEntries
import com.patrickzedler.grocy.feature.masterdata.api.masterDataRouteSerializers
import com.patrickzedler.grocy.feature.masterdata.impl.masterDataEntries
import com.patrickzedler.grocy.feature.settings.api.settingsRouteSerializers
import com.patrickzedler.grocy.feature.settings.impl.settingsEntries
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.start.api.startRouteSerializers
import com.patrickzedler.grocy.feature.start.impl.startEntry
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.plus

internal val routeSerializers =
    startRouteSerializers + loginRouteSerializers + aboutRouteSerializers +
        settingsRouteSerializers + masterDataRouteSerializers

/**
 * Shared entry point of the Android and web apps.
 *
 * [onContentReady] is called once the first real screen is shown, platforms use it to remove
 * their splash screen.
 */
@Composable
fun GrocyApp(
    graph: AppGraph,
    onContentReady: () -> Unit = {},
) {
    // Nothing until the stored appearance is read, the splash or loading screen covers it
    val appearance = graph.appearanceRepository.appearance.collectAsStateWithLifecycle().value
        ?: return

    GrocyTheme(appearance = appearance) {
        val authState by graph.authRepository.authState
            .collectAsStateWithLifecycle(initialValue = null)
        val fontsReady = rememberThemeFontsReady()
        val barcodeDecoderFactory by graph.barcodeDecoderFactory
            .collectAsStateWithLifecycle(initialValue = BarcodeDecoderFactory.Zxing)

        CompositionLocalProvider(LocalBarcodeDecoderFactory provides barcodeDecoderFactory) {
            // Screen transitions fade, this keeps the window or page behind them out of sight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                // Null only for the moment until the stored login has been read
                authState?.let {
                    GrocyNavigation(graph, it)
                    LaunchedEffect(fontsReady) {
                        if (fontsReady) onContentReady()
                    }
                    UpdateOffer(graph.updateRepository)
                }
            }
        }
    }
}

@Composable
private fun GrocyNavigation(
    graph: AppGraph,
    authState: AuthState,
) {
    val isLoggedIn = authState is AuthState.LoggedIn
    val navigator = rememberNavigator(
        startRoute = if (isLoggedIn) StartRoute else LoginChoiceRoute,
        routeSerializers = routeSerializers,
    )

    // Reacts only to real login and logout, not to a restored back stack after process death
    var shownLoggedIn by rememberSaveable { mutableStateOf(isLoggedIn) }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn != shownLoggedIn) {
            navigator.replaceAll(if (isLoggedIn) StartRoute else LoginChoiceRoute)
            shownLoggedIn = isLoggedIn
        }
    }

    GrocyNavDisplay(navigator) {
        loginEntries(graph.loginDependencies, navigator)
        startEntry(graph.startDependencies, navigator)
        aboutEntries(graph.aboutDependencies, navigator)
        settingsEntries(graph.settingsDependencies, navigator)
        masterDataEntries(graph.masterDataDependencies, navigator)
    }
}

/**
 * Automatic update checks where the platform allows them (the web app) and the dialog for an
 * update they find, over whatever screen is open. Elsewhere updates are only checked in About.
 */
@Composable
private fun UpdateOffer(updateRepository: UpdateRepository) {
    LaunchedEffect(updateRepository) {
        updateRepository.runAutomaticChecks()
    }
    val pendingUpdate by updateRepository.pendingUpdate.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    pendingUpdate?.let { update ->
        UpdateAvailableDialog(
            update = update,
            onUpdate = { scope.launch { updateRepository.install() } },
            onLater = { updateRepository.postpone(update) },
        )
    }
}
