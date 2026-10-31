package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Grocy's recipes, their ingredients and included recipes, and what the server computed for
 * them, stored as last downloaded, so the recipes open offline too.
 */
@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: Int): Flow<RecipeEntity?>

    @Query("DELETE FROM recipes")
    suspend fun deleteRecipes()

    @Insert
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Int)

    @Query("SELECT * FROM recipe_fulfillment")
    fun observeFulfillment(): Flow<List<RecipeFulfillmentEntity>>

    @Query("DELETE FROM recipe_fulfillment")
    suspend fun deleteFulfillment()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFulfillment(fulfillment: List<RecipeFulfillmentEntity>)

    @Query("SELECT * FROM recipe_ingredients ORDER BY id")
    fun observeIngredients(): Flow<List<RecipeIngredientEntity>>

    @Query("DELETE FROM recipe_ingredients")
    suspend fun deleteIngredients()

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngredient(ingredient: RecipeIngredientEntity)

    @Query("DELETE FROM recipe_ingredients WHERE id = :id")
    suspend fun deleteIngredient(id: Int)

    @Query("SELECT * FROM recipe_nestings ORDER BY id")
    fun observeNestings(): Flow<List<RecipeNestingEntity>>

    @Query("DELETE FROM recipe_nestings")
    suspend fun deleteNestings()

    @Insert
    suspend fun insertNestings(nestings: List<RecipeNestingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNesting(nesting: RecipeNestingEntity)

    @Query("DELETE FROM recipe_nestings WHERE id = :id")
    suspend fun deleteNesting(id: Int)

    @Query(
        "SELECT * FROM recipe_ingredients_resolved WHERE recipe_id = :recipeId " +
            "ORDER BY ingredient_group, product_group, ingredient_id",
    )
    fun observeResolvedIngredients(recipeId: Int): Flow<List<ResolvedRecipeIngredientEntity>>

    @Query("DELETE FROM recipe_ingredients_resolved")
    suspend fun deleteResolvedIngredients()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResolvedIngredients(ingredients: List<ResolvedRecipeIngredientEntity>)

    @Query("SELECT bytes FROM recipe_pictures WHERE file_name = :fileName")
    suspend fun picture(fileName: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPicture(picture: RecipePictureEntity)

    /** Pictures of recipes not stored yet, e.g. to download them for offline use. */
    @Query(
        "SELECT DISTINCT picture_file_name FROM recipes WHERE picture_file_name IS NOT NULL " +
            "AND picture_file_name NOT IN (SELECT file_name FROM recipe_pictures)",
    )
    suspend fun missingPictures(): List<String>

    /** Pictures no recipe shows anymore. */
    @Query(
        "DELETE FROM recipe_pictures WHERE file_name NOT IN " +
            "(SELECT picture_file_name FROM recipes WHERE picture_file_name IS NOT NULL)",
    )
    suspend fun deleteUnusedPictures()
}
