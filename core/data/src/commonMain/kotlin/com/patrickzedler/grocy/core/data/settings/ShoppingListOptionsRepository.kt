package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the user set up the shopping list, read once at app start in [scope], so the screen shows
 * its fields and groups in its first frame.
 */
class ShoppingListOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored options are read. */
    val options: StateFlow<ShoppingListOptions?> =
        settingsStore.shoppingListOptions.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current options; nothing happens before they are read. */
    suspend fun update(transform: (ShoppingListOptions) -> ShoppingListOptions) {
        val current = options.value ?: return
        settingsStore.setShoppingListOptions(transform(current))
    }
}
