package com.patrickzedler.grocy.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A row of Grocy's chores overview as the server computed it. */
@Entity(tableName = "chores_current")
data class CurrentChoreEntity(
    @PrimaryKey @ColumnInfo(name = "chore_id") val choreId: Int,
    val name: String,
    @ColumnInfo(name = "last_tracked_time") val lastTrackedTime: String?,
    @ColumnInfo(name = "next_estimated_execution_time") val nextEstimatedExecutionTime: String?,
    @ColumnInfo(name = "track_date_only") val trackDateOnly: Boolean,
    @ColumnInfo(name = "assigned_user_id") val assignedUserId: Int?,
    @ColumnInfo(name = "is_rescheduled") val isRescheduled: Boolean,
    @ColumnInfo(name = "is_reassigned") val isReassigned: Boolean,
)

/** A row of Grocy's chores journal. */
@Entity(tableName = "chores_log")
data class ChoreExecutionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "chore_id", index = true) val choreId: Int,
    @ColumnInfo(name = "tracked_time", index = true) val trackedTime: String,
    @ColumnInfo(name = "scheduled_execution_time") val scheduledExecutionTime: String?,
    @ColumnInfo(name = "done_by_user_id") val doneByUserId: Int?,
    val skipped: Boolean,
    val undone: Boolean,
    @ColumnInfo(name = "undone_timestamp") val undoneTimestamp: String?,
    @ColumnInfo(name = "created_timestamp") val createdTimestamp: String?,
    val userfields: String,
)

/** What Grocy's "Chore overview" card shows, as last downloaded for the chore. */
@Entity(tableName = "chore_details")
data class ChoreDetailsEntity(
    @PrimaryKey @ColumnInfo(name = "chore_id") val choreId: Int,
    @ColumnInfo(name = "last_tracked") val lastTracked: String?,
    @ColumnInfo(name = "tracked_count") val trackedCount: Int,
    @ColumnInfo(name = "last_done_by_name") val lastDoneByName: String?,
    @ColumnInfo(name = "average_frequency_hours") val averageFrequencyHours: Double?,
)
