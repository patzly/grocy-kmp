package com.patrickzedler.grocy.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A task of Grocy, also the done ones. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "due_date") val dueDate: String?,
    val done: Boolean,
    @ColumnInfo(name = "done_timestamp") val doneTimestamp: String?,
    @ColumnInfo(name = "category_id") val categoryId: Int?,
    @ColumnInfo(name = "assigned_to_user_id") val assignedToUserId: Int?,
    val userfields: String,
)
