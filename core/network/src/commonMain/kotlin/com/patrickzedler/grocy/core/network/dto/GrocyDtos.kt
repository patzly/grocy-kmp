package com.patrickzedler.grocy.core.network.dto

import com.patrickzedler.grocy.core.model.SystemInfo
import com.patrickzedler.grocy.core.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SystemInfoDto(
    @SerialName("grocy_version") val grocyVersion: GrocyVersionDto,
) {
    fun toModel() = SystemInfo(
        grocyVersion = grocyVersion.version,
        grocyReleaseDate = grocyVersion.releaseDate,
    )
}

@Serializable
internal data class GrocyVersionDto(
    @SerialName("Version") val version: String,
    @SerialName("ReleaseDate") val releaseDate: String,
)

@Serializable
internal data class UserDto(
    val id: Int,
    val username: String,
    @SerialName("display_name") val displayName: String,
) {
    fun toModel() = User(
        id = id,
        username = username,
        displayName = displayName,
    )
}
