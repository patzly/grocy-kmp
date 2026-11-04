package com.patrickzedler.grocy.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A row of Grocy's batteries overview as the server computed it. */
@Entity(tableName = "batteries_current")
data class CurrentBatteryEntity(
    @PrimaryKey @ColumnInfo(name = "battery_id") val batteryId: Int,
    @ColumnInfo(name = "last_tracked_time") val lastTrackedTime: String?,
    @ColumnInfo(name = "next_estimated_charge_time") val nextEstimatedChargeTime: String?,
)

/** A row of Grocy's batteries journal. */
@Entity(tableName = "battery_charge_cycles")
data class BatteryChargeCycleEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "battery_id", index = true) val batteryId: Int,
    @ColumnInfo(name = "tracked_time", index = true) val trackedTime: String,
    val undone: Boolean,
    @ColumnInfo(name = "undone_timestamp") val undoneTimestamp: String?,
    val userfields: String,
)

/** What Grocy's "Battery overview" card shows, as last downloaded for the battery. */
@Entity(tableName = "battery_details")
data class BatteryDetailsEntity(
    @PrimaryKey @ColumnInfo(name = "battery_id") val batteryId: Int,
    @ColumnInfo(name = "last_charged") val lastCharged: String?,
    @ColumnInfo(name = "charge_cycles_count") val chargeCyclesCount: Int,
)

/** A piece of Grocy's equipment. */
@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "instruction_manual_file_name") val instructionManualFileName: String?,
    val userfields: String,
)
