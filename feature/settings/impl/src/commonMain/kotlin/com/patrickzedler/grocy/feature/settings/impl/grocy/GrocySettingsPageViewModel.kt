package com.patrickzedler.grocy.feature.settings.impl.grocy

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
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.server.UserSettings
import com.patrickzedler.grocy.core.model.shoppinglist.ShoppingList
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPage
import com.patrickzedler.grocy.feature.settings.impl.SettingError
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEvent
import com.patrickzedler.grocy.feature.settings.impl.UserSettingSaver
import com.patrickzedler.grocy.feature.settings.impl.numberSettingError
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

/** What the page needs besides the settings: the feature flags and the shopping lists. */
internal data class GrocySettingsPageOptions(
    val config: ServerConfig,
    val shoppingLists: List<ShoppingList>,
)

/** What a page edits: switches and the list change at once, the due soon days as typed. */
@Stable
internal class GrocySettingsForm(settings: UserSettings) {
    val dueSoonDays = DueSoonDaysSetting.entries.associateWith {
        TextFieldState(it.valueOf(settings).toString())
    }
    var switches by mutableStateOf(
        GrocySwitchSetting.entries.associateWith { it.valueOf(settings) },
    )
    var autoAddListId by mutableStateOf(settings.shoppingListAutoAddListId)
    var errors by mutableStateOf<Map<DueSoonDaysSetting, SettingError>>(emptyMap())
}

/**
 * One of Grocy's settings pages of the user ([page]), saved like Grocy's: each change at once,
 * online only; a failed one returns to the stored value with a message.
 */
@OptIn(FlowPreview::class)
internal class GrocySettingsPageViewModel(
    private val repository: MasterDataRepository,
    val page: GrocySettingsPage,
) : ViewModel() {

    val options: StateFlow<GrocySettingsPageOptions?> =
        combine(repository.serverConfig, repository.shoppingLists, ::GrocySettingsPageOptions)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var form by mutableStateOf<GrocySettingsForm?>(null)
        private set

    private var stored = UserSettings()
    private val saver = UserSettingSaver(repository, viewModelScope)
    val events: Flow<UserSettingEvent> = saver.events

    init {
        viewModelScope.launch {
            stored = repository.userSettings.first()
            val form = GrocySettingsForm(stored)
            this@GrocySettingsPageViewModel.form = form
            form.dueSoonDays.forEach { (setting, state) -> watch(form, setting, state) }
        }
        viewModelScope.launch {
            repository.userSettings.collect { stored = it }
        }
    }

    /** Typed days are saved once typing pauses and the number is valid. */
    private fun watch(form: GrocySettingsForm, setting: DueSoonDaysSetting, state: TextFieldState) {
        viewModelScope.launch {
            snapshotFlow { state.text.toString() }
                .drop(1)
                .debounce(TYPING_DELAY_MILLIS)
                .collect { text ->
                    val error = numberSettingError(text, min = 0.0, max = null, places = 0)
                    form.errors = if (error == null) {
                        form.errors - setting
                    } else {
                        form.errors + (setting to error)
                    }
                    val days = text.trim().toLongOrNull()
                    if (error == null && days != null && days != setting.valueOf(stored).toLong()) {
                        saver.save(setting.key, UserSettingValue.Whole(days)) {
                            state.setTextAndPlaceCursorAtEnd(setting.valueOf(stored).toString())
                        }
                    }
                }
        }
    }

    fun setSwitch(setting: GrocySwitchSetting, checked: Boolean) {
        val form = form ?: return
        form.switches += setting to checked
        saver.save(setting.key, UserSettingValue.Flag(checked)) {
            form.switches += setting to setting.valueOf(stored)
        }
    }

    fun setAutoAddList(id: Int?) {
        val form = form ?: return
        if (id == null || id == form.autoAddListId) return
        form.autoAddListId = id
        saver.save(AUTO_ADD_LIST_KEY, UserSettingValue.Whole(id.toLong())) {
            form.autoAddListId = stored.shoppingListAutoAddListId
        }
    }

    private companion object {
        const val TYPING_DELAY_MILLIS = 800L
    }
}
