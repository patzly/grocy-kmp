package com.patrickzedler.grocy.feature.start.impl

import com.patrickzedler.grocy.core.data.batteries.BatteriesRepository
import com.patrickzedler.grocy.feature.batteries.api.BatteriesOverviewRoute
import com.patrickzedler.grocy.feature.batteries.api.BatteryTrackingRoute
import com.patrickzedler.grocy.core.data.chores.ChoresRepository
import com.patrickzedler.grocy.core.data.equipment.EquipmentRepository
import com.patrickzedler.grocy.core.data.tasks.TasksRepository
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.recipes.MealPlanRepository
import com.patrickzedler.grocy.core.data.recipes.RecipesRepository
import com.patrickzedler.grocy.core.data.shoppinglist.ShoppingListRepository
import com.patrickzedler.grocy.core.data.stock.StockRepository
import com.patrickzedler.grocy.core.data.util.RefreshError
import com.patrickzedler.grocy.core.database.createInMemoryGrocyDatabase
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.testing.FakeGrocyServer
import io.ktor.http.HttpMethod
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class StartViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val server = FakeGrocyServer(requestDispatcher = dispatcher)
    private val database = createInMemoryGrocyDatabase(queryContext = dispatcher)
    private val connection = { ServerConnection.ApiKey("https://grocy.test", "key") }
    private val repository = MasterDataRepository(server.api, database, connection)
    private val stock = StockRepository(server.api, database, connection)
    private val config = MutableStateFlow<ServerConfig?>(ServerConfig())
    private lateinit var shoppingLists: ShoppingListRepository

    private val recipes = RecipesRepository(server.api, database, connection)

    private fun TestScope.viewModel(today: String = "2026-10-25") = StartViewModel(
        repository = repository,
        stock = stock,
        shoppingLists = ShoppingListRepository(server.api, database, backgroundScope, connection)
            .also { shoppingLists = it },
        recipes = recipes,
        mealPlan = MealPlanRepository(server.api, database, connection, recipes),
        chores = ChoresRepository(server.api, database, connection),
        tasks = TasksRepository(server.api, database, connection),
        batteries = BatteriesRepository(server.api, database, connection),
        equipment = EquipmentRepository(server.api, database, connection),
        serverConfig = config,
        today = { epochDayOf(today)!! },
        now = { "$today 12:00:00" },
    ).also { backgroundScope.launch { it.state.collect {} } }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun openingDownloadsTheServersDataAndTheStockOverview() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertTrue(repository.products.first().isNotEmpty())
        assertTrue(stock.productStockInfo.first().isNotEmpty())
        assertTrue(viewModel.refresh.value.loadedOnce)
    }

    @Test
    fun stockSectionCountsLikeGrocysStockOverview() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val summary = viewModel.state.value!!.stock!!
        assertEquals(1, summary.productCount)
        assertEquals(3.0, summary.totalValue)
        // Milk's best before date lies five days back, Yoghurt is below its min. stock amount
        assertEquals(1, summary.overdueCount)
        assertEquals(1, summary.belowMinStockCount)
        assertEquals(5, viewModel.state.value!!.dueSoonDays)
    }

    @Test
    fun masterDataSectionCountsTheAvailableCategoriesWithObjects() = runTest(dispatcher) {
        config.value = ServerConfig(stockPriceTracking = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        val counts = viewModel.state.value!!.masterDataCounts.toMap()
        assertEquals(4, counts[MasterDataCount.PRODUCTS])
        // Stores belong to price tracking
        assertNull(counts[MasterDataCount.STORES])
        // In the order of Grocy's menu
        assertEquals(MasterDataCount.PRODUCTS, counts.keys.first())
    }

    @Test
    fun stockSectionTellsNothingWithoutTheStockFeature() = runTest(dispatcher) {
        config.value = ServerConfig(stock = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.stock)
        assertTrue("/api/stock/volatile" !in server.requestedPaths)
    }

    @Test
    fun offlineRefreshReportsIt() = runTest(dispatcher) {
        server.unreachable = true
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(RefreshError.OFFLINE, viewModel.refresh.value.error)
    }

    @Test
    fun shoppingListSectionCountsTheItemsToBuy() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        // Milk is still to buy, the batteries are done
        val lists = viewModel.state.value!!.shoppingLists!!
        assertEquals(1, lists.undoneCount)
        assertEquals(listOf("Hardware store", "Shopping list"), lists.lists.map { it.name })
    }

    @Test
    fun quickAddUsesTheShownListOtherwiseGrocysDefault() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val default = viewModel.quickAddListId()
        shoppingLists.selectList(2)
        val shown = viewModel.quickAddListId()
        shoppingLists.selectList(99)
        val removed = viewModel.quickAddListId()

        assertEquals(1, default)
        assertEquals(2, shown)
        assertEquals(1, removed)
    }

    @Test
    fun shoppingListSectionNeedsTheShoppingListFeature() = runTest(dispatcher) {
        config.value = ServerConfig(shoppingList = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.shoppingLists)
        assertNull(viewModel.quickAddListId())
        assertTrue("/api/objects/shopping_list" !in server.requestedPaths)
    }

    @Test
    fun recipesSectionCountsTheRecipesByTheirStock() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        // Pancakes miss Bread, which is on the shopping list; Soup has enough
        assertEquals(RecipesSummary(2, 1, 1, 0), viewModel.state.value!!.recipes)
    }

    @Test
    fun recipesSectionNeedsTheRecipesFeature() = runTest(dispatcher) {
        config.value = ServerConfig(recipes = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.recipes)
        assertTrue("/api/objects/recipes" !in server.requestedPaths)
    }

    @Test
    fun mealPlanSectionTellsWhatIsPlannedForToday() = runTest(dispatcher) {
        val viewModel = viewModel(today = "2026-11-02")

        advanceUntilIdle()

        // Grocy's default section first, then breakfast
        assertEquals(listOf("Milk", "Pancakes"), viewModel.state.value!!.mealPlanToday)
    }

    @Test
    fun mealPlanSectionNeedsTheMealPlanFeature() = runTest(dispatcher) {
        config.value = ServerConfig(recipesMealPlan = false)
        val viewModel = viewModel(today = "2026-11-02")

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.mealPlanToday)
        assertTrue("/api/objects/meal_plan" !in server.requestedPaths)
    }

    @Test
    fun choresSectionCountsLikeGrocysMessages() = runTest {
        val viewModel = viewModel(today = "2026-11-02")

        advanceUntilIdle()

        val chores = viewModel.state.value!!.chores!!
        assertEquals(2, chores.count)
        assertEquals(1, chores.overdue)
        assertEquals(1, chores.assignedToMe)
    }

    @Test
    fun tasksSectionCountsLikeGrocysMessages() = runTest {
        val viewModel = viewModel(today = "2026-11-02")

        advanceUntilIdle()

        val tasks = viewModel.state.value!!.tasks!!
        // Not the done one, however late it was
        assertEquals(2, tasks.count)
        assertEquals(1, tasks.overdue)
        assertEquals(0, tasks.dueToday)
        assertEquals(5, viewModel.state.value!!.tasksDueSoonDays)
    }

    @Test
    fun tasksSectionNeedsItsFeature() = runTest {
        config.value = ServerConfig(tasks = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.tasks)
        assertEquals(0, server.requestsTo("/api/objects/tasks", HttpMethod.Get).size)
    }

    @Test
    fun choresSectionNeedsItsFeature() = runTest {
        config.value = ServerConfig(chores = false)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertNull(viewModel.state.value!!.chores)
        assertEquals(0, server.requestsTo("/api/chores", HttpMethod.Get).size)
    }

    @Test
    fun batteriesAndEquipmentAreCountedLikeGrocysMessages() = runTest(dispatcher) {
        val viewModel = viewModel(today = "2026-11-04")
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertEquals(2, state.batteries?.count)
        assertEquals(1, state.batteries?.overdue)
        assertEquals(2, state.equipmentCount)
    }

    @Test
    fun aFeatureTurnedOffHidesItsSectionAndItsSummary() = runTest(dispatcher) {
        config.value = ServerConfig(batteries = false, equipment = false)
        val viewModel = viewModel(today = "2026-11-04")
        advanceUntilIdle()

        val state = viewModel.state.value!!
        assertNull(state.batteries)
        assertNull(state.equipmentCount)
        val sections = availableSections(config.value)
        assertFalse(StartSection.BATTERIES in sections)
        assertFalse(StartSection.EQUIPMENT in sections)
    }

    @Test
    fun batteryTrackingOpensBesideTheOverviewOnlyWhereItFits() {
        assertEquals(
            listOf(BatteriesOverviewRoute, BatteryTrackingRoute()),
            batteryTrackingRoutes(isBesideDetail = true),
        )
        assertEquals(
            listOf(BatteryTrackingRoute(inList = false)),
            batteryTrackingRoutes(isBesideDetail = false),
        )
    }
}
