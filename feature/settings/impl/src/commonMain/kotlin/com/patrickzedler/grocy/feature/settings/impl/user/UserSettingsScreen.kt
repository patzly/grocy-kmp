package com.patrickzedler.grocy.feature.settings.impl.user

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.grocy_language_default
import com.patrickzedler.grocy.core.resources.grocy_language_demo
import com.patrickzedler.grocy.core.resources.ic_rounded_translate
import com.patrickzedler.grocy.core.resources.settings_language
import com.patrickzedler.grocy.core.resources.settings_user
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEvent
import com.patrickzedler.grocy.feature.settings.impl.UserSettingEventEffect
import com.patrickzedler.grocy.feature.settings.impl.UserSettingSaver
import com.patrickzedler.grocy.feature.settings.impl.general.GrocyLanguageDialog
import com.patrickzedler.grocy.feature.settings.impl.general.languageName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's user settings: the language Grocy shows itself in for the user (its web frontend,
 * texts it writes), empty for its default; saved at once, a failed save returns to the stored
 * one. [server] tells whether it is a demo, which always shows the language of its address.
 */
internal class UserSettingsViewModel(
    private val repository: MasterDataRepository,
    val server: StateFlow<ServerState?>,
) : ViewModel() {

    /** Grocy's code of the language ("pt_BR"), empty for its default; null until read. */
    var locale by mutableStateOf<String?>(null)
        private set

    private var stored = ""
    private val saver = UserSettingSaver(repository, viewModelScope)
    val events: Flow<UserSettingEvent> = saver.events

    init {
        viewModelScope.launch {
            stored = repository.userSettings.first().locale
            locale = stored
        }
        viewModelScope.launch {
            repository.userSettings.collect { stored = it.locale }
        }
    }

    fun chooseLocale(code: String?) {
        val value = code.orEmpty()
        if (value == locale) return
        locale = value
        saver.save(LOCALE_KEY, UserSettingValue.Text(value)) { locale = stored }
    }

    private companion object {
        const val LOCALE_KEY = "locale"
    }
}

@Composable
internal fun UserSettingsScreen(
    viewModel: UserSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val server by viewModel.server.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    UserSettingEventEffect(viewModel.events, snackbarHostState)

    if (showLanguageDialog) {
        GrocyLanguageDialog(
            languages = GrocyLanguages,
            selectedLanguage = viewModel.locale?.takeIf { it.isNotEmpty() },
            onLanguageSelected = viewModel::chooseLocale,
            onDismissRequest = { showLanguageDialog = false },
        )
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_user),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
        // Opened from Grocy's settings, it returns to them beside the categories too
        hasBackButton = true,
        snackbarHostState = snackbarHostState,
    ) {
        val locale = viewModel.locale ?: return@SettingsScaffold
        segmentedGroup(key = "language") {
            LanguageItem(
                locale = locale,
                isDemo = server?.isDemo == true,
                onClick = { showLanguageDialog = true },
            )
        }
    }
}

/** The language with its current value; demos ignore it, so it is disabled there. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageItem(locale: String, isDemo: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val value = if (locale.isEmpty()) {
        stringResource(Res.string.grocy_language_default)
    } else {
        remember(locale) { languageName(locale.replace('_', '-')) }
    }

    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        enabled = !isDemo,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(enabled = !isDemo),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painterResource(Res.drawable.ic_rounded_translate), contentDescription = null)
            }
        },
        supportingContent = {
            Text(if (isDemo) stringResource(Res.string.grocy_language_demo) else value)
        },
        content = { Text(stringResource(Res.string.settings_language)) },
    )
}

/** The languages of Grocy (its `localization` folders), by Grocy's codes. */
internal val GrocyLanguages = listOf(
    "bg_BG", "ca", "cs", "da", "de", "el_GR", "en", "en_GB", "es", "et_EE", "fi", "fr",
    "he_IL", "hu", "it", "ja", "ko_KR", "lt", "nl", "no", "pl", "pt_BR", "pt_PT", "ro_RO",
    "ru", "sk_SK", "sl", "sv_SE", "ta", "tr", "uk", "zh_CN", "zh_TW",
)
