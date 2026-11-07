package com.patrickzedler.grocy.feature.stock.api

import androidx.navigation3.runtime.NavKey
import com.patrickzedler.grocy.core.model.stock.BookingKind
import com.patrickzedler.grocy.core.navigation.DetailRoute
import com.patrickzedler.grocy.core.navigation.Route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Grocy's "Stock overview": every product in stock or below its min. stock amount; a detail of
 * the start screen, beside it where the window is wide enough.
 */
@Serializable
@SerialName("stockoverview")
data object StockOverviewRoute : DetailRoute

/** Grocy's "Stock entries", only those of [productId] if given. */
@Serializable
@SerialName("stockentries")
data class StockEntriesRoute(val productId: Int? = null) : Route

/** Grocy's "Stock journal", only the bookings of [productId] if given. */
@Serializable
@SerialName("stockjournal")
data class StockJournalRoute(val productId: Int? = null) : Route

/** Grocy's "Stock journal summary", only of [productId] if given. */
@Serializable
@SerialName("stockjournal/summary")
data class StockJournalSummaryRoute(val productId: Int? = null) : Route

/**
 * Grocy's "Purchase", the product [productId] preselected if given. From a shopping list item
 * ([shoppingListItemId]) with its [amount] (stock unit) in the unit [quId]; the item is removed
 * once the purchase is booked, like Grocy's "Add this item to stock". "Add all list items to
 * stock" books item [shoppingListPosition] of [shoppingListCount]. [besideStart] as for every
 * stock page.
 */
@Serializable
@SerialName("purchase")
data class PurchaseRoute(
    val productId: Int? = null,
    val amount: Double? = null,
    val quId: Int? = null,
    val shoppingListItemId: Int? = null,
    val shoppingListPosition: Int? = null,
    val shoppingListCount: Int? = null,
    override val besideStart: Boolean = false,
) : BookingRoute

/**
 * Grocy's "Consume"; [locationId] and [stockId] preselect a location and a stock entry.
 * [besideStart] as for every stock page.
 */
@Serializable
@SerialName("consume")
data class ConsumeRoute(
    val productId: Int? = null,
    val locationId: Int? = null,
    val stockId: String? = null,
    override val besideStart: Boolean = false,
) : BookingRoute

/**
 * Grocy's "Transfer"; [locationId] and [stockId] preselect the source and a stock entry.
 * [besideStart] as for every stock page.
 */
@Serializable
@SerialName("transfer")
data class TransferRoute(
    val productId: Int? = null,
    val locationId: Int? = null,
    val stockId: String? = null,
    override val besideStart: Boolean = false,
) : BookingRoute

/**
 * Grocy's "Inventory", the product [productId] preselected if given. [besideStart] as for every
 * stock page.
 */
@Serializable
@SerialName("inventory")
data class InventoryRoute(
    val productId: Int? = null,
    override val besideStart: Boolean = false,
) : BookingRoute

/**
 * Scanning first: a scanned product, then the booking the user chooses for it (purchase,
 * consume, transfer or inventory) with that booking's fields. The stock overview's main action.
 */
@Serializable
@SerialName("scan")
data object StockScanRoute : Route

/** Grocy's "Location Content Sheet": every location with the products in stock there. */
@Serializable
@SerialName("locationcontentsheet")
data object LocationContentSheetRoute : Route

/** Grocy's stock report "Spendings": what purchases cost in a date range, as a pie chart. */
@Serializable
@SerialName("stockreports/spendings")
data object StockSpendingsRoute : Route

/**
 * One of Grocy's stock pages Purchase, Consume, Transfer and Inventory: opened from the start
 * screen's buttons ([besideStart]) the page shows beside the start screen where the window is
 * wide enough, replacing the section shown there; from elsewhere a screen of its own.
 */
sealed interface BookingRoute : DetailRoute {
    val besideStart: Boolean
}

/** The route of Grocy's stock page of [kind], the product [productId] preselected if given. */
fun bookingRoute(kind: BookingKind, productId: Int? = null): Route = when (kind) {
    BookingKind.PURCHASE -> PurchaseRoute(productId)
    BookingKind.CONSUME -> ConsumeRoute(productId)
    BookingKind.TRANSFER -> TransferRoute(productId)
    BookingKind.INVENTORY -> InventoryRoute(productId)
}

val stockRouteSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(StockOverviewRoute::class, StockOverviewRoute.serializer())
        subclass(StockEntriesRoute::class, StockEntriesRoute.serializer())
        subclass(StockJournalRoute::class, StockJournalRoute.serializer())
        subclass(StockJournalSummaryRoute::class, StockJournalSummaryRoute.serializer())
        subclass(PurchaseRoute::class, PurchaseRoute.serializer())
        subclass(ConsumeRoute::class, ConsumeRoute.serializer())
        subclass(TransferRoute::class, TransferRoute.serializer())
        subclass(InventoryRoute::class, InventoryRoute.serializer())
        subclass(StockScanRoute::class, StockScanRoute.serializer())
        subclass(LocationContentSheetRoute::class, LocationContentSheetRoute.serializer())
        subclass(StockSpendingsRoute::class, StockSpendingsRoute.serializer())
    }
}
