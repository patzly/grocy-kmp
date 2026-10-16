package com.patrickzedler.grocy.core.data.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebAppVersionTest {

    @Test
    fun manifestOfTheBuildIsRead() {
        // Written by apps/web at build time, the file list is for the service worker only
        val json = """{"buildId":"0b4913a5c","versionName":"1.0.0-nightly+0b4913a",""" +
            """"files":["index.html","grocy.js"]}"""

        assertEquals(
            WebAppVersion(buildId = "0b4913a5c", versionName = "1.0.0-nightly+0b4913a"),
            parseWebAppVersion(json),
        )
    }

    @Test
    fun errorPageIsNoManifest() {
        assertNull(parseWebAppVersion("<!DOCTYPE html><title>404</title>"))
    }

    @Test
    fun blankBuildIdIsNoManifest() {
        assertNull(parseWebAppVersion("""{"buildId":"","versionName":"1.0.0"}"""))
    }
}
