package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Grocy's chores overview, journal and overview cards as last downloaded, also for offline. */
@Dao
interface ChoreDao {

    @Query("SELECT * FROM chores_current ORDER BY name COLLATE NOCASE")
    fun observeCurrent(): Flow<List<CurrentChoreEntity>>

    @Query("DELETE FROM chores_current")
    suspend fun deleteCurrent()

    @Insert
    suspend fun insertCurrent(chores: List<CurrentChoreEntity>)

    /** Executions tracked at [since] or later, newest first; everything for an empty [since]. */
    @Query(
        "SELECT * FROM chores_log WHERE tracked_time >= :since ORDER BY tracked_time DESC, id DESC",
    )
    fun observeExecutions(since: String): Flow<List<ChoreExecutionEntity>>

    /** The executions of [since] and later, which a download of that range replaces. */
    @Query("DELETE FROM chores_log WHERE tracked_time >= :since")
    suspend fun deleteExecutions(since: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutions(executions: List<ChoreExecutionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExecution(execution: ChoreExecutionEntity)

    @Query("SELECT * FROM chore_details WHERE chore_id = :choreId")
    fun observeDetails(choreId: Int): Flow<ChoreDetailsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetails(details: ChoreDetailsEntity)
}
