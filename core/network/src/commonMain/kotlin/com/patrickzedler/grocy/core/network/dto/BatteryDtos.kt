package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.batteries.BatteryChargeCycle
import com.patrickzedler.grocy.core.model.batteries.BatteryDetails
import com.patrickzedler.grocy.core.model.batteries.CurrentBattery
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `/batteries`, Grocy's view `batteries_current`. */
@Serializable
internal data class CurrentBatteryDto(
    @SerialName("battery_id") val batteryId: Int,
    @SerialName("last_tracked_time") val lastTrackedTime: FlexString = null,
    @SerialName("next_estimated_charge_time") val nextChargeTime: FlexString = null,
) {
    fun toModel() = CurrentBattery(
        batteryId = batteryId,
        lastTrackedTime = lastTrackedTime?.ifBlank { null },
        // Grocy answers this date for batteries without a charge cycle interval
        nextEstimatedChargeTime = nextChargeTime?.ifBlank { null }
            ?.takeUnless { it.startsWith(NO_SCHEDULE_YEAR) },
    )
}

private const val NO_SCHEDULE_YEAR = "2999-"

/** A row of `battery_charge_cycles`. */
@Serializable
internal data class BatteryChargeCycleDto(
    val id: Int,
    @SerialName("battery_id") val batteryId: Int,
    @SerialName("tracked_time") val trackedTime: FlexString = null,
    val undone: FlexBoolean = null,
    @SerialName("undone_timestamp") val undoneTimestamp: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = BatteryChargeCycle(
        id = id,
        batteryId = batteryId,
        trackedTime = trackedTime.orEmpty(),
        undone = undone ?: false,
        undoneTimestamp = undoneTimestamp?.ifBlank { null },
        userfields = userfields.orEmpty(),
    )
}

/** `/batteries/{id}`, what Grocy's "Battery overview" card shows. */
@Serializable
internal data class BatteryDetailsDto(
    @SerialName("last_charged") val lastCharged: FlexString = null,
    @SerialName("charge_cycles_count") val chargeCyclesCount: FlexInt = null,
) {
    fun toModel(batteryId: Int) = BatteryDetails(
        batteryId = batteryId,
        lastCharged = lastCharged?.ifBlank { null },
        chargeCyclesCount = chargeCyclesCount ?: 0,
    )
}
