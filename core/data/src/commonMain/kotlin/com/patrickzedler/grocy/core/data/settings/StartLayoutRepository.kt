package com.patrickzedler.grocy.core.data.settings

import com.patrickzedler.grocy.core.model.start.StartLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** How the user arranged the start screen, read at app start. */
class StartLayoutRepository(
    private val settingsStore: SettingsStore,
    scope: CoroutineScope,
) {
    /** Null for the moment until it is read. */
    val layout: StateFlow<StartLayout?> = settingsStore.startLayout
        .map(StartLayout::of)
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun save(layout: StartLayout) {
        settingsStore.setStartLayout(layout.stored())
    }
}
