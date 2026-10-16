package com.patrickzedler.grocy.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class GitHubReleasesTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun releases(
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = GitHubReleases(
        httpClient = createGrocyHttpClient(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        ),
        repository = REPOSITORY,
    )

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun latestReleaseIsReadFromTheRestApi() = runTest {
        val release = releases { json(RELEASE_JSON) }.latestRelease()

        assertEquals(
            GitHubRelease(
                tagName = "v1.2.0",
                pageUrl = "https://github.com/$REPOSITORY/releases/tag/v1.2.0",
                commit = "main",
            ),
            release,
        )
        assertEquals(
            "https://api.github.com/repos/$REPOSITORY/releases/latest",
            requests.single().url.toString(),
        )
    }

    @Test
    fun nightlyReleaseUsesItsTag() = runTest {
        releases { json(RELEASE_JSON) }.nightlyRelease()

        assertEquals("/repos/$REPOSITORY/releases/tags/nightly", requests.single().url.encodedPath)
    }

    @Test
    fun requestsSendNoCredentials() = runTest {
        releases { json(RELEASE_JSON) }.latestRelease()

        val headers = requests.single().headers
        assertNull(headers[HttpHeaders.Authorization])
        assertNull(headers["GROCY-API-KEY"])
    }

    @Test
    fun missingReleaseIsNull() = runTest {
        assertNull(releases { json("{}", HttpStatusCode.NotFound) }.latestRelease())
    }

    @Test
    fun rateLimitFails() = runTest {
        assertFailsWith<GitHubReleasesException> {
            releases { json("{}", HttpStatusCode.Forbidden) }.latestRelease()
        }
    }

    @Test
    fun networkFailureFails() = runTest {
        assertFailsWith<GitHubReleasesException> {
            releases { throw IOException("offline") }.latestRelease()
        }
    }

    @Test
    fun unexpectedAnswerFails() = runTest {
        assertFailsWith<GitHubReleasesException> {
            releases { json("""{"message":"Moved"}""") }.latestRelease()
        }
    }

    @Test
    fun pageOutsideTheRepositoryIsRejected() = runTest {
        val foreignPage = RELEASE_JSON.replace(
            "https://github.com/$REPOSITORY/",
            "https://evil.example/",
        )

        assertFailsWith<GitHubReleasesException> {
            releases { json(foreignPage) }.latestRelease()
        }
    }

    private companion object {
        const val REPOSITORY = "patzly/grocy-kmp"

        // Shape of GitHub's "Get the latest release" answer, shortened
        const val RELEASE_JSON = """{"tag_name":"v1.2.0","name":"1.2.0","prerelease":false,""" +
            """"target_commitish":"main",""" +
            """"html_url":"https://github.com/$REPOSITORY/releases/tag/v1.2.0","assets":[]}"""
    }
}
