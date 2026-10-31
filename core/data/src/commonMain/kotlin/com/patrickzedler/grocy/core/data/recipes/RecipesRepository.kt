package com.patrickzedler.grocy.core.data.recipes

import androidx.room3.withWriteTransaction
import com.patrickzedler.grocy.core.data.masterdata.PictureChange
import com.patrickzedler.grocy.core.data.masterdata.cleanFileName
import com.patrickzedler.grocy.core.database.GrocyDatabase
import com.patrickzedler.grocy.core.database.RecipePictureEntity
import com.patrickzedler.grocy.core.database.toEntity
import com.patrickzedler.grocy.core.database.toModel
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.model.recipes.MealPlanSection
import com.patrickzedler.grocy.core.model.recipes.Recipe
import com.patrickzedler.grocy.core.model.recipes.RecipeFulfillment
import com.patrickzedler.grocy.core.model.recipes.RecipeIngredient
import com.patrickzedler.grocy.core.model.recipes.RecipeNesting
import com.patrickzedler.grocy.core.model.recipes.ResolvedRecipeIngredient
import com.patrickzedler.grocy.core.network.GrocyApi
import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Grocy's recipes, offline first: the recipes, their ingredients and included recipes, and what
 * the server computes for them at their desired servings (fulfillment, costs, the resolved
 * ingredients), stored as last downloaded. What the server computes is never derived here; every
 * change is online only and downloads it again.
 *
 * @param connection the logged in server, null after a logout.
 */
class RecipesRepository(
    private val api: GrocyApi,
    private val database: GrocyDatabase,
    private val connection: suspend () -> ServerConnection?,
    private val random: Random = Random.Default,
) {

    private val dao = database.recipeDao()

    /** Grocy's normal recipes, by name. */
    val recipes: Flow<List<Recipe>> = dao.observeRecipes().map { list -> list.map { it.toModel() } }

    /** What the server computed for each recipe. */
    val fulfillment: Flow<List<RecipeFulfillment>> =
        dao.observeFulfillment().map { list -> list.map { it.toModel() } }

    /** Every recipe's ingredients as defined, for the recipe forms. */
    val ingredients: Flow<List<RecipeIngredient>> =
        dao.observeIngredients().map { list -> list.map { it.toModel() } }

    /** Every recipe's included recipes, for the recipe forms. */
    val nestings: Flow<List<RecipeNesting>> =
        dao.observeNestings().map { list -> list.map { it.toModel() } }

    fun recipe(id: Int): Flow<Recipe?> = dao.observeRecipe(id).map { it?.toModel() }

    /**
     * The ingredients of recipe [recipeId] as the server resolved them, its included recipes'
     * ones too, ordered by ingredient group and product group like Grocy's recipe page.
     */
    fun resolvedIngredients(recipeId: Int): Flow<List<ResolvedRecipeIngredient>> =
        dao.observeResolvedIngredients(recipeId).map { list -> list.map { it.toModel() } }

    /**
     * Downloads the recipes and everything the server computes for them, and replaces the
     * stored ones in one transaction.
     *
     * @throws GrocyApiException e.g. [GrocyApiException.Unreachable] when offline; the stored
     *   recipes stay as they were.
     */
    suspend fun refresh() {
        val connection = requireConnection()
        val download = coroutineScope {
            val recipes = async { api.recipeObjects(connection) }
            val fulfillment = async { api.recipesFulfillment(connection) }
            val ingredients = async { api.recipeIngredients(connection) }
            val nestings = async { api.recipeNestings(connection) }
            val resolved = async { api.resolvedRecipeIngredients(connection) }
            Download(
                recipes = recipes.await(),
                fulfillment = fulfillment.await(),
                ingredients = ingredients.await(),
                nestings = nestings.await(),
                resolved = resolved.await(),
            )
        }
        // Meal plan entries are recipes too, only the normal ones are kept
        val ids = download.recipes.map { it.id }.toSet()
        database.withWriteTransaction {
            dao.deleteRecipes()
            dao.insertRecipes(download.recipes.map { it.toEntity() })
            dao.deleteFulfillment()
            dao.insertFulfillment(
                download.fulfillment.filter { it.recipeId in ids }.map { it.toEntity() },
            )
            dao.deleteIngredients()
            dao.insertIngredients(
                download.ingredients.filter { it.recipeId in ids }.map { it.toEntity() },
            )
            dao.deleteNestings()
            dao.insertNestings(
                download.nestings.filter { it.recipeId in ids }.map { it.toEntity() },
            )
            dao.deleteResolvedIngredients()
            dao.insertResolvedIngredients(
                download.resolved.filter { it.recipeId in ids }.map { it.toEntity() },
            )
            dao.deleteUnusedPictures()
        }
    }

    /**
     * Creates the recipe (id [GrocyApi.NEW_OBJECT_ID]) or saves it like Grocy's recipe form with
     * its changed [userfields] and [picture]; returns its id. Online only, like every following
     * change, which also downloads again what the server computes from it.
     *
     * @throws GrocyApiException if the server cannot be reached or rejects it.
     */
    suspend fun saveRecipe(
        recipe: Recipe,
        userfields: Map<String, String?> = emptyMap(),
        picture: PictureChange = PictureChange.Keep,
    ): Int {
        val connection = requireConnection()
        val oldPicture = recipe.pictureFileName
        val newPicture = (picture as? PictureChange.Replace)?.let { pictureFileName(it.fileName) }
        val saved = when (picture) {
            PictureChange.Keep -> recipe
            PictureChange.Delete -> recipe.copy(pictureFileName = null)
            is PictureChange.Replace -> recipe.copy(pictureFileName = newPicture)
        }
        val id = api.saveRecipe(connection, saved)
        if (userfields.isNotEmpty()) {
            api.setUserfields(connection, RECIPES, id, userfields)
        }
        if (picture is PictureChange.Replace && newPicture != null) {
            api.uploadRecipePicture(connection, newPicture, picture.bytes)
            dao.upsertPicture(RecipePictureEntity(newPicture, picture.bytes))
        }
        // Old pictures are removed only once the recipe no longer refers to them; a copy keeps
        // its original's file name, so it may still show it
        if (picture != PictureChange.Keep && oldPicture != null) {
            val stillUsed = api.recipeObjects(connection)
                .any { it.id != id && it.pictureFileName == oldPicture }
            if (!stillUsed) ignoringServerErrors { api.deleteRecipePicture(connection, oldPicture) }
        }
        dao.upsertRecipe(api.recipe(connection, id).toEntity())
        afterChange()
        return id
    }

    /** Like the servings field of Grocy's recipe page: the ingredients are scaled anew. */
    suspend fun setDesiredServings(recipeId: Int, servings: Double) {
        api.saveDesiredServings(requireConnection(), recipeId, servings)
        afterChange()
    }

    /** Deletes the recipe; Grocy removes its ingredients and inclusions with it. */
    suspend fun deleteRecipe(recipeId: Int) {
        api.deleteObject(requireConnection(), RECIPES, recipeId)
        dao.deleteRecipe(recipeId)
        afterChange()
    }

    /** Grocy's "Copy" (named "Copy of ...") with ingredients and inclusions; returns its id. */
    suspend fun copyRecipe(recipeId: Int): Int {
        val id = api.copyRecipe(requireConnection(), recipeId)
        afterChange()
        return id
    }

    /** Grocy's "Consume all ingredients needed by this recipe". */
    suspend fun consumeRecipe(recipeId: Int) {
        api.consumeRecipe(requireConnection(), recipeId)
        afterChange()
    }

    /** Grocy's "Put missing products on shopping list", but the products [excludedProductIds]. */
    suspend fun addMissingProductsToShoppingList(recipeId: Int, excludedProductIds: List<Int>) {
        val connection = requireConnection()
        api.addMissingRecipeProductsToShoppingList(connection, recipeId, excludedProductIds)
        afterChange()
    }

    /** Creates the ingredient (id [GrocyApi.NEW_OBJECT_ID]) or saves it; returns its id. */
    suspend fun saveIngredient(ingredient: RecipeIngredient): Int {
        val connection = requireConnection()
        val id = api.saveRecipeIngredient(connection, ingredient)
        dao.upsertIngredient(api.recipeIngredient(connection, id).toEntity())
        afterChange()
        return id
    }

    suspend fun deleteIngredient(id: Int) {
        api.deleteObject(requireConnection(), INGREDIENTS, id)
        dao.deleteIngredient(id)
        afterChange()
    }

    /** Creates the included recipe (id [GrocyApi.NEW_OBJECT_ID]) or saves it; returns its id. */
    suspend fun saveNesting(nesting: RecipeNesting): Int {
        val connection = requireConnection()
        val id = api.saveRecipeNesting(connection, nesting)
        dao.upsertNesting(api.recipeNesting(connection, id).toEntity())
        afterChange()
        return id
    }

    suspend fun deleteNesting(id: Int) {
        api.deleteObject(requireConnection(), NESTINGS, id)
        dao.deleteNesting(id)
        afterChange()
    }

    /** Prints the recipe's grocycode on the server's label printer. */
    suspend fun printLabel(recipeId: Int) {
        api.printRecipeLabel(requireConnection(), recipeId)
    }

    /** The sections of Grocy's meal plan, in their order; online only. */
    suspend fun mealPlanSections(): List<MealPlanSection> =
        api.mealPlanSections(requireConnection()).sortedWith(
            compareBy<MealPlanSection> { it.sortNumber ?: Int.MAX_VALUE }.thenBy { it.id },
        )

    /** Grocy's "Add to meal plan": [servings] of the recipe on [day] ("YYYY-MM-DD"). */
    suspend fun addToMealPlan(recipeId: Int, day: String, servings: Double, sectionId: Int) {
        api.addRecipeToMealPlan(requireConnection(), recipeId, day, servings, sectionId)
    }

    /**
     * The recipe picture [fileName], stored for offline use, downloaded at [PICTURE_WIDTH]
     * pixels the first time.
     */
    suspend fun picture(fileName: String): ByteArray {
        dao.picture(fileName)?.let { return it }
        val bytes = api.recipePicture(requireConnection(), fileName, PICTURE_WIDTH)
        dao.upsertPicture(RecipePictureEntity(fileName, bytes))
        return bytes
    }

    /** Downloads the pictures of recipes not stored yet, so they show offline later. */
    suspend fun downloadMissingPictures() {
        dao.missingPictures().forEach { picture(it) }
    }

    /**
     * Downloads what the server computes from recipes, ingredients, stock and shopping list
     * after a change; a failure never fails the change itself, the next refresh gets it.
     */
    private suspend fun afterChange() {
        ignoringServerErrors { refresh() }
    }

    private suspend fun requireConnection(): ServerConnection =
        connection() ?: throw GrocyApiException.Unauthorized()

    private fun pictureFileName(originalName: String): String {
        val prefix = (1..2).joinToString("") { random.nextLong(0, Long.MAX_VALUE).toString(36) }
        return prefix + cleanFileName(originalName)
    }

    private class Download(
        val recipes: List<Recipe>,
        val fulfillment: List<RecipeFulfillment>,
        val ingredients: List<RecipeIngredient>,
        val nestings: List<RecipeNesting>,
        val resolved: List<ResolvedRecipeIngredient>,
    )

    companion object {
        /** Grocy's entity of recipes, e.g. for their userfields. */
        const val RECIPES = "recipes"
        const val INGREDIENTS = "recipes_pos"
        const val NESTINGS = "recipes_nestings"

        /** Pixels of a stored picture: wide enough for the recipe page of a tablet. */
        const val PICTURE_WIDTH = 1200
    }
}

private suspend fun ignoringServerErrors(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: GrocyApiException) {
        // The next refresh or change tries again
    }
}
