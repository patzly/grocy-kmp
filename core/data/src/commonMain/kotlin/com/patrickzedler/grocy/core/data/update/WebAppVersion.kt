package com.patrickzedler.grocy.core.data.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Build id web builds have without CI, they neither cache themselves nor look for updates. */
const val DEV_BUILD_ID = "dev"

/** The deployed web app as `precache.json` next to index.html describes it. */
@Serializable
internal data class WebAppVersion(
    /** Changes with every deployment, e.g. the commit SHA. */
    val buildId: String,
    val versionName: String,
)

private val ManifestJson = Json { ignoreUnknownKeys = true }

/** Null for anything that is not a valid manifest, e.g. an error page of the server. */
internal fun parseWebAppVersion(json: String): WebAppVersion? =
    try {
        ManifestJson.decodeFromString<WebAppVersion>(json).takeIf { it.buildId.isNotBlank() }
    } catch (_: IllegalArgumentException) {
        null
    }
