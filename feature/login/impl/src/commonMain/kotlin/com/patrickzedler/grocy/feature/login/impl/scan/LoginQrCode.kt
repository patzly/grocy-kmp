package com.patrickzedler.grocy.feature.login.impl.scan

import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.network.ServerUrl

/** Content of a QR code scanned for login. */
internal sealed interface LoginQrCode {

    data class ApiKey(val connection: ServerConnection.ApiKey) : LoginQrCode

    /** Grocy's QR code of a calendar link, from the same page as the API key codes. */
    data object CalendarLink : LoginQrCode

    data object Invalid : LoginQrCode

    companion object {

        /**
         * Reads the code of Grocy's "Manage API keys" page: "<base URL>/api|<API key>"
         * (public/viewjs/manageapikeys.js).
         */
        fun parse(value: String): LoginQrCode {
            val text = value.trim()
            if ("/api/calendar/ical?secret=" in text) return CalendarLink

            val apiUrl = text.substringBeforeLast('|', missingDelimiterValue = "").trimEnd('/')
            val apiKey = text.substringAfterLast('|')
            // The URL always ends with /api, which keeps other codes with a "|" out
            if (!apiUrl.endsWith("/api") || apiKey.isEmpty() || apiKey.any(Char::isWhitespace)) {
                return Invalid
            }
            val serverUrl = ServerUrl.normalize(apiUrl) ?: return Invalid
            return ApiKey(ServerConnection.ApiKey(serverUrl = serverUrl, apiKey = apiKey))
        }
    }
}
