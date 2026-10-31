package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_home
import com.patrickzedler.grocy.core.resources.start_select_hint
import com.patrickzedler.grocy.feature.about.api.AboutRoute
import com.patrickzedler.grocy.feature.masterdata.api.MasterDataRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListItemRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.ConsumeRoute
import com.patrickzedler.grocy.feature.stock.api.InventoryRoute
import com.patrickzedler.grocy.feature.stock.api.PurchaseRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.stock.api.TransferRoute
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
        val server by dependencies.server.collectAsStateWithLifecycle()
        StartScreen(
            viewModel = viewModel {
                StartViewModel(
                    dependencies.masterDataRepository,
                    dependencies.stockRepository,
                    dependencies.shoppingListRepository,
                    dependencies.serverConfig,
                )
            },
            isDemoServer = server?.isDemo == true,
            isBesideDetail = showsDetailBesideList(),
            shownSection = startSectionOf(navigator.currentRoute),
            // The shopping list is a list-detail scene of its own, its forms open beside it
            onSectionClick = { section ->
                when (val route = section.route) {
                    is DetailRoute -> navigator.openDetail(route)
                    else -> navigator.navigate(route)
                }
            },
            onSettingsClick = { navigator.navigate(SettingsRoute) },
            // Like reached through the settings, back returns there
            onStockActionClick = { action ->
                navigator.navigate(
                    when (action) {
                        StockAction.PURCHASE -> PurchaseRoute()
                        StockAction.CONSUME -> ConsumeRoute()
                        StockAction.TRANSFER -> TransferRoute()
                        StockAction.INVENTORY -> InventoryRoute()
                    },
                )
            },
            onShoppingListAddClick = { listId ->
                navigator.navigate(ShoppingListItemRoute(listId = listId))
            },
            onDemoWarningClick = {
                navigator.navigateAll(listOf(SettingsRoute, ServerSettingsRoute))
            },
            onAboutClick = { navigator.navigate(AboutRoute) },
        )
    }
}

/** The page a section opens: beside the start screen for a [DetailRoute], else on its own. */
internal val StartSection.route: Route
    get() = when (this) {
        StartSection.STOCK_OVERVIEW -> StockOverviewRoute
        StartSection.SHOPPING_LIST -> ShoppingListRoute()
        StartSection.MASTER_DATA -> MasterDataRoute
    }

/** The section whose page [route] is, null for other routes. */
internal fun startSectionOf(route: NavKey): StartSection? =
    StartSection.entries.firstOrNull { it.route == route }
