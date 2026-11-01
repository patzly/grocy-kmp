package com.patrickzedler.grocy.feature.recipes.impl.mealplan

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntryType
import com.patrickzedler.grocy.core.model.text.epochDayOf
import com.patrickzedler.grocy.core.model.text.isoDateOf
import com.patrickzedler.grocy.core.testing.MasterDataJson
import com.patrickzedler.grocy.core.testing.RepositoryTestEnvironment
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class MealPlanViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val environment = RepositoryTestEnvironment(dispatcher)

    // Wednesday of the fake server's planned week
    private val wednesday = epochDayOf("2026-11-04")!!

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        environment.close()
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(initialDay: String? = null) = MealPlanViewModel(
        mealPlan = environment.mealPlanRepository,
        recipes = environment.recipesRepository,
        masterData = environment.repository,
        stock = environment.stockRepository,
        initialDay = initialDay,
        today = { wednesday },
        // Weeks start on Monday
        localeFirstDay = { 1 },
        decimalSeparator = '.',
        computeDispatcher = dispatcher,
    ).also { backgroundScope.launch { it.state.collect {} } }

    private fun MealPlanViewModel.week() = state.value!!.week

    private fun MealPlanViewModel.item(entryId: Int) =
        week().days.flatMap { it.items }.single { it.entry.id == entryId }

    @Test
    fun openingShowsTheWeekOfTodayWithEverythingDownloaded() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val week = viewModel.week()
        assertEquals("2026-11-02", isoDateOf(week.start))
        assertTrue(viewModel.state.value!!.isCurrentWeek)
        assertEquals(listOf(2, 1), week.days[0].items.map { it.entry.id })
        assertEquals("Eat out", week.days[1].items.single().name)
        assertEquals(7.5, week.weekRecipe?.fulfillment?.costs)
        assertEquals(7.5, week.days[0].summary?.costs)
        assertTrue(viewModel.refresh.value.loadedOnce)
    }

    @Test
    fun theWeeksStartWhereGrocysSettingSays() = runTest(dispatcher) {
        environment.server.respond(
            "/api/system/config",
            body = """{"MEAL_PLAN_FIRST_DAY_OF_WEEK":"0"}""",
        )
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals("2026-11-01", isoDateOf(viewModel.week().start))
    }

    @Test
    fun weeksChangeBackAndForthAndTodayLeadsBack() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.showNextWeek()
        advanceUntilIdle()
        assertEquals("2026-11-09", isoDateOf(viewModel.week().start))
        assertFalse(viewModel.state.value!!.isCurrentWeek)
        assertTrue(viewModel.week().isEmpty)

        viewModel.showPreviousWeek()
        viewModel.showPreviousWeek()
        advanceUntilIdle()
        assertEquals("2026-10-26", isoDateOf(viewModel.week().start))

        viewModel.showToday()
        advanceUntilIdle()
        assertEquals("2026-11-02", isoDateOf(viewModel.week().start))
    }

    @Test
    fun theRouteChoosesTheWeek() = runTest(dispatcher) {
        val viewModel = viewModel(initialDay = "2026-11-12")

        advanceUntilIdle()

        assertEquals("2026-11-09", isoDateOf(viewModel.week().start))
    }

    @Test
    fun newEntriesGoToTodayInTheCurrentWeekElseToTheWeeksFirstDay() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("2026-11-04", addDayOf(viewModel.state.value!!))
        viewModel.showNextWeek()
        advanceUntilIdle()
        assertEquals("2026-11-09", addDayOf(viewModel.state.value!!))
    }

    @Test
    fun anAddedNoteIsPostedAndTheDialogCloses() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.add(MealPlanEntryType.NOTE, "2026-11-05")
        advanceUntilIdle()
        val state = assertIs<MealPlanDialog.Entry>(viewModel.dialog).state
        state.note.setTextAndPlaceCursorAtEnd("Leftovers")
        viewModel.saveEntry()
        advanceUntilIdle()

        val post = environment.server.requestsTo("/api/objects/meal_plan", HttpMethod.Post).single()
        val body = (post.body as TextContent).text
        assertTrue(""""day":"2026-11-05"""" in body)
        assertTrue(""""note":"Leftovers"""" in body)
        assertNull(viewModel.dialog)
    }

    @Test
    fun anInvalidEntryKeepsItsDialogOpen() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.add(MealPlanEntryType.RECIPE, "2026-11-05")
        advanceUntilIdle()
        viewModel.saveEntry()
        advanceUntilIdle()

        assertIs<MealPlanDialog.Entry>(viewModel.dialog)
        assertTrue(environment.server.requestsTo("/api/objects/meal_plan", HttpMethod.Post).isEmpty())
    }

    @Test
    fun deletingTellsItAndCanBringTheEntryBack() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val note = viewModel.item(3).entry

        viewModel.events.test {
            viewModel.delete(note)
            advanceUntilIdle()

            assertEquals(MealPlanEvent.Deleted(note), awaitItem())
        }
        viewModel.restore(note)
        advanceUntilIdle()

        assertEquals(1, environment.server.requestsTo("/api/objects/meal_plan/3", HttpMethod.Delete).size)
        val post = environment.server.requestsTo("/api/objects/meal_plan", HttpMethod.Post).single()
        val body = (post.body as TextContent).text
        assertTrue(""""note":"Eat out"""" in body)
        // Done like it was
        assertTrue(""""done":1""" in body)
    }

    @Test
    fun aConsumedProductIsDoneAndCanBeUndone() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.consumeProduct(viewModel.item(2))
            advanceUntilIdle()

            val event = assertIs<MealPlanEvent.ProductConsumed>(awaitItem())
            assertEquals(2.0, event.amount)
            assertEquals("Milk", event.name)
            assertEquals(MasterDataJson.TRANSACTION_ID, event.transactionId)
        }
        val consume = environment.server.requestsTo("/api/stock/products/1/consume", HttpMethod.Post)
        assertEquals(1, consume.size)
        val done = environment.server.requestsTo("/api/objects/meal_plan/2", HttpMethod.Put).single()
        assertEquals("""{"done":1}""", (done.body as TextContent).text)
    }

    @Test
    fun aPlannedRecipeIsConsumedAfterGrocysQuestion() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val pancakes = viewModel.item(1)

        viewModel.requestConsume(pancakes)
        assertEquals(MealPlanDialog.ConsumeRecipe(pancakes), viewModel.dialog)
        viewModel.consumeRecipe(pancakes)
        advanceUntilIdle()

        assertNull(viewModel.dialog)
        assertEquals(
            1,
            environment.server.requestsTo("/api/recipes/-3/consume", HttpMethod.Post).size,
        )
    }

    @Test
    fun doneIsPutOnTheServer() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setDone(viewModel.item(3).entry, done = false)
        advanceUntilIdle()

        val put = environment.server.requestsTo("/api/objects/meal_plan/3", HttpMethod.Put).single()
        assertEquals("""{"done":0}""", (put.body as TextContent).text)
    }

    @Test
    fun aPlannedRecipeOpensAtItsServingsLikeGrocyShowsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.openRecipe(viewModel.item(1))
            advanceUntilIdle()

            assertEquals(MealPlanEvent.OpenRecipe(1), awaitItem())
        }
        val put = environment.server.requestsTo("/api/objects/recipes/1", HttpMethod.Put).single()
        assertEquals("""{"desired_servings":2.0}""", (put.body as TextContent).text)
    }

    @Test
    fun aDayIsCopiedToTheChosenDay() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestCopyDay("2026-11-03")
        val state = assertIs<MealPlanDialog.CopyDay>(viewModel.dialog).state
        state.toDay.setTextAndPlaceCursorAtEnd("2026-11-10")
        viewModel.events.test {
            viewModel.copyDay()
            advanceUntilIdle()

            assertEquals(MealPlanEvent.DayCopied, awaitItem())
        }

        assertNull(viewModel.dialog)
        val post = environment.server.requestsTo("/api/objects/meal_plan", HttpMethod.Post).single()
        assertTrue(""""day":"2026-11-10"""" in (post.body as TextContent).text)
    }

    @Test
    fun theWeeksMissingProductsGoOnTheShoppingList() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.putOnShoppingList(viewModel.week().weekRecipe!!.id)
            advanceUntilIdle()

            assertEquals(MealPlanEvent.PutOnShoppingList, awaitItem())
        }
        val path = "/api/recipes/-1/add-not-fulfilled-products-to-shoppinglist"
        assertEquals(1, environment.server.requestsTo(path, HttpMethod.Post).size)
    }

    @Test
    fun aFailedChangeIsReported() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        environment.server.unreachable = true

        viewModel.events.test {
            viewModel.setDone(viewModel.item(1).entry, done = true)
            advanceUntilIdle()

            assertEquals(MealPlanAction.DONE, assertIs<MealPlanEvent.Failed>(awaitItem()).action)
        }
        assertFalse(viewModel.isRunning)
    }

    @Test
    fun theTodaysDayLiesAfterTheWeekAndTheEarlierDays() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        // The week, Monday with two entries, Tuesday with one
        assertEquals(1 + 3 + 2, agendaIndexOfDay(viewModel.state.value!!, dayIndex = 2))
    }
}
