package com.patrickzedler.grocy.core.productcard

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository

/** What the stock pages open besides their own content. */
class StockPageActions(
    val overview: ProductOverviewViewModel,
    val shoppingList: AddToShoppingListViewModel,
    val labels: LabelPrintViewModel,
    val links: ProductLinks,
)

/**
 * The product overview, its shopping list form and label printing of a page, as ViewModels of
 * its navigation entry; [links] lead to the pages the overview's buttons open.
 */
@Composable
fun rememberStockPageActions(
    masterData: MasterDataRepository,
    stock: StockRepository,
    shoppingLists: ShoppingListRepository,
    links: ProductLinks,
): StockPageActions {
    // The app language, a change restarts the app, so the ViewModel keeps it
    val language = LocalLocale.current.toLanguageTag()
    return StockPageActions(
        overview = viewModel { ProductOverviewViewModel(masterData, language = language) },
        shoppingList = viewModel { AddToShoppingListViewModel(masterData, shoppingLists) },
        labels = viewModel { LabelPrintViewModel(stock) },
        links = links,
    )
}
