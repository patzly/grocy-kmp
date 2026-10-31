package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListOptions
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingListStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the user set up the shopping list, read once at app start in [scope], so the screen shows
 * its fields and groups in its first frame. The status filter is shared by the shopping list and
 * the shopping mode but kept only for the process, like Grocy forgets it.
 */
class ShoppingListOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored options are read. */
    val options: StateFlow<ShoppingListOptions?> =
        settingsStore.shoppingListOptions.stateIn(scope, SharingStarted.Eagerly, null)

    private val mutableStatus = MutableStateFlow<ShoppingListStatus?>(null)

    /** Grocy's status filter, null for all items. */
    val status: StateFlow<ShoppingListStatus?> = mutableStatus.asStateFlow()

    fun setStatus(status: ShoppingListStatus?) {
        mutableStatus.value = status
    }

    /** Changes the current options; nothing happens before they are read. */
    suspend fun update(transform: (ShoppingListOptions) -> ShoppingListOptions) {
        val current = options.value ?: return
        settingsStore.setShoppingListOptions(transform(current))
    }
}
