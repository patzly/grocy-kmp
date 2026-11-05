package com.patrickzedler.grocy.core.network.dto

import kotlinx.serialization.Serializable

/** Answer of `/calendar/ical/sharing-link`. */
@Serializable
internal data class CalendarSharingLinkDto(val url: String)
