package com.patrickzedler.grocy.feature.shoppinglist.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.forms.formFrame
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.productcard.ProductLinks
import com.patrickzedler.grocy.core.productcard.StockPageActions
import com.patrickzedler.grocy.core.productcard.rememberStockPageActions
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.objects_select_hint
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.settings.api.ShoppingModeSettingsRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListFormRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListItemRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingModeRoute
import com.patrickzedler.grocy.feature.stock.api.PurchaseRoute
import com.patrickzedler.grocy.feature.stock.api.StockEntriesRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalSummaryRoute
import com.patrickzedler.grocy.feature.stock.api.bookingRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// List, item forms and list forms share one list-detail scene
private const val SHOPPING_LIST_SCENE = "shoppinglist"

/**
 * Grocy's shopping list with its item and list forms, which open beside it where the window is
 * wide enough.
 */
fun EntryProviderScope<NavKey>.shoppingListEntries(
    dependencies: ShoppingListDependencies,
    navigator: Navigator,
) {
    val repository = dependencies.masterDataRepository
    val shoppingLists = dependencies.shoppingListRepository

    entry<ShoppingListRoute>(
        metadata = listPaneMetadata(SHOPPING_LIST_SCENE) {
            DetailPlaceholder(
                icon = painterResource(Res.drawable.ic_rounded_shopping_cart),
                text = stringResource(Res.string.objects_select_hint),
            )
        },
    ) { route ->
        val language = appLanguage()
        ShoppingListScreen(
            viewModel = viewModel {
                ShoppingListViewModel(
                    listId = route.listId,
                    masterData = repository,
                    stock = dependencies.stockRepository,
                    shoppingLists = shoppingLists,
                    optionsRepository = dependencies.shoppingListOptions,
                    language = language,
                )
            },
            actions = stockPageActions(dependencies, navigator),
            links = ShoppingListLinks(
                onAddItemClick = { navigator.openDetail(ShoppingListItemRoute(listId = it)) },
                onItemClick = { navigator.openDetail(ShoppingListItemRoute(id = it)) },
                onNewListClick = { navigator.openDetail(ShoppingListFormRoute()) },
                onEditListClick = { navigator.openDetail(ShoppingListFormRoute(it)) },
                onShoppingModeClick = { navigator.navigate(ShoppingModeRoute) },
                onPurchase = { purchase, progress ->
                    navigator.navigate(
                        PurchaseRoute(
                            productId = purchase.productId,
                            amount = purchase.amount,
                            quId = purchase.quId,
                            shoppingListItemId = purchase.itemId,
                            shoppingListPosition = progress?.position,
                            shoppingListCount = progress?.count,
                        ),
                    )
                },
            ),
            menuItems = appMenuItems(),
            onBackClick = { navigator.goBack(from = route) },
            selectedItemId = (navigator.currentRoute as? ShoppingListItemRoute)?.id,
            isBesideDetail = showsDetailBesideList(),
            // The back stack changes once a back gesture is done, not while it shows the list
            isShown = navigator.currentRoute.let {
                it == route || it is ShoppingListItemRoute || it is ShoppingListFormRoute
            },
        )
    }

    // A screen of its own, the store needs the whole window
    entry<ShoppingModeRoute> {
        val language = appLanguage()
        ShoppingModeScreen(
            viewModel = viewModel {
                ShoppingModeViewModel(
                    masterData = repository,
                    stock = dependencies.stockRepository,
                    shoppingLists = shoppingLists,
                    optionsRepository = dependencies.shoppingListOptions,
                    settingsRepository = dependencies.shoppingModeSettings,
                    language = language,
                )
            },
            onCloseClick = navigator::goBack,
            onSettingsClick = {
                navigator.navigate(ShoppingModeSettingsRoute(inSettings = false))
            },
        )
    }

    entry<ShoppingListItemRoute>(
        metadata = { route ->
            if (route.inList) detailPaneMetadata(SHOPPING_LIST_SCENE) else emptyMap()
        },
    ) { route ->
        ShoppingListItemScreen(
            viewModel = viewModel {
                ShoppingListItemViewModel(
                    id = route.id,
                    presetListId = route.listId,
                    presetProductId = route.productId,
                    masterData = repository,
                    shoppingLists = shoppingLists,
                )
            },
            frame = formFrame(
                navigator = navigator,
                route = route,
                entity = MasterDataRepository.SHOPPING_LIST,
                repository = repository,
                inList = route.inList,
            ) { ShoppingListItemRoute(it) },
        )
    }

    entry<ShoppingListFormRoute>(metadata = detailPaneMetadata(SHOPPING_LIST_SCENE)) { route ->
        ShoppingListFormScreen(
            viewModel = viewModel {
                ShoppingListFormViewModel(route.id, repository, shoppingLists)
            },
            frame = formFrame(
                navigator = navigator,
                route = route,
                entity = ShoppingListRepository.SHOPPING_LISTS,
                repository = repository,
            ) { ShoppingListFormRoute(it) },
        )
    }
}

/** The product overview, shopping list and labels; the product's pages open on their own. */
@Composable
private fun stockPageActions(
    dependencies: ShoppingListDependencies,
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
private fun appLanguage(): String = LocalLocale.current.toLanguageTag()
