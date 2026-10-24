package com.patrickzedler.grocy.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Serializable with fixed names, because the local database stores it. */
@Serializable
data class SystemInfo(
    @SerialName("grocy_version") val grocyVersion: String,
    @SerialName("grocy_release_date") val grocyReleaseDate: String,
)
