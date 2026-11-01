package com.patrickzedler.grocy.core.data.recipes

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.recipes.MealPlanEntry
import com.patrickzedler.grocy.core.model.recipes.MealPlanRecipe
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grocy's meal plan, offline first: its entries and sections as last downloaded, and through
 * [recipes] what the server computed for the recipes it keeps per entry, day and week (costs,
 * energy, fulfillment). Every change is online only and downloads the meal plan and those
 * recipes again, never computing anything itself.
 *
 * @param connection the logged in server, null after a logout.
 */
class MealPlanRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
    private val recipes: RecipesRepository,
) {

    private val dao = database.mealPlanDao()

    /** Every entry of the meal plan, by day. */
    val entries: Flow<List<MealPlanEntry>> =
        dao.observeEntries().map { list -> list.map { it.toModel() } }

    /** The sections in Grocy's order, by sort number; the default one among them. */
    val sections: Flow<List<MealPlanSection>> =
        dao.observeSections().map { list -> list.map { it.toModel() } }

    /** The recipes Grocy keeps for the meal plan with what it computed for them. */
    val mealPlanRecipes: Flow<List<MealPlanRecipe>> =
        dao.observeRecipes().map { list -> list.mapNotNull { it.toModel() } }

    /**
     * Downloads the entries and sections and replaces the stored ones in one transaction; the
     * recipes Grocy keeps for them come with [RecipesRepository.refresh].
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline.
     */
    suspend fun refresh() {
        val connection = requireConnection()
        val (entries, sections) = coroutineScope {
            val entries = async { api.mealPlan(connection) }
            val sections = async { api.mealPlanSections(connection) }
            entries.await() to sections.await()
        }
        database.withWriteTransaction {
            dao.deleteEntries()
            dao.insertEntries(entries.map { it.toEntity() })
            dao.deleteSections()
            dao.insertSections(sections.map { it.toEntity() })
        }
    }

    /**
     * Creates the entry (id [GrocyApi.NEW_OBJECT_ID]) or saves it like Grocy's dialogs; returns
     * its id.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun saveEntry(entry: MealPlanEntry): Int {
        val connection = requireConnection()
        val id = api.saveMealPlanEntry(connection, entry)
        dao.upsertEntry(api.mealPlanEntry(connection, id).toEntity())
        afterChange()
        return id
    }

    suspend fun deleteEntry(id: Int) {
        api.deleteObject(requireConnection(), MEAL_PLAN, id)
        dao.deleteEntry(id)
        afterChange()
    }

    /** Grocy's "Mark this item as done" ([done]) and "... as undone". */
    suspend fun setDone(id: Int, done: Boolean) {
        api.setMealPlanEntryDone(requireConnection(), id, done)
        afterChange()
    }

    /**
     * Grocy's "Copy this day": every entry the server has on [fromDay] once more on [toDay],
     * not done ("YYYY-MM-DD" both).
     */
    suspend fun copyDay(fromDay: String, toDay: String) {
        val connection = requireConnection()
        try {
            api.mealPlanOfDay(connection, fromDay).forEach { entry ->
                api.saveMealPlanEntry(
                    connection,
                    entry.copy(id = GrocyApi.NEW_OBJECT_ID, day = toDay, done = false),
                )
            }
        } finally {
            // What was copied shows even when a later entry failed
            afterChange()
        }
    }

    /**
     * Grocy's "Consume all ingredients needed by this recipe" of a planned recipe: its shadow
     * recipe [shadowRecipeId] at the entry's servings, then entry [entryId] is done.
     */
    suspend fun consumeRecipe(entryId: Int, shadowRecipeId: Int) {
        val connection = requireConnection()
        api.consumeRecipe(connection, shadowRecipeId)
        api.setMealPlanEntryDone(connection, entryId, true)
        afterChange()
    }

    /**
     * Grocy's "Put missing products on shopping list" of a planned recipe or a whole week
     * ([recipeId] its shadow or week recipe), at the planned servings.
     */
    suspend fun putMissingOnShoppingList(recipeId: Int) {
        api.addMissingRecipeProductsToShoppingList(requireConnection(), recipeId, emptyList())
        afterChange()
    }

    /** Grocy's "Consume all ingredients needed by this weeks recipes or products". */
    suspend fun consumeWeek(weekRecipeId: Int) {
        api.consumeRecipe(requireConnection(), weekRecipeId)
        afterChange()
    }

    /** Creates the section (id [GrocyApi.NEW_OBJECT_ID]) or saves it; returns its id. */
    suspend fun saveSection(section: MealPlanSection): Int {
        val connection = requireConnection()
        val id = api.saveMealPlanSection(connection, section)
        dao.upsertSection(api.mealPlanSection(connection, id).toEntity())
        return id
    }

    /** Deletes the section; Grocy keeps its entries, the app shows them in the default one. */
    suspend fun deleteSection(id: Int) {
        api.deleteObject(requireConnection(), MEAL_PLAN_SECTIONS, id)
        dao.deleteSection(id)
        afterChange()
    }

    /**
     * Downloads what changed with the meal plan: its entries and the recipes Grocy computes
     * for them; a failure never fails the change itself, the next refresh gets it.
     */
    private suspend fun afterChange() {
        try {
            coroutineScope {
                async { refresh() }
                async { recipes.refresh() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: GrocyApiException) {
            // The next refresh downloads it
        }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    companion object {
        const val MEAL_PLAN = "meal_plan"
        const val MEAL_PLAN_SECTIONS = "meal_plan_sections"
    }
}
