package com.patrickzedler.grocy.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RecipeImportApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(
        status: HttpStatusCode = HttpStatusCode.OK,
        contentType: ContentType = ContentType.Text.Html,
        body: ByteArray = PAGE.encodeToByteArray(),
    ) = RecipeImportApi(
        httpClient = createGrocyHttpClient(
            MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, contentType.toString()))
            },
        ),
        userAgent = "GrocyKMP/test",
    )

    @Test
    fun readsTheRecipeOfThePageAndNamesTheApp() = runTest {
        val recipe = api().recipe("https://www.chefkoch.de/rezepte/1/suppe.html")

        assertEquals("Suppe", recipe.name)
        assertEquals("https://www.chefkoch.de/rezepte/1/suppe.html", recipe.sourceUrl)
        assertEquals("GrocyKMP/test", requests.single().headers[HttpHeaders.UserAgent])
    }

    @Test
    fun aPageWithoutRecipeFailsAsSuch() = runTest {
        val error = assertFailsWith<RecipeImportException> {
            api(body = "<html></html>".encodeToByteArray()).recipe("https://example.com/news")
        }

        assertEquals(RecipeImportFailure.NO_RECIPE, error.failure)
    }

    @Test
    fun aRefusedPageIsUnreachable() = runTest {
        val error = assertFailsWith<RecipeImportException> {
            api(status = HttpStatusCode.Forbidden).recipe("https://example.com/r")
        }

        assertEquals(RecipeImportFailure.UNREACHABLE, error.failure)
    }

    @Test
    fun anAddressThatIsNoWebAddressIsNeverRequested() = runTest {
        val error = assertFailsWith<RecipeImportException> { api().recipe("file:///data/x") }

        assertEquals(RecipeImportFailure.INVALID_URL, error.failure)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun aPictureIsDownloaded() = runTest {
        val bytes = byteArrayOf(1, 2, 3)

        val picture = api(contentType = ContentType.Image.JPEG, body = bytes)
            .picture("https://img.example.com/a.jpg")

        assertContentEquals(bytes, picture)
    }

    @Test
    fun somethingElseThanAPictureIsNoPicture() = runTest {
        assertNull(api().picture("https://img.example.com/a.jpg"))
    }

    private companion object {
        const val PAGE = """<html><script type="application/ld+json">
            {"@type": "Recipe", "name": "Suppe"}</script></html>"""
    }
}
