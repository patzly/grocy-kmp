package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.chores.ChoresJournalOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * How the user set up the chores journal, read once at app start in [scope], so the screen shows
 * its fields, groups and order in its first frame.
 */
class ChoresJournalOptionsRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {

    /** Null only for the moment at app start until the stored options are read. */
    val options: StateFlow<ChoresJournalOptions?> =
        settingsStore.choresJournalOptions.stateIn(scope, SharingStarted.Eagerly, null)

    /** Changes the current options; nothing happens before they are read. */
    suspend fun update(transform: (ChoresJournalOptions) -> ChoresJournalOptions) {
        val current = options.value ?: return
        settingsStore.setChoresJournalOptions(transform(current))
    }
}
