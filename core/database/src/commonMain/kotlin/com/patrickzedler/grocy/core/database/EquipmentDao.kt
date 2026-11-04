package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Grocy's equipment as last downloaded, also for offline. */
@Dao
interface EquipmentDao {

    @Query("SELECT * FROM equipment ORDER BY name COLLATE NOCASE")
    fun observeEquipment(): Flow<List<EquipmentEntity>>

    @Query("DELETE FROM equipment")
    suspend fun deleteEquipment()

    @Insert
    suspend fun insertEquipment(equipment: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipment(equipment: EquipmentEntity)

    @Query("DELETE FROM equipment WHERE id = :id")
    suspend fun deleteEquipment(id: Int)
}
