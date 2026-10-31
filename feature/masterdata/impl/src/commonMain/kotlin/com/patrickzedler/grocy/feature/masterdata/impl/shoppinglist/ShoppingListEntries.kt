package com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.design.component.DetailPlaceholder
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.listPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.ic_rounded_shopping_cart
import com.patrickzedler.grocy.core.resources.objects_select_hint
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.PurchaseRoute
import com.patrickzedler.grocy.feature.masterdata.api.ShoppingListFormRoute
import com.patrickzedler.grocy.feature.masterdata.api.ShoppingListItemRoute
import com.patrickzedler.grocy.feature.masterdata.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.masterdata.api.StockEntriesRoute
import com.patrickzedler.grocy.feature.masterdata.api.StockJournalRoute
import com.patrickzedler.grocy.feature.masterdata.api.StockJournalSummaryRoute
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataDependencies
import com.patrickzedler.grocy.feature.masterdata.impl.appLanguage
import com.patrickzedler.grocy.feature.masterdata.impl.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.impl.formFrame
import com.patrickzedler.grocy.feature.masterdata.impl.stockPageActions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// List, item forms and list forms share one list-detail scene
private const val SHOPPING_LIST_SCENE = "shoppinglist"

/**
 * Grocy's shopping list with its item and list forms, which open beside it where the window is
 * wide enough.
 */
internal fun EntryProviderScope<NavKey>.shoppingListEntries(
    dependencies: MasterDataDependencies,
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
            // Outside the product list the product's pages open on their own
            actions = stockPageActions(dependencies, navigator),
            links = ShoppingListLinks(
                onAddItemClick = { navigator.openDetail(ShoppingListItemRoute(listId = it)) },
                onItemClick = { navigator.openDetail(ShoppingListItemRoute(id = it)) },
                onNewListClick = { navigator.openDetail(ShoppingListFormRoute()) },
                onEditListClick = { navigator.openDetail(ShoppingListFormRoute(it)) },
                onPurchase = { purchase ->
                    navigator.navigate(
                        PurchaseRoute(
                            productId = purchase.productId,
                            amount = purchase.amount,
                            quId = purchase.quId,
                            shoppingListItemId = purchase.itemId,
                        ),
                    )
                },
            ),
            menuItems = appMenuItems(navigator),
            onBackClick = { navigator.goBack(from = route) },
            selectedItemId = (navigator.currentRoute as? ShoppingListItemRoute)?.id,
            isBesideDetail = showsDetailBesideList(),
        )
    }

    entry<ShoppingListItemRoute>(metadata = detailPaneMetadata(SHOPPING_LIST_SCENE)) { route ->
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
            frame = formFrame(navigator, route, MasterDataRepository.SHOPPING_LIST, repository) {
                ShoppingListItemRoute(it)
            },
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
