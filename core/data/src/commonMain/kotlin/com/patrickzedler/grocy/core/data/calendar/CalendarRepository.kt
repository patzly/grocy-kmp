package com.patrickzedler.grocy.core.data.calendar

import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.calendar.CalendarData
import com.patrickzedler.grocy.core.model.calendar.CalendarEvent
import com.patrickzedler.grocy.core.model.calendar.calendarEventsOf
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Grocy's calendar: its events from what the other repositories stored, so it also works offline,
 * and its public sharing link, which is only ever asked for and never stored.
 */
class CalendarRepository(
    private val api: GrocyApi,
    private val connection: suspend () -> ServerConnection?,
    private val masterData: MasterDataRepository,
    private val stock: StockRepository,
    private val chores: ChoresRepository,
    private val tasks: TasksRepository,
    private val batteries: BatteriesRepository,
    private val recipes: RecipesRepository,
    private val mealPlan: MealPlanRepository,
) {

    /** Every event of the features the server has, by day. */
    val events: Flow<List<CalendarEvent>> = combine(
        combine(masterData.serverConfig, masterData.products, masterData.stock, ::Triple),
        combine(tasks.tasks, chores.current, masterData.users, ::Triple),
        combine(batteries.current, masterData.batteries, ::Pair),
        combine(mealPlan.entries, mealPlan.sections, recipes.recipes, ::Triple),
    ) { (config, products, amounts), (taskList, choreList, users), (current, batteryList), meals ->
        val (entries, sections, recipeList) = meals
        calendarEventsOf(
            CalendarData(
                config = config,
                products = products,
                stock = amounts,
                tasks = taskList,
                chores = choreList,
                users = users,
                currentBatteries = current,
                batteries = batteryList,
                mealPlan = entries,
                sections = sections,
                recipes = recipeList,
            ),
        )
    }

    /** Downloads what the events of the server's features are made of. */
    suspend fun refresh() {
        val config = masterData.serverConfig.first()
        coroutineScope {
            launch { masterData.refresh() }
            if (config.stock) launch { stock.refreshOverview(config.shoppingList) }
            if (config.tasks) launch { tasks.refresh() }
            if (config.chores) launch { chores.refresh() }
            if (config.batteries) launch { batteries.refresh() }
            if (config.recipes) launch { recipes.refresh() }
            if (config.recipes && config.recipesMealPlan) launch { mealPlan.refresh() }
        }
    }

    /**
     * Grocy's public link of the calendar in iCal format. Online only.
     *
     * @throws GrocyApiException if the server cannot be reached or refuses.
     */
    suspend fun sharingLink(): String {
        val server = connection() ?: throw GrocyApiException.Unauthorized()
        return api.calendarSharingLink(server)
    }
}
