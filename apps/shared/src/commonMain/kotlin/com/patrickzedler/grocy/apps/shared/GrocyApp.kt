package com.patrickzedler.grocy.apps.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.auth.AuthState
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.rememberThemeFontsReady
import com.patrickzedler.grocy.core.navigation.GrocyNavDisplay
import com.patrickzedler.grocy.core.navigation.rememberNavigator
import com.patrickzedler.grocy.feature.login.api.LoginChoiceRoute
import com.patrickzedler.grocy.feature.login.api.loginRouteSerializers
import com.patrickzedler.grocy.feature.login.impl.loginEntries
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.start.api.startRouteSerializers
import com.patrickzedler.grocy.feature.start.impl.startEntry
import kotlinx.serialization.modules.plus

private val routeSerializers = startRouteSerializers + loginRouteSerializers

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
    GrocyTheme {
        val authState by graph.authRepository.authState
            .collectAsStateWithLifecycle(initialValue = null)
        val fontsReady = rememberThemeFontsReady()

        // Null only for the moment until the stored login has been read
        authState?.let {
            GrocyNavigation(graph, it)
            LaunchedEffect(fontsReady) {
                if (fontsReady) onContentReady()
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
        startEntry()
    }
}
