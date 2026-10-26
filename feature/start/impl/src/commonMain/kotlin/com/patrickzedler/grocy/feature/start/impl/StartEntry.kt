package com.patrickzedler.grocy.feature.start.impl

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_home
import com.patrickzedler.grocy.core.resources.start_select_hint
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.masterdata.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun EntryProviderScope<NavKey>.startEntry(
    dependencies: StartDependencies,
    navigator: Navigator,
) {
    entry<StartRoute>(
        metadata = listPaneMetadata(StartRoute) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_home),
                text = stringResource(Res.string.start_select_hint),
            )
        },
    ) {
        StartScreen(
            viewModel = viewModel {
                StartViewModel(dependencies.masterDataRepository, dependencies.serverConfig)
            },
            isBesideDetail = showsDetailBesideList(),
            shownSection = startSectionOf(navigator.currentRoute),
            onSectionClick = { section -> navigator.openDetail(section.route) },
            onSettingsClick = { navigator.navigate(SettingsRoute) },
            onAboutClick = { navigator.navigate(AboutRoute) },
        )
    }
}

/** The page a section opens beside the start screen. */
internal val StartSection.route: DetailRoute
    get() = when (this) {
        StartSection.STOCK_OVERVIEW -> StockOverviewRoute
        StartSection.MASTER_DATA -> MasterDataRoute
    }

/** The section whose page [route] is, null for other routes. */
internal fun startSectionOf(route: NavKey): StartSection? =
    StartSection.entries.firstOrNull { it.route == route }
