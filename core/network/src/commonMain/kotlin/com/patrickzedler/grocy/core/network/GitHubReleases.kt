package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.network.dto.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/** A release of the app on GitHub. */
data class GitHubRelease(
    /** e.g. "v1.2.0" or "nightly" */
    val tagName: String,
    /** The release page on github.com, where the APKs can be downloaded. */
    val pageUrl: String,
    /** Commit SHA the release was built from, for nightly builds. */
    val commit: String,
)

/** Thrown when GitHub cannot be reached or answers in an unexpected way. */
class GitHubReleasesException(cause: Throwable? = null) :
    Exception("Releases could not be loaded", cause)

/**
 * Releases of [repository] (e.g. "patzly/grocy-kmp") through GitHub's REST API, for builds that
 * do not come from a store. Sends no credentials, the API is public.
 */
class GitHubReleases(
    private val httpClient: HttpClient,
    private val repository: String,
) {

    /** The newest regular release, which excludes nightly builds; null if none exists yet. */
    suspend fun latestRelease(): GitHubRelease? = release("latest")

    /** The current nightly build, null if none exists. */
    suspend fun nightlyRelease(): GitHubRelease? = release("tags/nightly")

    private suspend fun release(path: String): GitHubRelease? {
        val dto = try {
            val response = httpClient.get("$API_URL/repos/$repository/releases/$path") {
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
            when {
                response.status == HttpStatusCode.NotFound -> return null
                !response.status.isSuccess() -> throw GitHubReleasesException()
                else -> response.body<GitHubReleaseDto>()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GitHubReleasesException) {
            throw e
        } catch (e: Throwable) {
            throw GitHubReleasesException(e)
        }
        // The page is opened in the browser, so it must stay on github.com
        if (!dto.htmlUrl.startsWith("https://github.com/$repository/")) {
            throw GitHubReleasesException()
        }
        return GitHubRelease(
            tagName = dto.tagName,
            pageUrl = dto.htmlUrl,
            commit = dto.targetCommitish,
        )
    }

    private companion object {
        const val API_URL = "https://api.github.com"
    }
}
