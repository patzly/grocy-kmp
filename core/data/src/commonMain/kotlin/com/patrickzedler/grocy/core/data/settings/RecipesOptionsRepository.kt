package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.recipes.RecipesOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the user set up the recipes, read once at app start in [scope], so the screen shows its
 * fields, groups, order and view in its first frame.
 */
class RecipesOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored options are read. */
    val options: StateFlow<RecipesOptions?> =
        settingsStore.recipesOptions.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current options; nothing happens before they are read. */
    suspend fun update(transform: (RecipesOptions) -> RecipesOptions) {
        val current = options.value ?: return
        settingsStore.setRecipesOptions(transform(current))
    }
}
