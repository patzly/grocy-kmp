package com.patrickzedler.grocy.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
fun GrocyNavDisplay(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<NavKey>.() -> Unit,
) {
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = navigator::goBack,
        entryDecorators = listOf(
            // Keeps rememberSaveable state per entry
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes each ViewModel to its entry, cleared when the entry leaves the back stack
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider { entries() },
    )
}
