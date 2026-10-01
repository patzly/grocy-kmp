package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule

/**
 * [routeSerializers] must register every route under NavKey,
 * because Web has no reflection for state saving.
 */
@Composable
fun GrocyNavDisplay(
    startRoute: Route,
    routeSerializers: SerializersModule,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<NavKey>.() -> Unit,
) {
    val configuration = remember(routeSerializers) {
        SavedStateConfiguration { serializersModule = routeSerializers }
    }
    val backStack = rememberNavBackStack(configuration, startRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider { entries() },
    )
}