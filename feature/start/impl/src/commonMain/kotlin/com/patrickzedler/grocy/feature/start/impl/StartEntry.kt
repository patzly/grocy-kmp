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
import com.patrickzedler.grocy.feature.recipes.api.RecipesRoute
import com.patrickzedler.grocy.feature.settings.api.ServerSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListItemRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingModeRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.BookingRoute
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
        val isBesideDetail = showsDetailBesideList()
        StartScreen(
            viewModel = viewModel {
                StartViewModel(
                    repository = dependencies.masterDataRepository,
                    stock = dependencies.stockRepository,
                    shoppingLists = dependencies.shoppingListRepository,
                    recipes = dependencies.recipesRepository,
                    serverConfig = dependencies.serverConfig,
                )
            },
            isDemoServer = server?.isDemo == true,
            isBesideDetail = isBesideDetail,
            shownSection = startSectionOf(navigator.currentRoute),
            // The shopping list and the recipes are list-detail scenes of their own
            onSectionClick = { section ->
                when (val route = section.route) {
                    is DetailRoute -> navigator.openDetail(route)
                    else -> navigator.navigate(route)
                }
            },
            onSettingsClick = { navigator.navigate(SettingsRoute) },
            // Beside the start screen like its sections, in place of the one shown there
            onStockActionClick = { action -> navigator.openDetail(action.route) },
            // In wide windows beside the shopping list, else a quick add of its own
            onShoppingListAddClick = { listId ->
                navigator.navigateAll(shoppingListAddRoutes(listId, isBesideDetail))
            },
            onShoppingModeClick = { navigator.navigate(ShoppingModeRoute) },
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
        StartSection.RECIPES -> RecipesRoute()
        StartSection.MASTER_DATA -> MasterDataRoute
    }

/** The stock page of the start screen's button [this], beside the start screen. */
internal val StockAction.route: BookingRoute
    get() = when (this) {
        StockAction.PURCHASE -> PurchaseRoute(besideStart = true)
        StockAction.CONSUME -> ConsumeRoute(besideStart = true)
        StockAction.TRANSFER -> TransferRoute(besideStart = true)
        StockAction.INVENTORY -> InventoryRoute(besideStart = true)
    }

/**
 * What "Add item" of the start screen's shopping list [listId] opens: where details show
 * beside their list ([isBesideDetail]) the shopping list with the new item beside it, else
 * the item form alone, which stays a screen of its own when the window grows.
 */
internal fun shoppingListAddRoutes(listId: Int?, isBesideDetail: Boolean): List<Route> =
    if (isBesideDetail) {
        listOf(ShoppingListRoute(listId), ShoppingListItemRoute(listId = listId))
    } else {
        listOf(ShoppingListItemRoute(listId = listId, inList = false))
    }

/** The section whose page [route] is, null for other routes. */
internal fun startSectionOf(route: NavKey): StartSection? =
    StartSection.entries.firstOrNull { it.route == route }
