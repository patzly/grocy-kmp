package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_settings
import com.patrickzedler.grocy.core.resources.settings_select_hint
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.settings.api.GeneralSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsPageRoute
import com.patrickzedler.grocy.feature.settings.api.GrocySettingsRoute
import com.patrickzedler.grocy.feature.settings.api.LogRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.settings.api.ShoppingModeSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.StockSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.UserSettingsRoute
import com.patrickzedler.grocy.feature.settings.impl.general.GeneralSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.general.GeneralSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.grocy.GrocySettingsLinks
import com.patrickzedler.grocy.feature.settings.impl.grocy.GrocySettingsPageScreen
import com.patrickzedler.grocy.feature.settings.impl.grocy.GrocySettingsPageViewModel
import com.patrickzedler.grocy.feature.settings.impl.grocy.GrocySettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.grocy.GrocySettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.log.LogScreen
import com.patrickzedler.grocy.feature.settings.impl.log.LogViewModel
import com.patrickzedler.grocy.feature.settings.impl.server.ServerSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.server.ServerSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.shoppingmode.ShoppingModeSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.shoppingmode.ShoppingModeSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.stock.StockSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.stock.StockSettingsViewModel
import com.patrickzedler.grocy.feature.settings.impl.user.UserSettingsScreen
import com.patrickzedler.grocy.feature.settings.impl.user.UserSettingsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun EntryProviderScope<NavKey>.settingsEntries(
    dependencies: SettingsDependencies,
    navigator: Navigator,
) {
    val menuActions = SettingsMenuActions(
        onAboutClick = { navigator.navigate(AboutRoute) },
        onLogClick = { navigator.navigate(LogRoute) },
        logSource = dependencies.logReader.source,
    )

    entry<SettingsRoute>(
        metadata = listPaneMetadata(SettingsRoute) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_settings),
                text = stringResource(Res.string.settings_select_hint),
            )
        },
    ) { route ->
        SettingsScreen(
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            isBesideDetail = showsDetailBesideList(),
            shownCategory = settingsCategoryOf(navigator.currentRoute),
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

    entry<GrocySettingsRoute>(metadata = detailPaneMetadata(SettingsRoute)) {
        GrocySettingsScreen(
            viewModel = viewModel {
                GrocySettingsViewModel(dependencies.serverRepository.serverConfig)
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            // The pages take this page's place beside the categories, back returns to it
            links = GrocySettingsLinks(
                onStockSettingsClick = {
                    navigator.navigate(StockSettingsRoute(inSettings = true))
                },
                onPageClick = { navigator.navigate(GrocySettingsPageRoute(it)) },
                onUserSettingsClick = { navigator.navigate(UserSettingsRoute) },
            ),
            onBackClick = navigator::goBack,
            pane = ScreenPane.Detail.takeIf { showsDetailBesideList() },
        )
    }

    entry<GrocySettingsPageRoute>(metadata = detailPaneMetadata(SettingsRoute)) { route ->
        GrocySettingsPageScreen(
            viewModel = viewModel {
                GrocySettingsPageViewModel(dependencies.masterDataRepository, route.page)
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
            pane = ScreenPane.Detail.takeIf { showsDetailBesideList() },
        )
    }

    entry<UserSettingsRoute>(metadata = detailPaneMetadata(SettingsRoute)) {
        UserSettingsScreen(
            viewModel = viewModel {
                UserSettingsViewModel(
                    dependencies.masterDataRepository,
                    dependencies.serverRepository.server,
                )
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
            pane = ScreenPane.Detail.takeIf { showsDetailBesideList() },
        )
    }

    // Beside the categories when opened from Grocy's settings, or from the products in wide
    // windows together with both; a screen of its own from the products in narrow ones
    entry<StockSettingsRoute>(
        metadata = { route ->
            if (route.inSettings) {
                detailPaneMetadata(SettingsRoute, backLeavesScene = route.backLeavesSettings)
            } else {
                emptyMap()
            }
        },
    ) { route ->
        StockSettingsScreen(
            viewModel = viewModel { StockSettingsViewModel(dependencies.masterDataRepository) },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = {
                if (route.backLeavesSettings) {
                    navigator.goBack(from = SettingsRoute)
                } else {
                    navigator.goBack()
                }
            },
            pane = ScreenPane.Detail.takeIf { route.inSettings && showsDetailBesideList() },
        )
    }

    // Beside the categories, or from the shopping mode a screen of its own
    entry<ShoppingModeSettingsRoute>(
        metadata = { route ->
            if (route.inSettings) detailPaneMetadata(SettingsRoute) else emptyMap()
        },
    ) { route ->
        ShoppingModeSettingsScreen(
            viewModel = viewModel {
                ShoppingModeSettingsViewModel(dependencies.shoppingModeSettings)
            },
            menuViewModel = settingsMenuViewModel(dependencies),
            menuActions = menuActions,
            onBackClick = navigator::goBack,
            pane = ScreenPane.Detail.takeIf { route.inSettings && showsDetailBesideList() },
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
        SettingsCategory.GROCY -> GrocySettingsRoute
        SettingsCategory.SHOPPING_MODE -> ShoppingModeSettingsRoute()
        else -> null
    }

/** The category whose page [route] is or was opened from, null for other routes. */
internal fun settingsCategoryOf(route: NavKey): SettingsCategory? = when {
    route is StockSettingsRoute && route.inSettings -> SettingsCategory.GROCY
    route is GrocySettingsPageRoute || route == UserSettingsRoute -> SettingsCategory.GROCY
    else -> SettingsCategory.entries.firstOrNull { it.route != null && it.route == route }
}

@Composable
private fun settingsMenuViewModel(dependencies: SettingsDependencies): SettingsMenuViewModel =
    viewModel { SettingsMenuViewModel(dependencies.unlockRepository) }

private fun SettingsDependencies.demoServerSwitcher() =
    DemoServerSwitcher(authRepository, serverRepository)
