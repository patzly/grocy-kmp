package com.patrickzedler.grocy.feature.stock.impl.booking

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.navigation.Navigator
import com.patrickzedler.grocy.core.navigation.detailPaneMetadata
import com.patrickzedler.grocy.core.navigation.showsDetailBesideList
import com.patrickzedler.grocy.core.ui.appMenuItems
import com.patrickzedler.grocy.feature.masterdata.api.ProductRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.BookingRoute
import com.patrickzedler.grocy.feature.stock.api.ConsumeRoute
import com.patrickzedler.grocy.feature.stock.api.InventoryRoute
import com.patrickzedler.grocy.feature.stock.api.PurchaseRoute
import com.patrickzedler.grocy.feature.stock.api.TransferRoute
import com.patrickzedler.grocy.feature.stock.impl.StockDependencies
import com.patrickzedler.grocy.feature.stock.impl.stockPageActions

/**
 * Grocy's stock pages Purchase, Consume, Transfer and Inventory: beside the start screen when
 * opened from its buttons, else each a screen of its own.
 */
internal fun EntryProviderScope<NavKey>.bookingEntries(
    dependencies: StockDependencies,
    navigator: Navigator,
) {
    entry<PurchaseRoute>(metadata = { bookingMetadata(it) }) { route ->
        BookingEntry(
            kind = BookingKind.PURCHASE,
            preset = BookingPreset(
                productId = route.productId,
                amount = route.amount,
                quId = route.quId,
                shoppingListItemId = route.shoppingListItemId,
                shoppingListPosition = route.shoppingListPosition,
                shoppingListCount = route.shoppingListCount,
            ),
            besideStart = route.besideStart,
            dependencies = dependencies,
            navigator = navigator,
        )
    }
    entry<ConsumeRoute>(metadata = { bookingMetadata(it) }) { route ->
        BookingEntry(
            kind = BookingKind.CONSUME,
            preset = BookingPreset(
                productId = route.productId,
                locationId = route.locationId,
                stockId = route.stockId,
            ),
            besideStart = route.besideStart,
            dependencies = dependencies,
            navigator = navigator,
        )
    }
    entry<TransferRoute>(metadata = { bookingMetadata(it) }) { route ->
        BookingEntry(
            kind = BookingKind.TRANSFER,
            preset = BookingPreset(
                productId = route.productId,
                locationId = route.locationId,
                stockId = route.stockId,
            ),
            besideStart = route.besideStart,
            dependencies = dependencies,
            navigator = navigator,
        )
    }
    entry<InventoryRoute>(metadata = { bookingMetadata(it) }) { route ->
        BookingEntry(
            kind = BookingKind.INVENTORY,
            preset = BookingPreset(route.productId),
            besideStart = route.besideStart,
            dependencies = dependencies,
            navigator = navigator,
        )
    }
}

private fun bookingMetadata(route: BookingRoute): Map<String, Any> =
    if (route.besideStart) detailPaneMetadata(StartRoute) else emptyMap()

@Composable
private fun BookingEntry(
    kind: BookingKind,
    preset: BookingPreset,
    besideStart: Boolean,
    dependencies: StockDependencies,
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
        menuItems = appMenuItems(),
        onBackClick = navigator::goBack,
        // Grocy's "Add as new product": the barcode waits for the product to be chosen
        onNewProductClick = { navigator.navigate(ProductRoute()) },
        onDone = navigator::goBack,
        pane = ScreenPane.Detail.takeIf { besideStart && showsDetailBesideList() },
    )
}
