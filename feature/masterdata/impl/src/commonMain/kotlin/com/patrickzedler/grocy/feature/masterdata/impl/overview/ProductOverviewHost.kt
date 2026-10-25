package com.patrickzedler.grocy.feature.masterdata.impl.overview

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist.AddToShoppingListHost
import com.patrickzedler.grocy.feature.masterdata.impl.shoppinglist.AddToShoppingListViewModel

/** Where Grocy's product pages lead: its form, its stock entries and its journal. */
internal class ProductLinks(
    val onEditClick: (productId: Int) -> Unit,
    val onStockEntriesClick: (productId: Int) -> Unit,
    val onStockJournalClick: (productId: Int) -> Unit,
    val onStockJournalSummaryClick: (productId: Int) -> Unit,
)

/**
 * Grocy's "Product overview" of the product [overview] shows, with what its buttons open: the
 * shopping list form on top of it, the other pages instead of it. [nameOf] names the product in
 * the first frame.
 */
@Composable
internal fun ProductOverviewHost(
    overview: ProductOverviewViewModel,
    shoppingList: AddToShoppingListViewModel,
    snackbarHostState: SnackbarHostState,
    links: ProductLinks,
    nameOf: (productId: Int) -> String,
) {
    val productId by overview.productId.collectAsStateWithLifecycle()
    productId?.let { id ->
        ProductOverviewDialog(
            productId = id,
            name = nameOf(id),
            viewModel = overview,
            onEditClick = {
                overview.hide()
                links.onEditClick(it)
            },
            onAddToShoppingListClick = shoppingList::open,
            onStockEntriesClick = {
                overview.hide()
                links.onStockEntriesClick(it)
            },
            onStockJournalClick = {
                overview.hide()
                links.onStockJournalClick(it)
            },
            onDismissRequest = overview::hide,
        )
    }
    AddToShoppingListHost(shoppingList, snackbarHostState)
}
