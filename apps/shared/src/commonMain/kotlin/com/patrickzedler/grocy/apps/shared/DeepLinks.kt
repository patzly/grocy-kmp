package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteryTrackingRoute
import com.patrickzedler.grocy.feature.calendar.api.CalendarRoute
import com.patrickzedler.grocy.feature.chores.api.ChoreTrackingRoute
import com.patrickzedler.grocy.feature.chores.api.ChoresOverviewRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentDetailRoute
import com.patrickzedler.grocy.feature.equipment.api.EquipmentRoute
import com.patrickzedler.grocy.feature.recipes.api.MealPlanRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipeRoute
import com.patrickzedler.grocy.feature.recipes.api.RecipesRoute
import com.patrickzedler.grocy.feature.settings.api.NotificationSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingListRoute
import com.patrickzedler.grocy.feature.shoppinglist.api.ShoppingModeRoute
import com.patrickzedler.grocy.feature.start.api.StartRoute
import com.patrickzedler.grocy.feature.stock.api.ConsumeRoute
import com.patrickzedler.grocy.feature.stock.api.InventoryRoute
import com.patrickzedler.grocy.feature.stock.api.LocationContentSheetRoute
import com.patrickzedler.grocy.feature.stock.api.PurchaseRoute
import com.patrickzedler.grocy.feature.stock.api.StockEntriesRoute
import com.patrickzedler.grocy.feature.stock.api.StockJournalRoute
import com.patrickzedler.grocy.feature.stock.api.StockOverviewRoute
import com.patrickzedler.grocy.feature.stock.api.StockScanRoute
import com.patrickzedler.grocy.feature.stock.api.StockSpendingsRoute
import com.patrickzedler.grocy.feature.stock.api.TransferRoute
import com.patrickzedler.grocy.feature.tasks.api.TaskRoute
import com.patrickzedler.grocy.feature.tasks.api.TasksRoute

/**
 * The links `grocy://<page>?<parameter>=<id>` that NFC tags, QR codes and other apps open the
 * app with, e.g. `grocy://stockOverview` or `grocy://choreTracking?chore=3` on a tag at the
 * washing machine. A link only opens a page, the user still confirms whatever it books, so a
 * link from anywhere cannot change anything. Pages and parameters ignore case.
 */
internal object DeepLinks {
    const val SCHEME = "grocy"

    /** Every page a link opens, for the documentation and its test. */
    val pages: List<String> = listOf(
        "start", "stockOverview", "scan", "purchase", "consume", "transfer", "inventory",
        "stockEntries", "stockJournal", "locationContentSheet", "spendings", "shoppingList",
        "shoppingMode", "recipes", "recipe", "mealPlan", "chores", "choreTracking", "tasks",
        "task", "batteries", "batteryTracking", "equipment", "calendar", "settings",
    )

    /**
     * The screen [link] opens, null for anything that is no such link, an unknown page or a
     * parameter that is no id; parameters a page does not know are left out.
     */
    fun routeOf(link: String): Route? {
        val rest = link.trim().removeScheme() ?: return null
        val page = rest.substringBefore('?').substringBefore('#').trim('/').lowercase()
        val query = queryOf(rest.substringAfter('?', "").substringBefore('#'))
        val ids = Ids(query)
        return ids.routeOf(page)?.takeIf { ids.valid }
    }

    private fun Ids.routeOf(page: String): Route? = when (page) {
        "start" -> StartRoute
        "stockoverview" -> StockOverviewRoute
        "scan" -> StockScanRoute
        "purchase" -> PurchaseRoute(productId = id(PRODUCT))
        "consume" -> ConsumeRoute(productId = id(PRODUCT))
        "transfer" -> TransferRoute(productId = id(PRODUCT))
        "inventory" -> InventoryRoute(productId = id(PRODUCT))
        "stockentries" -> StockEntriesRoute(id(PRODUCT))
        "stockjournal" -> StockJournalRoute(id(PRODUCT))
        "locationcontentsheet" -> LocationContentSheetRoute
        "spendings" -> StockSpendingsRoute
        "shoppinglist" -> ShoppingListRoute(id(LIST))
        "shoppingmode" -> ShoppingModeRoute()
        "recipes" -> RecipesRoute()
        "recipe" -> id(ID)?.let(::RecipeRoute)
        "mealplan" -> MealPlanRoute()
        "chores" -> ChoresOverviewRoute
        // On its own, so back returns to where the link was opened
        "choretracking" -> ChoreTrackingRoute(id(CHORE), inList = false)
        "tasks" -> TasksRoute
        "task" -> TaskRoute(id(ID), inList = false)
        "batteries" -> BatteriesOverviewRoute
        "batterytracking" -> BatteryTrackingRoute(id(BATTERY), inList = false)
        "equipment" -> id(ID)?.let { EquipmentDetailRoute(it, inList = false) } ?: EquipmentRoute
        "calendar" -> CalendarRoute()
        "settings" -> SettingsRoute
        else -> null
    }

    private fun String.removeScheme(): String? {
        val colon = indexOf(':')
        if (colon < 0 || !substring(0, colon).equals(SCHEME, ignoreCase = true)) return null
        return substring(colon + 1).removePrefix("//")
    }

    private fun queryOf(query: String): Map<String, String> = query.split('&')
        .filter { it.isNotEmpty() }
        .associate { it.substringBefore('=').lowercase() to it.substringAfter('=', "") }

    /** The ids of a link's parameters; a parameter the page reads that is no id spoils it. */
    private class Ids(private val query: Map<String, String>) {
        var valid = true
            private set

        fun id(name: String): Int? {
            val value = query[name] ?: return null
            val id = value.toIntOrNull()?.takeIf { it > 0 }
            if (id == null) valid = false
            return id
        }
    }

    private const val ID = "id"
    private const val PRODUCT = "product"
    private const val CHORE = "chore"
    private const val BATTERY = "battery"
    private const val LIST = "list"
}

/** What goes on the back stack for an opened [route]: a settings page comes with its list. */
internal fun openedRoutesOf(route: Route): List<Route> =
    if (route is NotificationSettingsRoute) listOf(SettingsRoute, route) else listOf(route)
