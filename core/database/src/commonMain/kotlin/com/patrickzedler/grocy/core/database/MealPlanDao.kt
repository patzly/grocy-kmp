package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Grocy's meal plan, its sections and the recipes Grocy keeps for it, stored as last
 * downloaded, so the meal plan opens offline too.
 */
@Dao
interface MealPlanDao {

    @Query("SELECT * FROM meal_plan ORDER BY day, id")
    fun observeEntries(): Flow<List<MealPlanEntryEntity>>

    @Query("DELETE FROM meal_plan")
    suspend fun deleteEntries()

    @Insert
    suspend fun insertEntries(entries: List<MealPlanEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: MealPlanEntryEntity)

    @Query("DELETE FROM meal_plan WHERE id = :id")
    suspend fun deleteEntry(id: Int)

    @Query("SELECT * FROM meal_plan_sections ORDER BY sort_number IS NULL, sort_number, id")
    fun observeSections(): Flow<List<MealPlanSectionEntity>>

    @Query("DELETE FROM meal_plan_sections")
    suspend fun deleteSections()

    @Insert
    suspend fun insertSections(sections: List<MealPlanSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(section: MealPlanSectionEntity)

    @Query("DELETE FROM meal_plan_sections WHERE id = :id")
    suspend fun deleteSection(id: Int)

    @Query("SELECT * FROM meal_plan_recipes")
    fun observeRecipes(): Flow<List<MealPlanRecipeEntity>>

    @Query("DELETE FROM meal_plan_recipes")
    suspend fun deleteRecipes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<MealPlanRecipeEntity>)
}
