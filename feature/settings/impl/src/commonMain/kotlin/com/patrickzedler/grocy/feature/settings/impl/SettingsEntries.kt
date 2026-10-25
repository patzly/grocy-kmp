package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.settings.api.StockSettingsRoute
import com.patrickzedler.grocy.feature.settings.impl.general.GeneralSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.general.GeneralSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.log.LogScreen
import com.patrickzedler.grocy.feature.settings.impl.log.LogViewModel
import com.patrickzedler.grocy.feature.settings.impl.server.ServerSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.server.ServerSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.stock.StockSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.stock.StockSettingsViewModel

fun EntryProviderScope<NavKey>.settingsEntries(
    dependencies: SettingsDependencies,
    navigator: Navigator,
) {
    val menuActions = SettingsMenuActions(
        onAboutClick = { navigator.navigate(AboutRoute) },
        onLogClick = { navigator.navigate(LogRoute) },
        logSource = dependencies.logReader.source,
    )

    // Alone until a category is chosen, wide windows then show its page beside the categories
    entry<SettingsRoute>(metadata = listPaneMetadata(SettingsRoute, placeholder = null)) { route ->
        SettingsScreen(
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            shownCategory = settingsCategoryOf(navigator.currentRoute)
                ?.takeIf { showsDetailBesideList() },
            onBackClick = { navigator.goBack(from = route) },
            // The other pages follow one by one
            onCategoryClick = { category -> category.route?.let(navigator::openDetail) },
            onBackupClick = {},
            onResetClick = {},
        )
    }

    entry<GeneralSettingsRoute>(metadata = detailPaneMetadata(SettingsRoute)) {
        GeneralSettingsScreen(
            viewModel = viewModel {
                GeneralSettingsViewModel(
                    appearanceRepository = dependencies.appearanceRepository,
                    languageStore = dependencies.appLanguageStore,
                    server = dependencies.serverRepository.server,
                    demoServerSwitcher = dependencies.demoServerSwitcher(),
                )
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
            pane = ScreenPane.Detail.takeIf { showsDetailBesideList() },
        )
    }

    entry<ServerSettingsRoute>(metadata = detailPaneMetadata(SettingsRoute)) {
        ServerSettingsScreen(
            viewModel = viewModel {
                ServerSettingsViewModel(
                    serverRepository = dependencies.serverRepository,
                    authRepository = dependencies.authRepository,
                    demoServerSwitcher = dependencies.demoServerSwitcher(),
                )
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
            onRestart = dependencies::restartApp,
            pane = ScreenPane.Detail.takeIf { showsDetailBesideList() },
        )
    }

    entry<StockSettingsRoute> {
        StockSettingsScreen(
            viewModel = viewModel { StockSettingsViewModel(dependencies.masterDataRepository) },
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

/** The page a category opens, null while it has none. */
internal val SettingsCategory.route: DetailRoute?
    get() = when (this) {
        SettingsCategory.GENERAL -> GeneralSettingsRoute
        SettingsCategory.SERVER -> ServerSettingsRoute
        else -> null
    }

/** The category whose page [route] is, null for other routes. */
internal fun settingsCategoryOf(route: NavKey): SettingsCategory? =
    SettingsCategory.entries.firstOrNull { it.route != null && it.route == route }

@Composable
private fun settingsMenuViewModel(dependencies: SettingsDependencies): SettingsMenuViewModel =
    viewModel { SettingsMenuViewModel(dependencies.unlockRepository) }

private fun SettingsDependencies.demoServerSwitcher() =
    DemoServerSwitcher(authRepository, serverRepository)
