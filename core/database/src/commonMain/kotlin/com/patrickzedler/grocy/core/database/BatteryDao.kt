package com.patrickzedler.grocy.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Grocy's batteries overview, journal and overview cards as last downloaded, also for offline. */
@Dao
interface BatteryDao {

    @Query("SELECT * FROM batteries_current")
    fun observeCurrent(): Flow<List<CurrentBatteryEntity>>

    @Query("DELETE FROM batteries_current")
    suspend fun deleteCurrent()

    @Insert
    suspend fun insertCurrent(batteries: List<CurrentBatteryEntity>)

    /** The journal since [since] ("YYYY-MM-DD", everything for ""), newest first. */
    @Query(
        "SELECT * FROM battery_charge_cycles WHERE tracked_time >= :since " +
            "ORDER BY tracked_time DESC, id DESC",
    )
    fun observeChargeCycles(since: String): Flow<List<BatteryChargeCycleEntity>>

    @Query("DELETE FROM battery_charge_cycles WHERE tracked_time >= :since")
    suspend fun deleteChargeCycles(since: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChargeCycles(cycles: List<BatteryChargeCycleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChargeCycle(cycle: BatteryChargeCycleEntity)

    @Query("SELECT * FROM battery_details WHERE battery_id = :batteryId")
    fun observeDetails(batteryId: Int): Flow<BatteryDetailsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetails(details: BatteryDetailsEntity)
}
