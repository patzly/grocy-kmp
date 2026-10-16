package com.patrickzedler.grocy.core.data.update

import com.patrickzedler.grocy.core.model.UpdateCheck
import com.patrickzedler.grocy.core.network.GitHubReleases
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class GitHubAppUpdaterTest {

    private val requestedPaths = mutableListOf<String>()
    private val openedUrls = mutableListOf<String>()
    private var offline = false

    private fun updater(
        versionName: String,
        latestTag: String = "v1.2.0",
        nightlyCommit: String = "abc1234def",
    ): GitHubAppUpdater {
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            if (offline) throw IOException("offline")
            val nightly = request.url.encodedPath.endsWith("/tags/nightly")
            val tag = if (nightly) "nightly" else latestTag
            val commit = if (nightly) nightlyCommit else "main"
            respond(
                content = """{"tag_name":"$tag","target_commitish":"$commit",""" +
                    """"html_url":"https://github.com/$REPOSITORY/releases/tag/$tag"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return GitHubAppUpdater(
            releases = GitHubReleases(createGrocyHttpClient(engine), REPOSITORY),
            versionName = versionName,
            openUrl = { openedUrls += it },
        )
    }

    @Test
    fun newerReleaseIsAvailable() = runTest {
        assertEquals(UpdateCheck.Available("1.2.0"), updater("1.1.0").check())
    }

    @Test
    fun currentReleaseIsUpToDate() = runTest {
        assertEquals(UpdateCheck.UpToDate, updater("1.2.0").check())
    }

    @Test
    fun nightlyBuildComparesWithTheNightlyRelease() = runTest {
        val updater = updater("1.0.0-nightly+0000000")

        assertEquals(UpdateCheck.Available(versionName = null), updater.check())
        assertTrue(requestedPaths.single().endsWith("/releases/tags/nightly"))
    }

    @Test
    fun nightlyOfTheSameCommitIsUpToDate() = runTest {
        assertEquals(UpdateCheck.UpToDate, updater("1.0.0-nightly+abc1234").check())
    }

    @Test
    fun localNightlyWithoutCommitGetsThePublishedOne() = runTest {
        assertEquals(UpdateCheck.Available(versionName = null), updater("1.0.0-nightly").check())
    }

    @Test
    fun offlineCheckFails() = runTest {
        offline = true

        assertEquals(UpdateCheck.Failed, updater("1.0.0").check())
    }

    @Test
    fun installOpensTheReleasePage() = runTest {
        val updater = updater("1.1.0")
        updater.check()

        updater.install()

        assertEquals(listOf("https://github.com/$REPOSITORY/releases/tag/v1.2.0"), openedUrls)
    }

    @Test
    fun installWithoutUpdateOpensNothing() = runTest {
        val updater = updater("1.2.0")
        updater.check()

        updater.install()

        assertEquals(emptyList(), openedUrls)
    }

    private companion object {
        const val REPOSITORY = "patzly/grocy-kmp"
    }
}
