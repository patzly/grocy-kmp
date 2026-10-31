package com.patrickzedler.grocy.core.productcard

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.scanner.writer.Grocycode

/** Where Grocy's product pages lead: its form, its stock entries and its journal. */
class ProductLinks(
    val onEditClick: (productId: Int) -> Unit,
    val onStockEntriesClick: (productId: Int) -> Unit,
    val onStockJournalClick: (productId: Int) -> Unit,
    val onStockJournalSummaryClick: (productId: Int) -> Unit,
    /** Grocy's stock pages for the product: purchase, consume, transfer, inventory. */
    val onBookingClick: (productId: Int, kind: BookingKind) -> Unit,
)

/**
 * Grocy's "Product overview" of the product [overview] shows, with what its buttons open: the
 * shopping list form on top of it, the other pages instead of it. Printed grocycodes go through
 * [labels], whose results the screen shows (`LabelPrintEffect`). [nameOf] names the product in
 * the first frame.
 */
@Composable
fun ProductOverviewHost(
    overview: ProductOverviewViewModel,
    shoppingList: AddToShoppingListViewModel,
    labels: LabelPrintViewModel,
    snackbarHostState: SnackbarHostState,
    links: ProductLinks,
    nameOf: (productId: Int) -> String,
) {
    val productId by overview.productId.collectAsStateWithLifecycle()
    val downloadGrocycode = rememberGrocycodeDownload(snackbarHostState)
    // The pages open instead of the overview, what stays on this screen keeps it open
    fun leavingTo(link: (Int) -> Unit): (Int) -> Unit = { id ->
        overview.hide()
        link(id)
    }
    productId?.let { id ->
        ProductOverviewDialog(
            productId = id,
            name = nameOf(id),
            viewModel = overview,
            actions = OverviewActions(
                onEditClick = leavingTo(links.onEditClick),
                onAddToShoppingListClick = shoppingList::open,
                onStockEntriesClick = leavingTo(links.onStockEntriesClick),
                onStockJournalClick = leavingTo(links.onStockJournalClick),
                onStockJournalSummaryClick = leavingTo(links.onStockJournalSummaryClick),
                onBookingClick = { productId, kind ->
                    overview.hide()
                    links.onBookingClick(productId, kind)
                },
                onGrocycodeDownload = { productId, twoDimensional ->
                    downloadGrocycode(Grocycode.product(productId), twoDimensional)
                },
                onGrocycodePrint = labels::printProduct,
            ),
            onDismissRequest = overview::hide,
        )
    }
    AddToShoppingListHost(shoppingList, snackbarHostState)
}
