package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.tasks.Task
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A row of `objects/tasks`, with the done ones Grocy's `/tasks` leaves out. */
@Serializable
internal data class TaskDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    @SerialName("due_date") val dueDate: FlexString = null,
    val done: FlexBoolean = null,
    @SerialName("done_timestamp") val doneTimestamp: FlexString = null,
    @SerialName("category_id") val categoryId: FlexInt = null,
    @SerialName("assigned_to_user_id") val assignedToUserId: FlexInt = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Task(
        id = id,
        name = name,
        description = description?.ifBlank { null },
        dueDate = dueDate?.ifBlank { null },
        done = done ?: false,
        doneTimestamp = doneTimestamp?.ifBlank { null },
        categoryId = categoryId,
        assignedToUserId = assignedToUserId,
        userfields = userfields.orEmpty(),
    )
}

/** What Grocy's task form sends; whether it is done only changes through its own endpoints. */
internal fun Task.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    put("due_date", dueDate)
    put("category_id", categoryId)
    put("assigned_to_user_id", assignedToUserId)
}
