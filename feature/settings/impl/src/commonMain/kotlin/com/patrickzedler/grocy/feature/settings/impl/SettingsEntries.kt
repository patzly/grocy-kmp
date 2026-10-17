package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.settings.impl.general.GeneralSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.general.GeneralSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.log.LogScreen
import com.patrickzedler.grocy.feature.settings.impl.log.LogViewModel

fun EntryProviderScope<NavKey>.settingsEntries(
    dependencies: SettingsDependencies,
    navigator: Navigator,
) {
    val menuActions = SettingsMenuActions(
        onAboutClick = { navigator.navigate(AboutRoute) },
        onLogClick = { navigator.navigate(LogRoute) },
        logSource = dependencies.logReader.source,
    )

    entry<SettingsRoute> {
        SettingsScreen(
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
            onCategoryClick = { category ->
                // The other pages follow one by one
                when (category) {
                    SettingsCategory.GENERAL -> navigator.navigate(GeneralSettingsRoute)
                    else -> Unit
                }
            },
            onBackupClick = {},
            onResetClick = {},
        )
    }

    entry<GeneralSettingsRoute> {
        GeneralSettingsScreen(
            viewModel = viewModel {
                GeneralSettingsViewModel(
                    dependencies.appearanceRepository,
                    dependencies.appLanguageStore,
                )
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
        )
    }

    entry<LogRoute> {
        LogScreen(
            viewModel = viewModel { LogViewModel(dependencies.logReader) },
            menuViewModel = settingsMenuViewModel(dependencies),
            onBackClick = navigator::goBack,
        )
    }
}

@Composable
private fun settingsMenuViewModel(dependencies: SettingsDependencies): SettingsMenuViewModel =
    viewModel { SettingsMenuViewModel(dependencies.unlockRepository) }
