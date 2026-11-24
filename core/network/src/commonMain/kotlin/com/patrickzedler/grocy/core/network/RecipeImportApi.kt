package com.patrickzedler.grocy.core.network

import com.patrickzedler.grocy.core.model.recipes.ImportedRecipe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/** Why a recipe page could not be imported. */
enum class RecipeImportFailure {
    /** No http(s) address. */
    INVALID_URL,

    /** The site could not be reached or refused the page. */
    UNREACHABLE,

    /** The page describes no recipe the way search engines read it. */
    NO_RECIPE,
}

class RecipeImportException(val failure: RecipeImportFailure, cause: Throwable? = null) :
    Exception("The recipe could not be imported: $failure", cause)

/**
 * Reads recipes from recipe sites. Sends nothing but the request for the page and its picture,
 * no credentials; [userAgent] names the app. Browsers refuse other sites' pages to web apps, so
 * only the Android app imports.
 */
class RecipeImportApi(
    private val httpClient: HttpClient,
    private val userAgent: String,
) {

    /** The recipe of the page at [url]. */
    suspend fun recipe(url: String): ImportedRecipe {
        val address = url.trim()
        if (!isWebAddress(address)) throw RecipeImportException(RecipeImportFailure.INVALID_URL)
        val (html, finalUrl) = try {
            val response = httpClient.get(address) {
                header(HttpHeaders.UserAgent, userAgent)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
            }
            if (!response.status.isSuccess() || response.isTooLarge(MAX_PAGE_BYTES)) {
                throw RecipeImportException(RecipeImportFailure.UNREACHABLE)
            }
            response.bodyAsText() to response.request.url.toString()
        } catch (e: CancellationException) {
            throw e
        } catch (e: RecipeImportException) {
            throw e
        } catch (e: Throwable) {
            throw RecipeImportException(RecipeImportFailure.UNREACHABLE, e)
        }
        return parseRecipePage(html, finalUrl)
            ?: throw RecipeImportException(RecipeImportFailure.NO_RECIPE)
    }

    /** The picture at [url] if it is one and not too large, otherwise null. */
    suspend fun picture(url: String): ByteArray? {
        if (!isWebAddress(url)) return null
        return try {
            val response = httpClient.get(url) { header(HttpHeaders.UserAgent, userAgent) }
            val isImage = response.contentType()?.match(ContentType.Image.Any) == true
            val tooLarge = response.isTooLarge(MAX_PICTURE_BYTES)
            if (!response.status.isSuccess() || !isImage || tooLarge) {
                null
            } else {
                response.bodyAsBytes().takeIf { it.size <= MAX_PICTURE_BYTES }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    private fun HttpResponse.isTooLarge(limit: Long) = (contentLength() ?: 0) > limit

    private companion object {
        // Recipe pages are a few hundred kilobytes, pictures a few hundred more
        const val MAX_PAGE_BYTES = 10L * 1024 * 1024
        const val MAX_PICTURE_BYTES = 15L * 1024 * 1024
    }
}

/** Whether [url] is an absolute http or https address the app may request. */
fun isWebAddress(url: String): Boolean {
    val trimmed = url.trim()
    val scheme = trimmed.substringBefore("://", "").lowercase()
    return (scheme == "https" || scheme == "http") &&
        trimmed.substringAfter("://").substringBefore('/').isNotBlank() &&
        trimmed.none { it.isWhitespace() }
}
