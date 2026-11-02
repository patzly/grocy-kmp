package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Grocy's tasks as last downloaded, also for offline. */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY name COLLATE NOCASE")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun deleteTasks()

    @Insert
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)
}
