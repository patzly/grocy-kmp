package com.patrickzedler.grocy.feature.settings.impl.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class GeneralSettingsViewModel(
    private val settingsStore: SettingsStore,
    private val languageStore: AppLanguageStore,
) : ViewModel() {

    /** Null until the stored settings are read, so no default flashes up. */
    val appearance: StateFlow<AppearanceSettings?> = settingsStore.appearance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = null,
    )

    val language: StateFlow<String?> = languageStore.language

    fun updateAppearance(update: (AppearanceSettings) -> AppearanceSettings) {
        val current = appearance.value ?: return
        viewModelScope.launch { settingsStore.setAppearance(update(current)) }
    }

    /** [tag] null is the system language; the screen restarts in the new language. */
    fun selectLanguage(tag: String?) {
        languageStore.setLanguage(tag)
    }
}
