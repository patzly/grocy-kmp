package com.patrickzedler.grocy.feature.settings.impl.stock

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.masterdata.Location
import com.patrickzedler.grocy.core.model.masterdata.ProductGroup
import com.patrickzedler.grocy.core.model.masterdata.QuantityUnit
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.text.platformDecimalSeparator
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEvent
import com.patrickzedler.grocy.feature.settings.impl.UserSettingSaver
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The choices of the presets, from the database. */
internal data class StockSettingsOptions(
    val locations: List<Location>,
    val productGroups: List<ProductGroup>,
    val quantityUnits: List<QuantityUnit>,
    val config: ServerConfig,
)

/**
 * What the page edits: switches and choices change at once, numbers as typed; each change is
 * saved on its own like on Grocy's settings pages, a failed one returns to the stored value.
 */
@Stable
internal class StockSettingsForm(settings: UserSettings, decimalSeparator: Char) {
    val numbers = StockNumberSetting.entries.associateWith {
        TextFieldState(it.textOf(settings, decimalSeparator))
    }
    var switches by mutableStateOf(
        StockSwitchSetting.entries.associateWith { it.valueOf(settings) },
    )
    var presets by mutableStateOf(StockPresetSetting.entries.associateWith { it.valueOf(settings) })
    var labelType by mutableStateOf(settings.productPresets.defaultStockLabelType)
    var errors by mutableStateOf<Map<StockNumberSetting, SettingError>>(emptyMap())
}

/**
 * Grocy's stock settings of the user, "Presets for new products" first. Online only: each
 * change is saved right away; offline it returns to the stored value with a message.
 */
@OptIn(FlowPreview::class)
internal class StockSettingsViewModel(
    private val repository: MasterDataRepository,
    private val decimalSeparator: Char = platformDecimalSeparator(),
) : ViewModel() {

    val options: StateFlow<StockSettingsOptions?> = combine(
        repository.locations,
        repository.productGroups,
        repository.quantityUnits,
        repository.serverConfig,
    ) { locations, groups, units, config ->
        StockSettingsOptions(locations, groups, units, config)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var form by mutableStateOf<StockSettingsForm?>(null)
        private set

    private var stored = UserSettings()
    private val saver = UserSettingSaver(repository, viewModelScope)
    val events: Flow<UserSettingEvent> = saver.events

    init {
        viewModelScope.launch {
            stored = repository.userSettings.first()
            val form = StockSettingsForm(stored, decimalSeparator)
            this@StockSettingsViewModel.form = form
            form.numbers.forEach { (setting, state) -> watch(form, setting, state) }
        }
        viewModelScope.launch {
            repository.userSettings.collect { stored = it }
        }
    }

    /** Typed numbers are saved once typing pauses and the number is valid. */
    private fun watch(form: StockSettingsForm, setting: StockNumberSetting, state: TextFieldState) {
        viewModelScope.launch {
            snapshotFlow { state.text.toString() }
                .drop(1)
                .debounce(TYPING_DELAY_MILLIS)
                .collect { text ->
                    val error = setting.errorOf(text, stored)
                    form.errors = if (error == null) {
                        form.errors - setting
                    } else {
                        form.errors + (setting to error)
                    }
                    val value = if (error == null) setting.valueOf(text, stored) else null
                    val current = setting.valueOf(stored)
                    val unchanged = when (value) {
                        is UserSettingValue.Whole -> value.value.toDouble() == current
                        is UserSettingValue.Number -> value.value == current
                        else -> true
                    }
                    if (value != null && !unchanged) {
                        save(setting.key, value) {
                            state.setTextAndPlaceCursorAtEnd(
                                setting.textOf(stored, decimalSeparator),
                            )
                        }
                    }
                }
        }
    }

    fun setSwitch(setting: StockSwitchSetting, checked: Boolean) {
        val form = form ?: return
        form.switches += setting to checked
        save(setting.key, UserSettingValue.Flag(checked)) {
            form.switches += setting to setting.valueOf(stored)
        }
    }

    fun setPreset(setting: StockPresetSetting, id: Int?) {
        val form = form ?: return
        if (form.presets[setting] == id) return
        form.presets += setting to id
        save(setting.key, setting.valueOf(id)) {
            form.presets += setting to setting.valueOf(stored)
        }
    }

    fun setLabelType(type: Int) {
        val form = form ?: return
        form.labelType = type
        save(LABEL_TYPE_KEY, UserSettingValue.Whole(type.toLong())) {
            form.labelType = stored.productPresets.defaultStockLabelType
        }
    }

    private fun save(key: String, value: UserSettingValue, revert: () -> Unit) =
        saver.save(key, value, revert)

    private companion object {
        const val TYPING_DELAY_MILLIS = 800L
    }
}
