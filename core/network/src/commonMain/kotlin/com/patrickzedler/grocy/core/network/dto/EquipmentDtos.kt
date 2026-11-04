package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.equipment.Equipment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class EquipmentDto(
    val id: Int,
    val name: String,
    val description: FlexString = null,
    @SerialName("instruction_manual_file_name") val instructionManualFileName: FlexString = null,
    val userfields: Map<String, FlexString>? = null,
) {
    fun toModel() = Equipment(
        id = id,
        name = name,
        description = description?.ifBlank { null },
        instructionManualFileName = instructionManualFileName?.ifBlank { null },
        userfields = userfields.orEmpty(),
    )
}

internal fun Equipment.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description.orEmpty())
    // Grocy's form sends null once the manual is deleted
    put(
        "instruction_manual_file_name",
        instructionManualFileName?.let(::JsonPrimitive) ?: JsonNull,
    )
}
