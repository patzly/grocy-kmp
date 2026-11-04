package com.patrickzedler.grocy.feature.settings.impl.behavior

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.settings.BehaviorSettingsRepository
import com.patrickzedler.grocy.core.model.BehaviorSettings
import com.patrickzedler.grocy.core.model.DateInputOrder
import com.patrickzedler.grocy.core.model.ProductFactsSource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The behavior settings, kept on the device; every change is saved at once. */
internal class BehaviorSettingsViewModel(
    private val repository: BehaviorSettingsRepository,
) : ViewModel() {

    /** Read at app start, so the page shows the stored values in its first frame. */
    val settings: StateFlow<BehaviorSettings?> = repository.settings

    fun setSource(source: ProductFactsSource, enabled: Boolean) = update {
        val sources = it.productFactsSources
        it.copy(productFactsSources = if (enabled) sources + source else sources - source)
    }

    fun setDateKeyboard(enabled: Boolean) = update { it.copy(dateKeyboard = enabled) }

    fun setDateInputOrder(order: DateInputOrder) = update { it.copy(dateInputOrder = order) }

    fun setBarcodeUsesAmount(enabled: Boolean) = update { it.copy(barcodeUsesAmount = enabled) }

    private fun update(transform: (BehaviorSettings) -> BehaviorSettings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
