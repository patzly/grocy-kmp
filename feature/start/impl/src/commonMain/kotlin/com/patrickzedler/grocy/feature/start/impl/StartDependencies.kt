package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.core.data.calendar.CalendarRepository
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.server.ServerState
import com.patrickzedler.grocy.core.data.settings.StartLayoutRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.flow.StateFlow

/** What the start screen needs from the app. */
interface StartDependencies {
    val authRepository: AuthRepository
    val masterDataRepository: MasterDataRepository
    val recipesRepository: RecipesRepository
    val mealPlanRepository: MealPlanRepository
    val choresRepository: ChoresRepository
    val tasksRepository: TasksRepository
    val batteriesRepository: BatteriesRepository
    val equipmentRepository: EquipmentRepository
    val calendarRepository: CalendarRepository
    val stockRepository: StockRepository
    val shoppingListRepository: ShoppingListRepository

    /** How the user arranged the screen, read at app start. */
    val startLayoutRepository: StartLayoutRepository

    /** The stored server configuration, read at app start (null for the moment until then). */
    val serverConfig: StateFlow<ServerConfig?>

    /** The logged in server, read at app start, for the demo's warning. */
    val server: StateFlow<ServerState?>
}
