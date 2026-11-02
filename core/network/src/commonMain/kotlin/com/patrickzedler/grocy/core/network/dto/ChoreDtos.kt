package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.chores.ChoreDetails
import com.patrickzedler.grocy.core.model.chores.ChoreExecution
import com.patrickzedler.grocy.core.model.chores.CurrentChore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `/chores`, Grocy's view `chores_current`. */
@Serializable
internal data class CurrentChoreDto(
    @SerialName("chore_id") val choreId: Int,
    @SerialName("chore_name") val choreName: FlexString = null,
    @SerialName("last_tracked_time") val lastTrackedTime: FlexString = null,
    @SerialName("next_estimated_execution_time") val nextExecutionTime: FlexString = null,
    @SerialName("track_date_only") val trackDateOnly: FlexBoolean = null,
    @SerialName("next_execution_assigned_to_user_id") val assignedUserId: FlexInt = null,
    @SerialName("is_rescheduled") val isRescheduled: FlexBoolean = null,
    @SerialName("is_reassigned") val isReassigned: FlexBoolean = null,
) {
    fun toModel() = CurrentChore(
        choreId = choreId,
        name = choreName.orEmpty(),
        lastTrackedTime = lastTrackedTime?.ifBlank { null },
        // Older servers answer this date for chores without a schedule
        nextEstimatedExecutionTime = nextExecutionTime?.ifBlank { null }
            ?.takeUnless { it.startsWith(NO_SCHEDULE_YEAR) },
        trackDateOnly = trackDateOnly ?: false,
        assignedUserId = assignedUserId,
        isRescheduled = isRescheduled ?: false,
        isReassigned = isReassigned ?: false,
    )
}

private const val NO_SCHEDULE_YEAR = "2999-"

/** A row of `chores_log`. */
@Serializable
internal data class ChoreLogDto(
    val id: Int,
    @SerialName("chore_id") val choreId: Int,
    @SerialName("tracked_time") val trackedTime: FlexString = null,
    @SerialName("scheduled_execution_time") val scheduledExecutionTime: FlexString = null,
    @SerialName("done_by_user_id") val doneByUserId: FlexInt = null,
    val skipped: FlexBoolean = null,
    val undone: FlexBoolean = null,
    @SerialName("undone_timestamp") val undoneTimestamp: FlexString = null,
    @SerialName("row_created_timestamp") val createdTimestamp: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = ChoreExecution(
        id = id,
        choreId = choreId,
        trackedTime = trackedTime.orEmpty(),
        scheduledExecutionTime = scheduledExecutionTime?.ifBlank { null },
        doneByUserId = doneByUserId,
        skipped = skipped ?: false,
        undone = undone ?: false,
        undoneTimestamp = undoneTimestamp?.ifBlank { null },
        createdTimestamp = createdTimestamp?.ifBlank { null },
        userfields = userfields.orEmpty(),
    )
}

/** `/chores/{id}`, what Grocy's "Chore overview" card shows. */
@Serializable
internal data class ChoreDetailsDto(
    @SerialName("last_tracked") val lastTracked: FlexString = null,
    @SerialName("tracked_count") val trackedCount: FlexInt = null,
    @SerialName("last_done_by") val lastDoneBy: UserDto? = null,
    @SerialName("average_execution_frequency_hours")
    val averageFrequencyHours: FlexDouble = null,
) {
    fun toModel(choreId: Int) = ChoreDetails(
        choreId = choreId,
        lastTracked = lastTracked?.ifBlank { null },
        trackedCount = trackedCount ?: 0,
        lastDoneByName = lastDoneBy?.displayName,
        averageFrequencyHours = averageFrequencyHours,
    )
}
