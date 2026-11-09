package com.patrickzedler.grocy.core.data.widget

import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.flow.first

/**
 * Keeps what the home screen widgets show current in the background. One small request tells
 * whether the server changed anything, only then the data of the widgets that are placed is
 * downloaded, so checking often costs next to nothing.
 */
class WidgetDataRefresher(
    private val api: GrocyApi,
    private val connection: suspend () -> ServerConnection?,
    private val masterData: MasterDataRepository,
    private val calendar: CalendarRepository,
    private val shoppingLists: ShoppingListRepository,
) {

    /**
     * Downloads for [calendarWidgets] when the server's database changed or [today] is another day
     * than then (what the server computes depends on the day too), for [shoppingListWidgets] when
     * it changed. Without a login nothing happens. Returns whether it downloaded.
     *
     * @throws GrocyApiException if the server cannot be reached or refuses.
     */
    suspend fun refreshIfChanged(
        today: Long,
        calendarWidgets: Boolean,
        shoppingListWidgets: Boolean,
    ): Boolean {
        val server = connection() ?: return false
        val changed = api.dbChangedTime(server)
        val calendarState = "$changed $today"
        val refreshesCalendar =
            calendarWidgets && masterData.storedServerValue(CALENDAR_STATE) != calendarState
        val refreshesShoppingLists = shoppingListWidgets &&
            masterData.serverConfig.first().shoppingList &&
            masterData.storedServerValue(SHOPPING_LIST_STATE) != changed
        if (refreshesCalendar) {
            calendar.refresh()
            masterData.storeServerValue(CALENDAR_STATE, calendarState)
        }
        if (refreshesShoppingLists) {
            shoppingLists.refresh()
            // Names a new item may need, the calendar's download brought them already
            if (!refreshesCalendar) masterData.refreshShoppingListNames()
            masterData.storeServerValue(SHOPPING_LIST_STATE, changed)
        }
        return refreshesCalendar || refreshesShoppingLists
    }
}

// The server's change time (and the day) of the last complete download
private const val CALENDAR_STATE = "calendar_downloaded_state"
private const val SHOPPING_LIST_STATE = "shopping_list_downloaded_state"
