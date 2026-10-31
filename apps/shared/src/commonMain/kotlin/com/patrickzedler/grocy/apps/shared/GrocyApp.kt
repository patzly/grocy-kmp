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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.auth.AuthState
import com.patrickzedler.grocy.core.data.update.UpdateRepository
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.LocalPointerKind
import com.patrickzedler.grocy.core.design.rememberPointerKindState
import com.patrickzedler.grocy.core.design.rememberResourcesReady
import com.patrickzedler.grocy.core.design.trackPointerKind
import com.patrickzedler.grocy.core.navigation.GrocyNavDisplay
import com.patrickzedler.grocy.core.navigation.rememberNavigator
import com.patrickzedler.grocy.core.scanner.BarcodeDecoderFactory
import com.patrickzedler.grocy.core.scanner.LocalBarcodeDecoderFactory
import com.patrickzedler.grocy.core.ui.AppMenuLinks
import com.patrickzedler.grocy.core.ui.LocalAppMenuLinks
import com.patrickzedler.grocy.core.ui.LocalUnlockState
import com.patrickzedler.grocy.core.ui.dialog.UpdateAvailableDialog
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.about.api.aboutRouteSerializers
import com.patrickzedler.grocy.feature.about.impl.aboutEntries
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.api.loginRouteSerializers
import com.patrickzedler.grocy.feature.login.impl.loginEntries
import com.patrickzedler.grocy.feature.masterdata.api.masterDataRouteSerializers
import com.patrickzedler.grocy.feature.masterdata.impl.masterDataEntries
import com.patrickzedler.grocy.feature.recipes.api.recipesRouteSerializers
import com.patrickzedler.grocy.feature.recipes.impl.recipesEntries
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.settings.api.settingsRouteSerializers
import com.patrickzedler.grocy.feature.settings.impl.settingsEntries
import com.patrickzedler.grocy.feature.shoppinglist.api.shoppingListRouteSerializers
import com.patrickzedler.grocy.feature.shoppinglist.impl.shoppingListEntries
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.start.api.startRouteSerializers
import com.patrickzedler.grocy.feature.start.impl.startEntry
import com.patrickzedler.grocy.feature.stock.api.stockRouteSerializers
import com.patrickzedler.grocy.feature.stock.impl.stockEntries
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.plus

internal val routeSerializers =
    startRouteSerializers + loginRouteSerializers + aboutRouteSerializers +
        settingsRouteSerializers + masterDataRouteSerializers + stockRouteSerializers +
        shoppingListRouteSerializers + recipesRouteSerializers

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
        val resourcesReady = rememberResourcesReady()
        val barcodeDecoderFactory by graph.barcodeDecoderFactory
            .collectAsStateWithLifecycle(initialValue = BarcodeDecoderFactory.Zxing)

        val pointerKind = rememberPointerKindState()
        val unlockState by graph.unlockRepository.unlockState.collectAsStateWithLifecycle()

        CompositionLocalProvider(
            LocalBarcodeDecoderFactory provides barcodeDecoderFactory,
            LocalPointerKind provides pointerKind,
            LocalUnlockState provides unlockState,
        ) {
            // Screen transitions fade, this keeps the window or page behind them out of sight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .trackPointerKind(pointerKind)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                // Null only for the moment until the stored login has been read
                authState?.let {
                    GrocyNavigation(graph, it)
                    LaunchedEffect(resourcesReady) {
                        if (resourcesReady) onContentReady()
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

    // The app menu of the features' screens leads to the settings and About
    val appMenuLinks = remember(navigator) {
        AppMenuLinks(
            onSettingsClick = { navigator.navigate(SettingsRoute) },
            onAboutClick = { navigator.navigate(AboutRoute) },
        )
    }
    CompositionLocalProvider(LocalAppMenuLinks provides appMenuLinks) {
        GrocyNavDisplay(navigator) {
            loginEntries(graph.loginDependencies, navigator)
            startEntry(graph.startDependencies, navigator)
            aboutEntries(graph.aboutDependencies, navigator)
            settingsEntries(graph.settingsDependencies, navigator)
            masterDataEntries(graph.masterDataDependencies, navigator)
            stockEntries(graph.stockDependencies, navigator)
            shoppingListEntries(graph.shoppingListDependencies, navigator)
            recipesEntries(graph.recipesDependencies, navigator)
        }
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
