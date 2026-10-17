package com.patrickzedler.grocy.feature.settings.impl.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.model.AppearanceSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class GeneralSettingsViewModel(
    private val appearanceRepository: AppearanceRepository,
    private val languageStore: AppLanguageStore,
) : ViewModel() {

    /** Read at app start, so the controls show the stored values in the first frame. */
    val appearance: StateFlow<AppearanceSettings?> = appearanceRepository.appearance

    val language: StateFlow<String?> = languageStore.language

    fun updateAppearance(update: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { appearanceRepository.update(update) }
    }

    /** [tag] null is the system language; the screen restarts in the new language. */
    fun selectLanguage(tag: String?) {
        languageStore.setLanguage(tag)
    }
}
