package com.patrickzedler.grocy.feature.stock.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.productcard.ProductLinks
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.productcard.rememberStockPageActions
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipesRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.StockEntriesRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalSummaryRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.stock.api.bookingRoute
import com.patrickzedler.grocy.feature.stock.impl.booking.bookingEntries
import com.patrickzedler.grocy.feature.stock.impl.journal.StockJournalScreen
import com.patrickzedler.grocy.feature.stock.impl.journal.StockJournalViewModel
import com.patrickzedler.grocy.feature.stock.impl.stock.StockEntriesScreen
import com.patrickzedler.grocy.feature.stock.impl.stock.StockEntriesViewModel
import com.patrickzedler.grocy.feature.stock.impl.stockoverview.StockOverviewItemActions
import com.patrickzedler.grocy.feature.stock.impl.stockoverview.StockOverviewLinks
import com.patrickzedler.grocy.feature.stock.impl.stockoverview.StockOverviewScreen
import com.patrickzedler.grocy.feature.stock.impl.stockoverview.StockOverviewViewModel

/** Grocy's stock pages: the stock overview, stock entries, the journal and the bookings. */
fun EntryProviderScope<NavKey>.stockEntries(
    dependencies: StockDependencies,
    navigator: Navigator,
) {
    val repository = dependencies.masterDataRepository
    bookingEntries(dependencies, navigator)

    // Beside the start screen like the master data, its bar has back and the app menu
    entry<StockOverviewRoute>(metadata = detailPaneMetadata(StartRoute)) {
        val isBesideList = showsDetailBesideList()
        val language = appLanguage()
        val actions = stockPageActions(dependencies, navigator)
        // Outside the product list the form opens on its own
        val openProduct = { id: Int -> navigator.navigate(ProductRoute(id)) }
        val stockOverview = viewModel {
            StockOverviewViewModel(
                repository,
                dependencies.stockRepository,
                dependencies.stockOverviewOptions,
                language = language,
            )
        }
        StockOverviewScreen(
            viewModel = stockOverview,
            actions = actions,
            // Grocy's reports and scanning do not exist in the app yet
            links = StockOverviewLinks(
                onJournalClick = { navigator.navigate(StockJournalRoute()) },
                onStockEntriesClick = { navigator.navigate(StockEntriesRoute()) },
                onReportsClick = {},
                onScanClick = {},
            ),
            itemActions = StockOverviewItemActions(
                onClick = { actions.overview.show(it.productId) },
                onConsume = stockOverview::consume,
                onOpen = stockOverview::open,
                onConsumeAll = stockOverview::consumeAll,
                onConsumeSpoiled = stockOverview::consumeSpoiled,
                onAddToShoppingList = { actions.shoppingList.open(it.productId) },
                // Grocy's link searches the recipes for the product's name
                onSearchRecipes = { navigator.navigate(RecipesRoute(search = it.name)) },
                onEditProduct = { openProduct(it.productId) },
            ),
            menuItems = if (isBesideList) emptyList() else appMenuItems(),
            onBackClick = navigator::goBack,
            pane = ScreenPane.Detail.takeIf { isBesideList },
        )
    }

    // Grocy's stock pages of a product, or of all products
    entry<StockEntriesRoute> { route ->
        val language = appLanguage()
        StockEntriesScreen(
            viewModel = viewModel {
                StockEntriesViewModel(
                    route.productId,
                    repository,
                    dependencies.stockRepository,
                    language = language,
                )
            },
            actions = stockPageActions(dependencies, navigator),
            menuItems = appMenuItems(),
            onBackClick = navigator::goBack,
        )
    }
    entry<StockJournalRoute> { route ->
        StockJournalEntry(route.productId, summary = false, dependencies, navigator)
    }
    entry<StockJournalSummaryRoute> { route ->
        StockJournalEntry(route.productId, summary = true, dependencies, navigator)
    }
}

@Composable
private fun StockJournalEntry(
    productId: Int?,
    summary: Boolean,
    dependencies: StockDependencies,
    navigator: Navigator,
) {
    val language = appLanguage()
    StockJournalScreen(
        viewModel = viewModel {
            StockJournalViewModel(
                productId = productId,
                summary = summary,
                masterData = dependencies.masterDataRepository,
                stock = dependencies.stockRepository,
                language = language,
            )
        },
        actions = stockPageActions(dependencies, navigator),
        menuItems = appMenuItems(),
        onBackClick = navigator::goBack,
    )
}

/** The product overview, shopping list and labels of a stock page; the form opens on its own. */
@Composable
internal fun stockPageActions(
    dependencies: StockDependencies,
    navigator: Navigator,
): StockPageActions = rememberStockPageActions(
    masterData = dependencies.masterDataRepository,
    stock = dependencies.stockRepository,
    shoppingLists = dependencies.shoppingListRepository,
    links = ProductLinks(
        onEditClick = { navigator.navigate(ProductRoute(it)) },
        onStockEntriesClick = { navigator.navigate(StockEntriesRoute(it)) },
        onStockJournalClick = { navigator.navigate(StockJournalRoute(it)) },
        onStockJournalSummaryClick = { navigator.navigate(StockJournalSummaryRoute(it)) },
        onBookingClick = { id, kind -> navigator.navigate(bookingRoute(kind, id)) },
    ),
)

// The app language, a change restarts the app, so ViewModels keep it
@Composable
internal fun appLanguage(): String = LocalLocale.current.toLanguageTag()
