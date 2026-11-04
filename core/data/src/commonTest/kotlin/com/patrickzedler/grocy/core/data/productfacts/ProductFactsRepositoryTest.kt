package com.patrickzedler.grocy.core.data.productfacts

import com.patrickzedler.grocy.core.model.ProductFactsSource
import com.patrickzedler.grocy.core.network.ProductFactsApi
import com.patrickzedler.grocy.core.network.createGrocyHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ProductFactsRepositoryTest {

    private val asked = mutableListOf<String>()
    private val answers = mutableMapOf<String, HttpStatusCode>()

    private fun repository(vararg sources: ProductFactsSource): ProductFactsRepository {
        val api = ProductFactsApi(
            httpClient = createGrocyHttpClient(
                MockEngine { request ->
                    asked += request.url.host
                    val status = answers[request.url.host] ?: HttpStatusCode.NotFound
                    respond(
                        """{"product":{"product_name":"${request.url.host}"}}""",
                        status,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            ),
            userAgent = "test",
        )
        return ProductFactsRepository(api) { sources.toSet() }
    }

    @Test
    fun withNoDatabaseEnabledNothingLeavesTheDevice() = runTest {
        val repository = repository()

        assertEquals(ProductFactsResult.NotFound, repository.lookUp("3017620422003", "en"))
        assertFalse(repository.isEnabled)
        assertTrue(asked.isEmpty())
    }

    @Test
    fun onlyEnabledDatabasesAreAskedUntilOneKnowsTheBarcode() = runTest {
        answers["world.openbeautyfacts.org"] = HttpStatusCode.OK

        val result = repository(
            ProductFactsSource.FOOD,
            ProductFactsSource.BEAUTY,
            ProductFactsSource.PRODUCTS,
        ).lookUp("3017620422003", "en")

        assertEquals(
            ProductFactsSource.BEAUTY,
            assertIs<ProductFactsResult.Found>(result).facts.source,
        )
        assertEquals(listOf("world.openfoodfacts.org", "world.openbeautyfacts.org"), asked)
    }

    @Test
    fun otherCodesNeverLeaveTheDevice() = runTest {
        val repository = repository(ProductFactsSource.FOOD)

        assertEquals(ProductFactsResult.NotFound, repository.lookUp("grcy:p:12", "en"))
        assertEquals(ProductFactsResult.NotFound, repository.lookUp("my-secret-code", "en"))
        assertEquals(ProductFactsResult.NotFound, repository.lookUp("123", "en"))
        assertTrue(asked.isEmpty())
    }

    @Test
    fun aFailureIsOnlyReportedWhenNoDatabaseKnowsTheBarcode() = runTest {
        answers["world.openfoodfacts.org"] = HttpStatusCode.ServiceUnavailable
        val both = repository(ProductFactsSource.FOOD, ProductFactsSource.BEAUTY)

        assertEquals(ProductFactsResult.Failed, both.lookUp("3017620422003", "en"))

        answers["world.openbeautyfacts.org"] = HttpStatusCode.OK
        assertIs<ProductFactsResult.Found>(both.lookUp("3017620422003", "en"))
    }

    @Test
    fun retailBarcodesCanBeLookedUp() {
        assertTrue(isLookupBarcode("12345678"))
        assertTrue(isLookupBarcode("3017620422003"))
        assertTrue(isLookupBarcode("12345678901234"))
        assertFalse(isLookupBarcode("1234567"))
        assertFalse(isLookupBarcode("123456789012345"))
        assertFalse(isLookupBarcode("3017a20422003"))
    }
}
