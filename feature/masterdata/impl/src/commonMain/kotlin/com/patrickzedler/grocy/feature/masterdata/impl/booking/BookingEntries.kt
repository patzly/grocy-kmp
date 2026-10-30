package com.patrickzedler.grocy.feature.masterdata.impl.booking

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.feature.masterdata.api.ConsumeRoute
import com.patrickzedler.grocy.feature.masterdata.api.InventoryRoute
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.masterdata.api.PurchaseRoute
import com.patrickzedler.grocy.feature.masterdata.api.TransferRoute
import com.patrickzedler.grocy.feature.masterdata.impl.MasterDataDependencies
import com.patrickzedler.grocy.feature.masterdata.impl.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.impl.stockPageActions

/** The route of the stock page of [kind] for the product [productId]. */
internal fun bookingRoute(kind: BookingKind, productId: Int?): Route = when (kind) {
    BookingKind.PURCHASE -> PurchaseRoute(productId)
    BookingKind.CONSUME -> ConsumeRoute(productId)
    BookingKind.TRANSFER -> TransferRoute(productId)
    BookingKind.INVENTORY -> InventoryRoute(productId)
}

/** Grocy's stock pages Purchase, Consume, Transfer and Inventory, each a screen of its own. */
internal fun EntryProviderScope<NavKey>.bookingEntries(
    dependencies: MasterDataDependencies,
    navigator: Navigator,
) {
    entry<PurchaseRoute> { route ->
        BookingEntry(
            kind = BookingKind.PURCHASE,
            preset = BookingPreset(
                productId = route.productId,
                amount = route.amount,
                quId = route.quId,
                shoppingListItemId = route.shoppingListItemId,
            ),
            dependencies = dependencies,
            navigator = navigator,
        )
    }
    entry<ConsumeRoute> { route ->
        BookingEntry(
            kind = BookingKind.CONSUME,
            preset = BookingPreset(
                productId = route.productId,
                locationId = route.locationId,
                stockId = route.stockId,
            ),
            dependencies = dependencies,
            navigator = navigator,
        )
    }
    entry<TransferRoute> { route ->
        BookingEntry(
            kind = BookingKind.TRANSFER,
            preset = BookingPreset(
                productId = route.productId,
                locationId = route.locationId,
                stockId = route.stockId,
            ),
            dependencies = dependencies,
            navigator = navigator,
        )
    }
    entry<InventoryRoute> { route ->
        BookingEntry(
            kind = BookingKind.INVENTORY,
            preset = BookingPreset(route.productId),
            dependencies = dependencies,
            navigator = navigator,
        )
    }
}

@Composable
private fun BookingEntry(
    kind: BookingKind,
    preset: BookingPreset,
    dependencies: MasterDataDependencies,
    navigator: Navigator,
) {
    val shoppingLists = dependencies.shoppingListRepository
    BookingScreen(
        viewModel = viewModel {
            BookingViewModel(
                kind = kind,
                masterData = dependencies.masterDataRepository,
                stock = dependencies.stockRepository,
                preset = preset,
                removeShoppingListItem = { shoppingLists.deleteItem(it) },
            )
        },
        actions = stockPageActions(dependencies, navigator),
        menuItems = appMenuItems(navigator),
        onBackClick = navigator::goBack,
        // Grocy's "Add as new product": the barcode waits for the product to be chosen
        onNewProductClick = { navigator.navigate(ProductRoute()) },
        onDone = navigator::goBack,
    )
}
